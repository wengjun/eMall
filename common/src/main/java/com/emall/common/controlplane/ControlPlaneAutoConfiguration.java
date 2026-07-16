package com.emall.common.controlplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import org.apache.kafka.clients.admin.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaAdmin;
import com.emall.common.web.OutboundHttpClientFactory;
import org.springframework.core.env.Environment;

@AutoConfiguration
@AutoConfigureAfter(KafkaAutoConfiguration.class)
@ConditionalOnProperty(name = "emall.control-plane.enabled", havingValue = "true")
@EnableConfigurationProperties(ControlPlaneProperties.class)
@Import(ControlPlaneAutoConfiguration.ControlPlaneMapperScanConfiguration.class)
public class ControlPlaneAutoConfiguration {
    @Bean
    @ConditionalOnBean(ControlPlaneOperationMapper.class)
    @ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
    @ConditionalOnMissingBean
    ControlPlaneOperationStore mybatisPlusControlPlaneOperationStore(ControlPlaneOperationMapper mapper,
            ObjectMapper objectMapper) {
        return new MybatisPlusControlPlaneOperationStore(mapper, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
    @ConditionalOnMissingBean(ControlPlaneOperationStore.class)
    ControlPlaneOperationStore inMemoryControlPlaneOperationStore() {
        return new InMemoryControlPlaneOperationStore();
    }

    @Bean
    @ConditionalOnMissingBean(name = "controlPlaneClock")
    Clock controlPlaneClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(ControlPlaneClient.class)
    ControlPlaneCommandService controlPlaneCommandService(ControlPlaneOperationStore store,
            ControlPlaneProperties properties, ObjectMapper objectMapper, Clock controlPlaneClock) {
        return new ControlPlaneCommandService(store, properties, objectMapper, controlPlaneClock);
    }

    @Bean
    @ConditionalOnMissingBean
    ControlPlaneReconciler controlPlaneReconciler(ControlPlaneOperationStore store, List<ControlPlaneAdapter> adapters,
            ControlPlaneProperties properties, Clock controlPlaneClock) {
        return new ControlPlaneReconciler(store, adapters, properties, controlPlaneClock);
    }

    @Bean
    @ConditionalOnProperty(name = "emall.control-plane.nacos.enabled", havingValue = "true")
    NacosConfigControlPlaneAdapter nacosConfigControlPlaneAdapter(OutboundHttpClientFactory clientFactory,
            ControlPlaneProperties properties, ObjectMapper objectMapper) {
        return new NacosConfigControlPlaneAdapter(clientFactory, properties.getNacos(), objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "emall.control-plane.kubernetes.enabled", havingValue = "true")
    KubernetesResourceControlPlaneAdapter kubernetesResourceControlPlaneAdapter(OutboundHttpClientFactory clientFactory,
            ControlPlaneProperties properties, ObjectMapper objectMapper) {
        return new KubernetesResourceControlPlaneAdapter(clientFactory, properties.getKubernetes(), objectMapper);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnBean(KafkaAdmin.class)
    @ConditionalOnProperty(name = "emall.control-plane.kafka.enabled", havingValue = "true")
    Admin controlPlaneKafkaAdmin(KafkaAdmin kafkaAdmin) {
        return Admin.create(kafkaAdmin.getConfigurationProperties());
    }

    @Bean
    @ConditionalOnBean(name = "controlPlaneKafkaAdmin")
    @ConditionalOnProperty(name = "emall.control-plane.kafka.enabled", havingValue = "true")
    KafkaOffsetsControlPlaneAdapter kafkaOffsetsControlPlaneAdapter(@Qualifier("controlPlaneKafkaAdmin") Admin admin,
            ControlPlaneProperties properties) {
        return new KafkaOffsetsControlPlaneAdapter(admin, properties.getKafka().getTimeout());
    }

    @Bean
    @ConditionalOnProperty(name = "emall.control-plane.infrastructure.enabled", havingValue = "true")
    InfrastructureApiControlPlaneAdapter infrastructureApiControlPlaneAdapter(OutboundHttpClientFactory clientFactory,
            ControlPlaneProperties properties) {
        return new InfrastructureApiControlPlaneAdapter(clientFactory, properties.getInfrastructure());
    }

    @Bean
    @ConditionalOnMissingBean
    ControlPlaneRuntimeGuard controlPlaneRuntimeGuard(Environment environment, ControlPlaneProperties properties,
            ControlPlaneOperationStore store, List<ControlPlaneAdapter> adapters) {
        return new ControlPlaneRuntimeGuard(environment, properties, store, adapters);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(SqlSessionFactory.class)
    @MapperScan(basePackages = "com.emall.common.controlplane", annotationClass = Mapper.class)
    static class ControlPlaneMapperScanConfiguration {
    }
}
