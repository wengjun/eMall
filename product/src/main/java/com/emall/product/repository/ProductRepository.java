package com.emall.product.repository;

import com.emall.product.domain.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findBySkuId(long skuId);

    List<Product> search(String keyword, int limit);
}
