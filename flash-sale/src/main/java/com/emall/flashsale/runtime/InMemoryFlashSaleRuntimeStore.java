package com.emall.flashsale.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "emall.flash-sale.runtime", havingValue = "memory")
public class InMemoryFlashSaleRuntimeStore implements FlashSaleRuntimeStore {
    private final Map<Long, Integer> availableStock = new HashMap<>();
    private final Map<String, Integer> userReserved = new HashMap<>();
    private final Map<Long, Integer> queuedStock = new HashMap<>();
    private final Set<String> usedTokens = new HashSet<>();

    @Override
    public synchronized void preloadStock(long campaignId, int totalStock) {
        availableStock.put(campaignId, totalStock);
        queuedStock.put(campaignId, 0);
    }

    @Override
    public synchronized boolean reserveTokenStock(long campaignId, long userId, int quantity, int perUserLimit,
            long ttlSeconds) {
        int available = availableStock.getOrDefault(campaignId, 0);
        String userKey = campaignId + ":" + userId;
        int reserved = userReserved.getOrDefault(userKey, 0);
        if (available < quantity || reserved + quantity > perUserLimit) {
            return false;
        }
        availableStock.put(campaignId, available - quantity);
        userReserved.put(userKey, reserved + quantity);
        return true;
    }

    @Override
    public synchronized void releaseTokenStock(long campaignId, long userId, int quantity) {
        availableStock.compute(campaignId, (key, existing) -> existing == null ? quantity : existing + quantity);
        String userKey = campaignId + ":" + userId;
        int reserved = Math.max(0, userReserved.getOrDefault(userKey, 0) - quantity);
        if (reserved == 0) {
            userReserved.remove(userKey);
            return;
        }
        userReserved.put(userKey, reserved);
    }

    @Override
    public synchronized boolean enqueueToken(long campaignId, String token, int quantity, int queueCapacity,
            long ttlSeconds) {
        int queued = queuedStock.getOrDefault(campaignId, 0);
        if (queued >= queueCapacity || !usedTokens.add(token)) {
            return false;
        }
        queuedStock.put(campaignId, queued + quantity);
        return true;
    }

    @Override
    public synchronized void releaseQueuedToken(long campaignId, String token, int quantity) {
        usedTokens.remove(token);
        int queued = Math.max(0, queuedStock.getOrDefault(campaignId, 0) - quantity);
        queuedStock.put(campaignId, queued);
    }
}
