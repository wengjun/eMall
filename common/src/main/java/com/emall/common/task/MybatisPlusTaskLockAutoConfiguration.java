package com.emall.common.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.UUID;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = TaskLockAutoConfiguration.class)
@ConditionalOnClass({BaseMapper.class, SqlSessionTemplate.class})
public class MybatisPlusTaskLockAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnBean(SqlSessionTemplate.class)
    @ConditionalOnMissingBean(DistributedTaskLock.class)
    public DistributedTaskLock mybatisPlusDistributedTaskLock(SqlSessionTemplate sqlSessionTemplate, Clock clock) {
        registerTaskLockMapper(sqlSessionTemplate);
        return new MybatisPlusDistributedTaskLock(sqlSessionTemplate.getMapper(ScheduledTaskLockMapper.class), clock,
                ownerId());
    }

    private void registerTaskLockMapper(SqlSessionTemplate sqlSessionTemplate) {
        Configuration configuration = sqlSessionTemplate.getSqlSessionFactory().getConfiguration();
        if (!configuration.hasMapper(ScheduledTaskLockMapper.class)) {
            configuration.addMapper(ScheduledTaskLockMapper.class);
        }
    }

    private String ownerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException ex) {
            return "unknown-" + UUID.randomUUID();
        }
    }
}
