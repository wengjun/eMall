package com.emall.product.service;

import com.emall.product.domain.Product;

public record ProductCacheEntry(boolean present, Product product) {
    public static ProductCacheEntry hit(Product product) {
        return new ProductCacheEntry(true, product);
    }

    public static ProductCacheEntry miss() {
        return new ProductCacheEntry(false, null);
    }
}
