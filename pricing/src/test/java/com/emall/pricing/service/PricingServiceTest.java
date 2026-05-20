package com.emall.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.cache.ExpiringMapCacheStore;
import com.emall.common.cache.TwoLevelCache;
import com.emall.common.exception.BusinessException;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.pricing.domain.PriceBook;
import com.emall.pricing.domain.PriceQuote;
import com.emall.pricing.repository.InMemoryPriceRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService(new InMemoryPriceRepository());

    @Test
    void shouldCreatePriceAndQuoteSubtotal() {
        pricingService.upsert(30001L, new BigDecimal("3999.00"), new BigDecimal("3799.00"), "CNY", true);

        PriceQuote quote = pricingService.quote(30001L, 2);

        assertThat(quote.unitPrice()).isEqualByComparingTo("3799.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("7598.00");
        assertThat(quote.priceVersion()).isEqualTo(1L);
    }

    @Test
    void shouldRejectSalePriceGreaterThanListPrice() {
        assertThatThrownBy(
                () -> pricingService.upsert(30001L, new BigDecimal("99.00"), new BigDecimal("100.00"), "CNY", true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("sale price cannot exceed list price");
    }

    @Test
    void shouldReadPriceBookFromTwoLevelCacheAfterFirstLoad() {
        CountingPriceRepository repository = new CountingPriceRepository();
        TwoLevelCache<Long, PriceBookCacheEntry> cache = new TwoLevelCache<>(
                new ExpiringMapCacheStore<>(Duration.ofMinutes(1)), new ExpiringMapCacheStore<>(Duration.ofMinutes(5)));
        PricingService cachedService =
                new PricingService(repository, ShardRoutingOperations.noop(), OwnershipGuard.noop(), provider(cache));

        PriceBook first = cachedService.get(10001L);
        PriceBook second = cachedService.get(10001L);

        assertThat(first.skuId()).isEqualTo(10001L);
        assertThat(second.skuId()).isEqualTo(10001L);
        assertThat(repository.findCalls).isEqualTo(1);
        assertThat(cache.getIfPresent(10001L)).get().extracting(PriceBookCacheEntry::present).isEqualTo(true);
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }
        };
    }

    private static final class CountingPriceRepository extends InMemoryPriceRepository {
        private int findCalls;

        @Override
        public Optional<PriceBook> findBySkuId(long skuId) {
            findCalls++;
            return super.findBySkuId(skuId);
        }
    }
}
