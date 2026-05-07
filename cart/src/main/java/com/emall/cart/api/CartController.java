package com.emall.cart.api;

import com.emall.cart.domain.CartItem;
import com.emall.cart.domain.CartSnapshot;
import com.emall.cart.service.CartService;
import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<CartSnapshot> list(@PathVariable long userId) {
        return ApiResponse.ok(cartService.list(userId));
    }

    @PostMapping("/{userId}/items")
    public ApiResponse<CartItem> add(@PathVariable long userId, @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.ok(cartService.add(userId, request.skuId(), request.quantity()));
    }

    @PutMapping("/{userId}/items/{skuId}")
    public ApiResponse<CartItem> update(@PathVariable long userId, @PathVariable long skuId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.ok(cartService.update(userId, skuId, request.quantity(), request.selected()));
    }

    @DeleteMapping("/{userId}/items/{skuId}")
    public ApiResponse<Void> remove(@PathVariable long userId, @PathVariable long skuId) {
        cartService.remove(userId, skuId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{userId}/selected")
    public ApiResponse<Void> clearSelected(@PathVariable long userId) {
        cartService.clearSelected(userId);
        return ApiResponse.ok(null);
    }

    public record AddCartItemRequest(@Positive long skuId, @Positive @Max(999) int quantity) {
    }

    public record UpdateCartItemRequest(@Positive @Max(999) int quantity, boolean selected) {
    }
}
