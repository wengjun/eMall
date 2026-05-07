package com.emall.order.api;

import com.emall.common.api.ApiResponse;
import com.emall.order.domain.Order;
import com.emall.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse
                .ok(orderService.create(request.requestId(), request.userId(), request.skuId(), request.quantity()));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Order> getOrder(@PathVariable long orderId) {
        return ApiResponse.ok(orderService.get(orderId));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<Order> pay(@PathVariable long orderId) {
        return ApiResponse.ok(orderService.pay(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Order> cancel(@PathVariable long orderId) {
        return ApiResponse.ok(orderService.cancel(orderId));
    }

    public record CreateOrderRequest(@NotBlank String requestId, @Positive long userId, @Positive long skuId,
            @Positive int quantity) {
    }
}
