package com.emall.cart.domain;

import java.time.Instant;

public record CartItem(long userId, long skuId, int quantity, boolean selected, Instant updatedAt) {
    public CartItem add(int delta) {
        return new CartItem(userId, skuId, quantity + delta, selected, Instant.now());
    }

    public CartItem changeQuantity(int newQuantity) {
        return new CartItem(userId, skuId, newQuantity, selected, Instant.now());
    }

    public CartItem select(boolean newSelected) {
        return new CartItem(userId, skuId, quantity, newSelected, Instant.now());
    }
}
