package com.emall.flashsale.messaging;

import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaFlashSaleOrderQueuePublisher implements FlashSaleOrderQueuePublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaFlashSaleOrderQueuePublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
            @Value("${emall.flash-sale.order-topic:emall.flash-sale.orders}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publish(FlashSaleOrderRequest request) {
        try {
            kafkaTemplate.send(topic, Long.toString(request.campaignId()), objectMapper.writeValueAsString(request))
                    .get(2, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize flash sale order request", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while publishing flash sale order request", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new IllegalStateException("failed to publish flash sale order request", ex);
        }
    }
}
