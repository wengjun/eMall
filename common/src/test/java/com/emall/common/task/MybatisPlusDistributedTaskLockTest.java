package com.emall.common.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.dao.DuplicateKeyException;

class MybatisPlusDistributedTaskLockTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final ScheduledTaskLockMapper taskLockMapper = mock(ScheduledTaskLockMapper.class);
    private final MybatisPlusDistributedTaskLock taskLock =
            new MybatisPlusDistributedTaskLock(taskLockMapper, FIXED_CLOCK, "node-a");

    @Test
    void shouldInsertLockRecordWhenLockDoesNotExist() {
        when(taskLockMapper.insert(any(ScheduledTaskLockRecord.class))).thenReturn(1);

        boolean acquired = taskLock.tryLock("order.outbox.publish", Duration.ofSeconds(30));

        ArgumentCaptor<ScheduledTaskLockRecord> recordCaptor = ArgumentCaptor.forClass(ScheduledTaskLockRecord.class);
        verify(taskLockMapper).insert(recordCaptor.capture());
        ScheduledTaskLockRecord record = recordCaptor.getValue();
        assertThat(acquired).isTrue();
        assertThat(record.getLockName()).isEqualTo("order.outbox.publish");
        assertThat(record.getOwnerId()).isEqualTo("node-a");
        assertThat(record.getLockedUntil()).isEqualTo("2026-01-01T00:00:30");
        assertThat(record.getUpdatedAt()).isEqualTo("2026-01-01T00:00");
    }

    @Test
    void shouldTakeOverExpiredLockWhenDuplicateKeyExists() {
        when(taskLockMapper.insert(any(ScheduledTaskLockRecord.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        when(taskLockMapper.update(isNull(), anyWrapper())).thenReturn(1);

        boolean acquired = taskLock.tryLock("order.outbox.publish", Duration.ofSeconds(30));

        verify(taskLockMapper).update(isNull(), anyWrapper());
        assertThat(acquired).isTrue();
    }

    @Test
    void shouldRejectLockWhenExistingLockHasNotExpired() {
        when(taskLockMapper.insert(any(ScheduledTaskLockRecord.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        when(taskLockMapper.update(isNull(), anyWrapper())).thenReturn(0);

        boolean acquired = taskLock.tryLock("order.outbox.publish", Duration.ofSeconds(30));

        assertThat(acquired).isFalse();
    }

    @Test
    void shouldReleaseOnlyCurrentOwnerLock() {
        taskLock.unlock("order.outbox.publish");

        verify(taskLockMapper).update(isNull(), anyWrapper());
    }

    @Test
    void shouldPreferMybatisPlusTaskLockWhenSqlSessionTemplateExists() {
        Configuration configuration = mock(Configuration.class);
        SqlSessionFactory sqlSessionFactory = mock(SqlSessionFactory.class);
        SqlSessionTemplate sqlSessionTemplate = mock(SqlSessionTemplate.class);
        when(sqlSessionTemplate.getSqlSessionFactory()).thenReturn(sqlSessionFactory);
        when(sqlSessionFactory.getConfiguration()).thenReturn(configuration);
        when(configuration.hasMapper(ScheduledTaskLockMapper.class)).thenReturn(false);
        when(sqlSessionTemplate.getMapper(ScheduledTaskLockMapper.class)).thenReturn(taskLockMapper);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MybatisPlusTaskLockAutoConfiguration.class, TaskLockAutoConfiguration.class))
                .withBean(SqlSessionTemplate.class, () -> sqlSessionTemplate)
                .run(context -> {
                    assertThat(context).hasSingleBean(DistributedTaskLock.class);
                    assertThat(context.getBean(DistributedTaskLock.class))
                            .isInstanceOf(MybatisPlusDistributedTaskLock.class);
                });

        verify(configuration).addMapper(ScheduledTaskLockMapper.class);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<ScheduledTaskLockRecord> anyWrapper() {
        return any(Wrapper.class);
    }
}
