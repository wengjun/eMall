package com.emall.cart.repository;

import com.emall.cart.domain.CartItem;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryCartRepository implements CartRepository {
    private final ConcurrentMap<String, CartItem> items = new ConcurrentHashMap<>();

    @Override
    public CartItem save(CartItem item) {
        items.put(key(item.userId(), item.skuId()), item);
        return item;
    }

    @Override
    public Optional<CartItem> addQuantity(CartItem item, int maxQuantity) {
        AtomicBox result = new AtomicBox();
        items.compute(key(item.userId(), item.skuId()), (key, existing) -> {
            CartItem next = existing == null ? item : existing.add(item.quantity());
            if (next.quantity() > maxQuantity) {
                result.item = null;
                return existing;
            }
            result.item = next;
            return next;
        });
        return Optional.ofNullable(result.item);
    }

    @Override
    public Optional<CartItem> find(long userId, long skuId) {
        return Optional.ofNullable(items.get(key(userId, skuId)));
    }

    @Override
    public List<CartItem> findByUserId(long userId) {
        return items.values().stream().filter(item -> item.userId() == userId)
                .sorted(Comparator.comparing(CartItem::updatedAt).reversed()).toList();
    }

    @Override
    public void delete(long userId, long skuId) {
        items.remove(key(userId, skuId));
    }

    @Override
    public void clearSelected(long userId) {
        items.values().stream().filter(item -> item.userId() == userId && item.selected())
                .forEach(item -> items.remove(key(item.userId(), item.skuId())));
    }

    private String key(long userId, long skuId) {
        return userId + ":" + skuId;
    }

    private static final class AtomicBox {
        private CartItem item;
    }
}
