package com.emall.cart.repository;

import com.emall.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;

public interface CartRepository {
    CartItem save(CartItem item);

    Optional<CartItem> find(long userId, long skuId);

    List<CartItem> findByUserId(long userId);

    void delete(long userId, long skuId);

    void clearSelected(long userId);
}
