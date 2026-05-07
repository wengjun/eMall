package com.emall.cart.service;

import com.emall.cart.domain.CartItem;
import com.emall.cart.domain.CartSnapshot;
import com.emall.cart.repository.CartRepository;
import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private static final int MAX_ITEM_QUANTITY = 999;
    private static final int MAX_CART_LINES = 500;

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Transactional
    public synchronized CartItem add(long userId, long skuId, int quantity) {
        int lines = cartRepository.findByUserId(userId).size();
        if (lines >= MAX_CART_LINES && cartRepository.find(userId, skuId).isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "cart line limit exceeded");
        }
        CartItem item = cartRepository.find(userId, skuId).map(existing -> existing.add(quantity))
                .orElse(new CartItem(userId, skuId, quantity, true, Instant.now()));
        if (item.quantity() > MAX_ITEM_QUANTITY) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "item quantity limit exceeded");
        }
        return cartRepository.save(item);
    }

    @Transactional
    public CartItem update(long userId, long skuId, int quantity, boolean selected) {
        CartItem item = cartRepository.find(userId, skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "cart item not found"))
                .changeQuantity(quantity).select(selected);
        return cartRepository.save(item);
    }

    public CartSnapshot list(long userId) {
        return CartSnapshot.of(userId, cartRepository.findByUserId(userId));
    }

    @Transactional
    public void remove(long userId, long skuId) {
        cartRepository.delete(userId, skuId);
    }

    @Transactional
    public void clearSelected(long userId) {
        cartRepository.clearSelected(userId);
    }
}
