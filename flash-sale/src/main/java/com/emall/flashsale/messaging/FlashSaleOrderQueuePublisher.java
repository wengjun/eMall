package com.emall.flashsale.messaging;

import com.emall.flashsale.domain.FlashSaleOrderRequest;

public interface FlashSaleOrderQueuePublisher {
    void publish(FlashSaleOrderRequest request);
}
