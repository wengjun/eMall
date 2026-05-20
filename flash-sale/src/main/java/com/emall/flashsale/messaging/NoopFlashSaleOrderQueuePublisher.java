package com.emall.flashsale.messaging;

import com.emall.flashsale.domain.FlashSaleOrderRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FlashSaleOrderQueuePublisher.class)
public class NoopFlashSaleOrderQueuePublisher implements FlashSaleOrderQueuePublisher {
    @Override
    public void publish(FlashSaleOrderRequest request) {
    }
}
