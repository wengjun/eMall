package com.emall.cart.domain;

import java.time.Instant;
import java.util.List;

public record CartSnapshot(long userId, List<CartItem> items, int selectedQuantity, Instant generatedAt) {
    public static CartSnapshot of(long userId, List<CartItem> items) {
        int selectedQuantity = items.stream().filter(CartItem::selected).mapToInt(CartItem::quantity).sum();
        return new CartSnapshot(userId, items, selectedQuantity, Instant.now());
    }
}
