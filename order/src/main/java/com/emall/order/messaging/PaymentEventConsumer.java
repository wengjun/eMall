package com.emall.order.messaging;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.MessageConsumerTemplate;
import com.emall.common.messaging.ProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.order.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventConsumer {
    private final OrderService orderService;
    private final MessageConsumerTemplate consumerTemplate;

    public PaymentEventConsumer(ObjectMapper objectMapper, OrderService orderService, BusinessMetrics businessMetrics,
            ProcessedMessageRepository processedMessageRepository,
            @Value("${emall.events.payment-consumer-max-attempts:4}") int maxAttempts) {
        this.orderService = orderService;
        this.consumerTemplate = new MessageConsumerTemplate(objectMapper, processedMessageRepository, businessMetrics,
                maxAttempts, "order-payment-consumer");
    }

    @Transactional
    @KafkaListener(topics = "${emall.events.payment-topic:emall.payment.events}",
            groupId = "${spring.kafka.consumer.group-id:order}")
    public void onPaymentEvent(String message) throws JsonProcessingException {
        consumerTemplate.consume(message, EventTypes.PAYMENT_SUCCEEDED, this::markOrderPaid);
    }

    private void markOrderPaid(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        orderService.pay(longValue(payload.get("orderId")));
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
