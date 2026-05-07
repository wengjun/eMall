package com.emall.product.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Product(long skuId, long spuId, String title, String category, BigDecimal price, ProductStatus status,
        Instant createdAt, Instant updatedAt) {
    public Product changePrice(BigDecimal newPrice) {
        return new Product(skuId, spuId, title, category, newPrice, status, createdAt, Instant.now());
    }

    public Product changeStatus(ProductStatus newStatus) {
        return new Product(skuId, spuId, title, category, price, newStatus, createdAt, Instant.now());
    }

    public Product rename(String newTitle) {
        return new Product(skuId, spuId, newTitle, category, price, status, createdAt, Instant.now());
    }
}
