package com.emall.pricing.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.cache.CacheTtlPolicy;
import com.emall.common.cache.TwoLevelCache;
import com.emall.common.exception.BusinessException;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.pricing.domain.PriceBook;
import com.emall.pricing.domain.PriceQuote;
import com.emall.pricing.repository.PriceRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {
    private final PriceRepository priceRepository;
    private final ShardRoutingOperations shardRoutingOperations;
    private final OwnershipGuard ownershipGuard;
    private final TwoLevelCache<Long, PriceBookCacheEntry> priceBookCache;
    private final CacheTtlPolicy priceTtlPolicy = new CacheTtlPolicy(Duration.ofMinutes(5), 0.1);
    private final CacheTtlPolicy nullTtlPolicy = new CacheTtlPolicy(Duration.ofSeconds(15), 0.2);

    public PricingService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
        this.shardRoutingOperations = ShardRoutingOperations.noop();
        this.ownershipGuard = OwnershipGuard.noop();
        this.priceBookCache = null;
    }

    @Autowired
    public PricingService(PriceRepository priceRepository, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard, ObjectProvider<TwoLevelCache<Long, PriceBookCacheEntry>> cacheProvider) {
        this.priceRepository = priceRepository;
        this.shardRoutingOperations = shardRoutingOperations;
        this.ownershipGuard = ownershipGuard;
        this.priceBookCache = cacheProvider.getIfAvailable();
    }

    @Transactional
    public PriceBook upsert(long skuId, BigDecimal listPrice, BigDecimal salePrice, String currency, boolean active) {
        return shardRoutingOperations.execute("price_book", skuId, () -> {
            ownershipGuard.checkWrite("pricing", skuId);
            validatePrice(listPrice, salePrice);
            PriceBook existing = priceRepository.findBySkuId(skuId).orElse(null);
            if (existing == null) {
                PriceBook created = new PriceBook(skuId, listPrice, salePrice, currency, 1L, active, Instant.now());
                PriceBook saved = priceRepository.save(created);
                cachePrice(saved);
                return saved;
            }
            PriceBook saved = priceRepository.save(existing.change(listPrice, salePrice, currency, active));
            cachePrice(saved);
            return saved;
        });
    }

    public PriceBook get(long skuId) {
        PriceBookCacheEntry entry = priceBookCache == null
                ? loadPriceBookCacheEntry(skuId)
                : priceBookCache.get(skuId, () -> loadPriceBookCacheEntry(skuId),
                        value -> value.present()
                                ? priceTtlPolicy.ttlForKey(skuId)
                                : nullTtlPolicy.ttlForKey("price-miss:" + skuId));
        if (!entry.present()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "price not found");
        }
        return entry.priceBook();
    }

    public PriceQuote quote(long skuId, int quantity) {
        PriceBook priceBook = get(skuId);
        if (!priceBook.active()) {
            throw new BusinessException(ErrorCode.CONFLICT, "price is inactive");
        }
        return PriceQuote.of(priceBook, quantity);
    }

    private void validatePrice(BigDecimal listPrice, BigDecimal salePrice) {
        if (listPrice.signum() <= 0 || salePrice.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "prices must be positive");
        }
        if (salePrice.compareTo(listPrice) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sale price cannot exceed list price");
        }
    }

    private PriceBookCacheEntry loadPriceBookCacheEntry(long skuId) {
        return shardRoutingOperations.execute("price_book", skuId, () -> priceRepository.findBySkuId(skuId)
                .map(PriceBookCacheEntry::hit).orElseGet(PriceBookCacheEntry::miss));
    }

    private void cachePrice(PriceBook priceBook) {
        if (priceBookCache != null) {
            priceBookCache.put(priceBook.skuId(), PriceBookCacheEntry.hit(priceBook),
                    priceTtlPolicy.ttlForKey(priceBook.skuId()));
        }
    }
}
