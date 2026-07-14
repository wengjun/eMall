package com.emall.order.api;

import com.emall.common.api.ApiResponse;
import com.emall.common.rpc.OrderPaymentSnapshot;
import com.emall.common.security.AuthorizationGuard;
import com.emall.common.trust.ClientTrustContext;
import com.emall.order.domain.Order;
import com.emall.order.domain.OrderClientContext;
import com.emall.order.domain.OrderClientType;
import com.emall.order.service.OrderService;
import com.emall.order.payment.OrderPaymentConfirmationService;
import com.emall.common.rpc.OrderPaymentConfirmationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final AuthorizationGuard authorizationGuard;
    private final OrderPaymentConfirmationService paymentConfirmationService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
        this.authorizationGuard = AuthorizationGuard.noop();
        this.paymentConfirmationService = null;
    }

    @Autowired
    public OrderController(OrderService orderService, AuthorizationGuard authorizationGuard,
            OrderPaymentConfirmationService paymentConfirmationService) {
        this.orderService = orderService;
        this.authorizationGuard = authorizationGuard;
        this.paymentConfirmationService = paymentConfirmationService;
    }

    public ApiResponse<Order> createOrder(CreateOrderRequest request, String deviceIdHeader, String channelHeader) {
        OrderClientContext context = OrderClientContext.of(request.clientType(),
                firstPresent(request.deviceId(), deviceIdHeader), firstPresent(request.channel(), channelHeader));
        return ApiResponse.ok(orderService.create(request.requestId(), request.userId(), request.skuId(),
                request.quantity(), context));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Order> createOrder(@Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceIdHeader,
            @RequestHeader(value = "X-Client-Channel", required = false) String channelHeader,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        authorizationGuard.requireAccount(request.userId());
        long authenticatedAccountId = authorizationGuard.accountIdOr(request.userId());
        String deviceId = firstPresent(request.deviceId(), deviceIdHeader);
        String channel = firstPresent(request.channel(), channelHeader);
        OrderClientContext context = OrderClientContext.of(request.clientType(), deviceId, channel);
        ClientTrustContext trustContext = ClientTrustContext.fromBearerHeader(authenticatedAccountId, authorization,
                deviceId, firstPresent(forwardedFor, realIp), channel);
        return ApiResponse.ok(orderService.create(request.requestId(), request.userId(), request.skuId(),
                request.quantity(), context, trustContext));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Order> getOrder(@PathVariable long orderId) {
        Order order = orderService.get(orderId);
        authorizationGuard.requireOwnerOrOperator(order.userId());
        return ApiResponse.ok(order);
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<Order> pay(@PathVariable long orderId) {
        authorizationGuard.requireServiceOrOperator();
        return ApiResponse.ok(orderService.pay(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Order> cancel(@PathVariable long orderId) {
        authorizationGuard.requireOwnerOrOperator(orderService.get(orderId).userId());
        return ApiResponse.ok(orderService.cancel(orderId));
    }

    @GetMapping("/{orderId}/payment-snapshot")
    public ApiResponse<OrderPaymentSnapshot> paymentSnapshot(@PathVariable long orderId) {
        authorizationGuard.requireServiceOrOperator();
        Order order = orderService.get(orderId);
        return ApiResponse.ok(new OrderPaymentSnapshot(order.orderId(), order.userId(), order.payableAmount(),
                order.currency(), order.status().name()));
    }

    @PostMapping("/{orderId}/confirm-payment")
    public ApiResponse<Boolean> confirmPayment(@PathVariable long orderId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        authorizationGuard.requireServiceOrOperator();
        return ApiResponse.ok(paymentConfirmationService.confirm(new OrderPaymentConfirmationCommand(orderId,
                request.paymentId(), request.paidAmount(), request.currency(), request.channelTradeNo())));
    }

    public record CreateOrderRequest(@NotBlank String requestId, @Positive long userId, @Positive long skuId,
            @Positive int quantity, OrderClientType clientType, String deviceId, String channel) {
    }

    public record ConfirmPaymentRequest(@Positive long paymentId, @NotNull BigDecimal paidAmount,
            @NotBlank String currency, @NotBlank String channelTradeNo) {
    }

    private String firstPresent(String bodyValue, String headerValue) {
        return bodyValue == null || bodyValue.isBlank() ? headerValue : bodyValue;
    }
}
