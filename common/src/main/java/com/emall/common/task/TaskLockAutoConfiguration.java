package com.emall.common.task;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
public class TaskLockAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean(DistributedTaskLock.class)
    public DistributedTaskLock jdbcDistributedTaskLock(JdbcTemplate jdbcTemplate, Clock clock) {
        return new JdbcDistributedTaskLock(jdbcTemplate, clock, ownerId());
    }

    @Bean
    @ConditionalOnMissingBean(DistributedTaskLock.class)
    public DistributedTaskLock inMemoryDistributedTaskLock(Clock clock) {
        return new InMemoryDistributedTaskLock(clock, ownerId());
    }

    private String ownerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException ex) {
            return "unknown-" + UUID.randomUUID();
        }
    }
}
