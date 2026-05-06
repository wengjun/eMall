package com.emall.common.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class JdbcDistributedTaskLockIT {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("emall_common_it")
            .withUsername("emall")
            .withPassword("emall");

    @Test
    void shouldCoordinateTaskExecutionAcrossOwnersThroughMysql() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
        createLockTable(jdbcTemplate);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        JdbcDistributedTaskLock ownerA = new JdbcDistributedTaskLock(jdbcTemplate, clock, "node-a");
        JdbcDistributedTaskLock ownerB = new JdbcDistributedTaskLock(jdbcTemplate, clock, "node-b");

        boolean ownerAAcquired = ownerA.tryLock("order.outbox.publish", Duration.ofSeconds(30));
        boolean ownerBAcquiredWhileLocked = ownerB.tryLock("order.outbox.publish", Duration.ofSeconds(30));
        ownerA.unlock("order.outbox.publish");
        boolean ownerBAcquiredAfterUnlock = ownerB.tryLock("order.outbox.publish", Duration.ofSeconds(30));

        assertThat(ownerAAcquired).isTrue();
        assertThat(ownerBAcquiredWhileLocked).isFalse();
        assertThat(ownerBAcquiredAfterUnlock).isTrue();
    }

    private DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(mysql.getJdbcUrl())
                .username(mysql.getUsername())
                .password(mysql.getPassword())
                .build();
    }

    private void createLockTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                create table if not exists scheduled_task_lock (
                    lock_name varchar(128) primary key,
                    owner_id varchar(256) not null,
                    locked_until timestamp(6) not null,
                    updated_at timestamp(6) not null,
                    index idx_scheduled_task_lock_until (locked_until)
                )
                """);
    }
}
