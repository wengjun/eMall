package com.emall.flashsale.repository;

import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleRequestStatus;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryFlashSaleRepository implements FlashSaleRepository {
    private final ConcurrentMap<Long, FlashSaleCampaign> campaigns = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, FlashSaleStock> stocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FlashSaleToken> tokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, FlashSaleOrderRequest> requests = new ConcurrentHashMap<>();

    @Override
    public FlashSaleCampaign saveCampaign(FlashSaleCampaign campaign) {
        campaigns.put(campaign.campaignId(), campaign);
        return campaign;
    }

    @Override
    public Optional<FlashSaleCampaign> findCampaign(long campaignId) {
        return Optional.ofNullable(campaigns.get(campaignId));
    }

    @Override
    public FlashSaleStock saveStock(FlashSaleStock stock) {
        stocks.put(stock.campaignId(), stock);
        return stock;
    }

    @Override
    public Optional<FlashSaleStock> findStock(long campaignId) {
        return Optional.ofNullable(stocks.get(campaignId));
    }

    @Override
    public synchronized boolean reserveTokenStock(long campaignId, int quantity) {
        FlashSaleStock stock = stocks.get(campaignId);
        if (stock == null || stock.availableStock() < quantity) {
            return false;
        }
        stocks.put(campaignId, stock.reserveForToken(quantity));
        return true;
    }

    @Override
    public synchronized void releaseTokenStock(long campaignId, int quantity) {
        FlashSaleStock stock = stocks.get(campaignId);
        if (stock == null) {
            return;
        }
        stocks.put(campaignId,
                new FlashSaleStock(stock.campaignId(), stock.skuId(), stock.totalStock(),
                        stock.availableStock() + quantity, Math.max(0, stock.tokenReservedStock() - quantity),
                        stock.queuedStock(), stock.soldStock(), stock.updatedAt()));
    }

    @Override
    public synchronized boolean moveTokenStockToQueue(long campaignId, int quantity) {
        FlashSaleStock stock = stocks.get(campaignId);
        if (stock == null || stock.tokenReservedStock() < quantity) {
            return false;
        }
        stocks.put(campaignId, stock.moveTokenToQueue(quantity));
        return true;
    }

    @Override
    public synchronized void releaseQueuedStock(long campaignId, int quantity) {
        FlashSaleStock stock = stocks.get(campaignId);
        if (stock == null) {
            return;
        }
        stocks.put(campaignId,
                new FlashSaleStock(stock.campaignId(), stock.skuId(), stock.totalStock(), stock.availableStock(),
                        stock.tokenReservedStock() + quantity, Math.max(0, stock.queuedStock() - quantity),
                        stock.soldStock(), stock.updatedAt()));
    }

    @Override
    public boolean markTokenUsed(String token) {
        AtomicFlag updated = new AtomicFlag();
        tokens.computeIfPresent(token, (key, existing) -> {
            if (existing.used()) {
                return existing;
            }
            updated.mark();
            return existing.use();
        });
        return updated.value();
    }

    @Override
    public void unmarkTokenUsed(String token) {
        tokens.computeIfPresent(token,
                (key, existing) -> existing.used()
                        ? new FlashSaleToken(existing.tokenId(), existing.campaignId(), existing.userId(),
                                existing.skuId(), existing.quantity(), existing.token(), existing.expiresAt(), false,
                                existing.createdAt(), existing.updatedAt())
                        : existing);
    }

    @Override
    public FlashSaleToken saveToken(FlashSaleToken token) {
        tokens.put(token.token(), token);
        return token;
    }

    @Override
    public Optional<FlashSaleToken> findTokenById(long tokenId) {
        return tokens.values().stream().filter(token -> token.tokenId() == tokenId).findFirst();
    }

    @Override
    public Optional<FlashSaleToken> findToken(String token) {
        return Optional.ofNullable(tokens.get(token));
    }

    @Override
    public int countTokensByUser(long campaignId, long userId) {
        return (int) tokens.values().stream()
                .filter(token -> token.campaignId() == campaignId && token.userId() == userId).count();
    }

    @Override
    public FlashSaleOrderRequest saveOrderRequest(FlashSaleOrderRequest request) {
        requests.put(request.requestId(), request);
        return request;
    }

    @Override
    public void deleteOrderRequest(long requestId) {
        requests.remove(requestId);
    }

    @Override
    public Optional<FlashSaleOrderRequest> findOrderRequestByToken(String token) {
        return requests.values().stream().filter(request -> request.token().equals(token)).findFirst();
    }

    @Override
    public Optional<FlashSaleOrderRequest> findOrderRequest(long requestId) {
        return Optional.ofNullable(requests.get(requestId));
    }

    @Override
    public List<FlashSaleOrderRequest> findQueuedRequests(long campaignId, int limit) {
        return requests.values().stream().filter(request -> request.campaignId() == campaignId)
                .filter(request -> request.status() == FlashSaleRequestStatus.QUEUED)
                .sorted(Comparator.comparing(FlashSaleOrderRequest::createdAt)
                        .thenComparingLong(FlashSaleOrderRequest::requestId))
                .limit(limit).toList();
    }

    @Override
    public int countQueuedRequests(long campaignId) {
        return (int) requests.values().stream().filter(request -> request.campaignId() == campaignId)
                .filter(request -> request.status() == FlashSaleRequestStatus.QUEUED).count();
    }

    private static final class AtomicFlag {
        private boolean value;

        void mark() {
            value = true;
        }

        boolean value() {
            return value;
        }
    }
}
