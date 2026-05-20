package com.emall.order.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.messaging.InMemoryProcessedMessageRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaymentEventConsumerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final OrderService orderService = mock(OrderService.class);
    private final PaymentEventConsumer consumer = new PaymentEventConsumer(objectMapper, orderService,
            BusinessMetrics.noop(), new InMemoryProcessedMessageRepository(), 2);

    @Test
    void shouldMarkOrderPaidOnceForDuplicatedPaymentSuccessEvent() throws Exception {
        String message = message();

        consumer.onPaymentEvent(message);
        consumer.onPaymentEvent(message);

        verify(orderService, times(1)).pay(90001L);
    }

    private String message() throws Exception {
        OutboxEvent event = OutboxEvent.create("payment-event-90001", "Payment", "80001", EventTypes.PAYMENT_SUCCEEDED,
                Map.of("orderId", 90001L));
        return objectMapper.writeValueAsString(event);
    }
}
