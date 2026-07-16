package com.emall.product.repository;

import com.emall.product.domain.Product;
import com.emall.product.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryProductRepository implements ProductRepository {
    private final ConcurrentMap<Long, Product> products = new ConcurrentHashMap<>();

    public InMemoryProductRepository() {
        Instant now = Instant.now();
        save(new Product(10001L, 90001L, "flagship phone", "digital", BigDecimal.valueOf(399900, 2),
                ProductStatus.ON_SALE, now, now));
        save(new Product(10002L, 90002L, "thin laptop", "computer", BigDecimal.valueOf(699900, 2),
                ProductStatus.ON_SALE, now, now));
    }

    @Override
    public Product save(Product product) {
        products.put(product.skuId(), product);
        return product;
    }

    @Override
    public Optional<Product> findBySkuId(long skuId) {
        return Optional.ofNullable(products.get(skuId));
    }
}
