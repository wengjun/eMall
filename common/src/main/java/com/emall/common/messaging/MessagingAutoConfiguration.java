package com.emall.common.messaging;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnClass(ProcessedMessageRecordMapper.class)
@Import(MessagingAutoConfiguration.MessagingMapperScanConfiguration.class)
public class MessagingAutoConfiguration {
    @Bean
    @ConditionalOnBean(ProcessedMessageRecordMapper.class)
    @ConditionalOnMissingBean
    ProcessedMessageRepository mybatisPlusProcessedMessageRepository(ProcessedMessageRecordMapper mapper) {
        return new MybatisPlusProcessedMessageRepositorySupport(mapper) {
        };
    }

    @Bean
    @ConditionalOnMissingBean({ProcessedMessageRepository.class, ProcessedMessageRecordMapper.class})
    ProcessedMessageRepository inMemoryProcessedMessageRepository() {
        return new InMemoryProcessedMessageRepository();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(SqlSessionFactory.class)
    @MapperScan(basePackages = "com.emall.common.messaging", annotationClass = Mapper.class)
    static class MessagingMapperScanConfiguration {
    }
}
