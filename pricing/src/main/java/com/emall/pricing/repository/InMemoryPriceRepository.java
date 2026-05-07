package com.emall.pricing.repository;

import com.emall.pricing.domain.PriceBook;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryPriceRepository implements PriceRepository {
    private final ConcurrentMap<Long, PriceBook> prices = new ConcurrentHashMap<>();

    public InMemoryPriceRepository() {
        save(new PriceBook(10001L, BigDecimal.valueOf(399900, 2), BigDecimal.valueOf(379900, 2), "USD", 1L, true,
                Instant.now()));
        save(new PriceBook(10002L, BigDecimal.valueOf(699900, 2), BigDecimal.valueOf(679900, 2), "USD", 1L, true,
                Instant.now()));
    }

    @Override
    public PriceBook save(PriceBook priceBook) {
        prices.put(priceBook.skuId(), priceBook);
        return priceBook;
    }

    @Override
    public Optional<PriceBook> findBySkuId(long skuId) {
        return Optional.ofNullable(prices.get(skuId));
    }
}
