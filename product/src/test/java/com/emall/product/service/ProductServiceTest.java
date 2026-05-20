package com.emall.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.cache.ExpiringMapCacheStore;
import com.emall.common.cache.TwoLevelCache;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import com.emall.product.domain.Product;
import com.emall.product.domain.ProductStatus;
import com.emall.product.repository.InMemoryOutboxRepository;
import com.emall.product.repository.InMemoryProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ProductServiceTest {
    private final InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
    private final ProductService productService =
            new ProductService(new InMemoryProductRepository(), outboxRepository, new SnowflakeIdGenerator(1));

    @Test
    void shouldCreatePublishAndSearchProduct() {
        Product created = productService.create(1001L, "flagship phone", "digital", new BigDecimal("3999.00"));

        Product onSale = productService.changeStatus(created.skuId(), ProductStatus.ON_SALE);
        Product repriced = productService.changePrice(created.skuId(), new BigDecimal("3799.00"));

        assertThat(onSale.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(repriced.price()).isEqualByComparingTo("3799.00");
        assertThat(productService.search("phone", 10)).extracting(Product::skuId).contains(created.skuId());
        assertThat(outboxRepository.findPublishable(Instant.now(), 10)).hasSize(3);
    }

    @Test
    void shouldRejectNonPositivePrice() {
        Product created = productService.create(1001L, "flagship phone", "digital", new BigDecimal("3999.00"));

        assertThatThrownBy(() -> productService.changePrice(created.skuId(), BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class).hasMessageContaining("price must be positive");
    }

    @Test
    void shouldReadProductFromTwoLevelCacheAfterFirstLoad() {
        CountingProductRepository repository = new CountingProductRepository();
        TwoLevelCache<Long, ProductCacheEntry> cache = new TwoLevelCache<>(
                new ExpiringMapCacheStore<>(Duration.ofMinutes(1)), new ExpiringMapCacheStore<>(Duration.ofMinutes(5)));
        ProductService cachedService = new ProductService(repository, new InMemoryOutboxRepository(),
                new SnowflakeIdGenerator(2), ShardRoutingOperations.noop(), OwnershipGuard.noop(), provider(cache));

        Product first = cachedService.get(10001L);
        Product second = cachedService.get(10001L);

        assertThat(first.skuId()).isEqualTo(10001L);
        assertThat(second.skuId()).isEqualTo(10001L);
        assertThat(repository.findCalls).isEqualTo(1);
        assertThat(cache.getIfPresent(10001L)).get().extracting(ProductCacheEntry::present).isEqualTo(true);
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

    private static final class CountingProductRepository extends InMemoryProductRepository {
        private int findCalls;

        @Override
        public Optional<Product> findBySkuId(long skuId) {
            findCalls++;
            return super.findBySkuId(skuId);
        }
    }
}
