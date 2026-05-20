package com.emall.common.idempotency;

import java.time.Clock;
import java.time.Duration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties.class)
@ConditionalOnClass(IdempotencyRecordMapper.class)
@Import(IdempotencyAutoConfiguration.IdempotencyMapperScanConfiguration.class)
public class IdempotencyAutoConfiguration {
    @Bean
    @ConditionalOnBean(IdempotencyRecordMapper.class)
    @ConditionalOnMissingBean
    IdempotencyRepository mybatisPlusIdempotencyRepository(IdempotencyRecordMapper mapper) {
        return new MybatisPlusIdempotencyRepositorySupport(mapper) {
        };
    }

    @Bean
    @ConditionalOnMissingBean({IdempotencyRepository.class, IdempotencyRecordMapper.class})
    IdempotencyRepository inMemoryIdempotencyRepository() {
        return new InMemoryIdempotencyRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    IdempotencyService idempotencyService(IdempotencyRepository repository, IdempotencyProperties properties,
            Clock clock) {
        return new IdempotencyService(repository, clock, properties.getProcessingTtl(), properties.getRecordTtl());
    }

    @Bean
    @ConditionalOnMissingBean
    Clock idempotencyClock() {
        return Clock.systemUTC();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(SqlSessionFactory.class)
    @MapperScan(basePackages = "com.emall.common.idempotency", annotationClass = Mapper.class)
    static class IdempotencyMapperScanConfiguration {
    }
}
