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
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    CommonErrorHandler databaseRetryAwareKafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".dlt", record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, Long.MAX_VALUE));
        errorHandler.addNotRetryableExceptions(DeadMessageException.class, JsonProcessingException.class);
        errorHandler.setCommitRecovered(true);
        errorHandler.setAckAfterHandle(true);
        return errorHandler;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(SqlSessionFactory.class)
    @MapperScan(basePackages = "com.emall.common.messaging", annotationClass = Mapper.class)
    static class MessagingMapperScanConfiguration {
    }
}
