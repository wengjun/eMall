package com.emall.order.integration;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.emall.common.rpc.CouponConfirmationCommand;
import com.emall.common.rpc.CouponReleaseCommand;
import com.emall.common.rpc.CouponReservationCommand;
import com.emall.common.rpc.CouponReservationView;
import com.emall.common.rpc.MarketingRpcService;
import com.emall.common.rpc.PromotionQuoteCommand;
import com.emall.common.rpc.PromotionQuoteView;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MarketingClient {
    private final RestClient marketingRestClient;
    private final String rpcProtocol;

    @DubboReference(check = false, retries = 0, timeout = 2000)
    private MarketingRpcService marketingRpcService;

    @Autowired
    public MarketingClient(RestClient marketingRestClient, @Value("${emall.rpc.protocol:http}") String rpcProtocol) {
        this.marketingRestClient = marketingRestClient;
        this.rpcProtocol = rpcProtocol;
    }

    public MarketingClient(RestClient marketingRestClient) {
        this(marketingRestClient, "http");
    }

    @SentinelResource(value = "order.marketing.quote", blockHandler = "blockQuote", fallback = "fallbackQuote")
    public PromotionQuote quote(long userId, BigDecimal orderAmount) {
        PromotionQuote result;
        if (dubboEnabled()) {
            result = toLocal(marketingRpcService.quote(new PromotionQuoteCommand(userId, orderAmount)));
        } else {
            PromotionQuoteResponse response = marketingRestClient.post().uri("/api/marketing/quotes")
                    .body(new PromotionQuoteRequest(userId, orderAmount)).retrieve().body(PromotionQuoteResponse.class);
            result = response == null ? null : response.data();
        }
        return result == null ? PromotionQuote.none(userId, orderAmount) : result;
    }

    public PromotionQuote fallbackQuote(long userId, BigDecimal orderAmount, Throwable error) {
        return PromotionQuote.none(userId, orderAmount);
    }

    public PromotionQuote blockQuote(long userId, BigDecimal orderAmount, BlockException error) {
        return PromotionQuote.none(userId, orderAmount);
    }

    public CouponReservation reserveCoupon(String reservationId, long userId, String couponId, BigDecimal orderAmount,
            long orderId) {
        if (couponId == null || couponId.isBlank()) {
            return CouponReservation.none(reservationId, userId, orderAmount, orderId);
        }
        try {
            CouponReservation result;
            if (dubboEnabled()) {
                result = toLocal(marketingRpcService.reserveCoupon(
                        new CouponReservationCommand(reservationId, userId, couponId, orderAmount, orderId)));
            } else {
                CouponReservationResponse response =
                        marketingRestClient.post().uri("/api/marketing/coupons/{couponId}/reservations", couponId)
                                .body(new CouponReservationRequest(reservationId, userId, orderAmount, orderId))
                                .retrieve().body(CouponReservationResponse.class);
                result = response == null ? null : response.data();
            }
            return result == null
                    ? CouponReservation.unavailable(reservationId, userId, couponId, orderAmount, orderId,
                            "EMPTY_RESPONSE")
                    : result;
        } catch (RuntimeException ex) {
            return CouponReservation.unavailable(reservationId, userId, couponId, orderAmount, orderId,
                    "COUPON_RESERVATION_FALLBACK");
        }
    }

    public boolean confirmCoupon(String reservationId, String couponId, long orderId) {
        if (couponId == null || couponId.isBlank()) {
            return true;
        }
        try {
            CouponReservation result;
            if (dubboEnabled()) {
                result = toLocal(marketingRpcService
                        .confirmCoupon(new CouponConfirmationCommand(reservationId, couponId, orderId)));
            } else {
                CouponReservationResponse response = marketingRestClient.post()
                        .uri("/api/marketing/coupons/{couponId}/reservations/{reservationId}/confirm", couponId,
                                reservationId)
                        .body(new CouponReservationDecisionRequest(orderId)).retrieve()
                        .body(CouponReservationResponse.class);
                result = response == null ? null : response.data();
            }
            return result != null && result.used();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public boolean releaseCoupon(String reservationId, String couponId, long orderId) {
        if (couponId == null || couponId.isBlank()) {
            return true;
        }
        try {
            CouponReservation result;
            if (dubboEnabled()) {
                result = toLocal(
                        marketingRpcService.releaseCoupon(new CouponReleaseCommand(reservationId, couponId, orderId)));
            } else {
                CouponReservationResponse response = marketingRestClient.post()
                        .uri("/api/marketing/coupons/{couponId}/reservations/{reservationId}/release", couponId,
                                reservationId)
                        .body(new CouponReservationDecisionRequest(orderId)).retrieve()
                        .body(CouponReservationResponse.class);
                result = response == null ? null : response.data();
            }
            return result != null && result.available();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean dubboEnabled() {
        return "dubbo".equalsIgnoreCase(rpcProtocol) && marketingRpcService != null;
    }

    private PromotionQuote toLocal(PromotionQuoteView view) {
        return view == null
                ? null
                : new PromotionQuote(view.userId(), view.orderAmount(), view.discountAmount(), view.payableAmount(),
                        view.couponId(), view.quotedAt());
    }

    private CouponReservation toLocal(CouponReservationView view) {
        return view == null
                ? null
                : new CouponReservation(view.reservationId(), view.userId(), view.couponId(), view.status(),
                        view.discountAmount(), view.orderId(), view.updatedAt(), null);
    }

    public record PromotionQuoteRequest(long userId, BigDecimal orderAmount) {
    }

    public record CouponReservationRequest(String reservationId, long userId, BigDecimal orderAmount, long orderId) {
    }

    public record CouponReservationDecisionRequest(long orderId) {
    }

    public record PromotionQuote(long userId, BigDecimal orderAmount, BigDecimal discountAmount,
            BigDecimal payableAmount, String couponId, Instant quotedAt) {
        public static PromotionQuote none(long userId, BigDecimal orderAmount) {
            return new PromotionQuote(userId, orderAmount, BigDecimal.ZERO, orderAmount, null, Instant.now());
        }
    }

    public record PromotionQuoteResponse(boolean success, String code, String message, PromotionQuote data,
            Instant timestamp) {
    }

    public record CouponReservation(String reservationId, long userId, String couponId, String status,
            BigDecimal discountAmount, long orderId, Instant updatedAt, String reason) {
        public boolean reserved() {
            return "RESERVED".equals(status);
        }

        public boolean used() {
            return "USED".equals(status);
        }

        public boolean available() {
            return "AVAILABLE".equals(status);
        }

        public static CouponReservation none(String reservationId, long userId, BigDecimal orderAmount, long orderId) {
            return new CouponReservation(reservationId, userId, null, "AVAILABLE", BigDecimal.ZERO, orderId,
                    Instant.now(), null);
        }

        public static CouponReservation unavailable(String reservationId, long userId, String couponId,
                BigDecimal orderAmount, long orderId, String reason) {
            return new CouponReservation(reservationId, userId, couponId, "UNAVAILABLE", BigDecimal.ZERO, orderId,
                    Instant.now(), reason);
        }
    }

    public record CouponReservationResponse(boolean success, String code, String message, CouponReservation data,
            Instant timestamp) {
    }
}
