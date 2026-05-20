package com.emall.cart.service;

import com.emall.cart.domain.CartItem;
import com.emall.cart.domain.CartSnapshot;
import com.emall.cart.repository.CartRepository;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.region.OwnershipGuard;
import com.emall.common.sharding.ShardRoutingOperations;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private static final int MAX_ITEM_QUANTITY = 999;
    private static final int MAX_CART_LINES = 500;

    private final CartRepository cartRepository;
    private final ShardRoutingOperations shardRoutingOperations;
    private final OwnershipGuard ownershipGuard;

    public CartService(CartRepository cartRepository) {
        this(cartRepository, ShardRoutingOperations.noop(), OwnershipGuard.noop());
    }

    @Autowired
    public CartService(CartRepository cartRepository, ShardRoutingOperations shardRoutingOperations,
            OwnershipGuard ownershipGuard) {
        this.cartRepository = cartRepository;
        this.shardRoutingOperations = shardRoutingOperations;
        this.ownershipGuard = ownershipGuard;
    }

    @Transactional
    public CartItem add(long userId, long skuId, int quantity) {
        return shardRoutingOperations.execute("cart_item", userId, () -> {
            ownershipGuard.checkWrite("cart", userId);
            int lines = cartRepository.findByUserId(userId).size();
            if (lines >= MAX_CART_LINES && cartRepository.find(userId, skuId).isEmpty()) {
                throw new BusinessException(ErrorCode.CONFLICT, "cart line limit exceeded");
            }
            return cartRepository
                    .addQuantity(new CartItem(userId, skuId, quantity, true, Instant.now()), MAX_ITEM_QUANTITY)
                    .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "item quantity limit exceeded"));
        });
    }

    @Transactional
    public CartItem update(long userId, long skuId, int quantity, boolean selected) {
        return shardRoutingOperations.execute("cart_item", userId, () -> {
            ownershipGuard.checkWrite("cart", userId);
            CartItem item = cartRepository.find(userId, skuId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "cart item not found"))
                    .changeQuantity(quantity).select(selected);
            return cartRepository.save(item);
        });
    }

    public CartSnapshot list(long userId) {
        return shardRoutingOperations.execute("cart_item", userId,
                () -> CartSnapshot.of(userId, cartRepository.findByUserId(userId)));
    }

    @Transactional
    public void remove(long userId, long skuId) {
        shardRoutingOperations.execute("cart_item", userId, () -> {
            ownershipGuard.checkWrite("cart", userId);
            cartRepository.delete(userId, skuId);
            return null;
        });
    }

    @Transactional
    public void clearSelected(long userId) {
        shardRoutingOperations.execute("cart_item", userId, () -> {
            ownershipGuard.checkWrite("cart", userId);
            cartRepository.clearSelected(userId);
            return null;
        });
    }
}
