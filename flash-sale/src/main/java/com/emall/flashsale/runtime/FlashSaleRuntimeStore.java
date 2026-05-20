package com.emall.flashsale.runtime;

public interface FlashSaleRuntimeStore {
    void preloadStock(long campaignId, int totalStock);

    boolean reserveTokenStock(long campaignId, long userId, int quantity, int perUserLimit, long ttlSeconds);

    void releaseTokenStock(long campaignId, long userId, int quantity);

    boolean enqueueToken(long campaignId, String token, int quantity, int queueCapacity, long ttlSeconds);

    void releaseQueuedToken(long campaignId, String token, int quantity);
}
