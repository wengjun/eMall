package com.emall.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.rpc.OrderPaymentSnapshot;
import com.emall.payment.domain.PaymentRefundStatus;
import com.emall.payment.domain.PaymentReconciliationRecord;
import com.emall.payment.domain.PaymentStatus;
import com.emall.payment.domain.ReconciliationStatus;
import com.emall.payment.domain.StatementType;
import com.emall.payment.channel.ChannelOperationStatus;
import com.emall.payment.channel.ChannelPaymentResult;
import com.emall.payment.channel.ChannelRefundResult;
import com.emall.payment.channel.PaymentChannelClient;
import com.emall.payment.domain.PaymentRefundOrder;
import com.emall.payment.integration.OrderClient;
import com.emall.payment.repository.InMemoryOutboxRepository;
import com.emall.payment.repository.InMemoryPaymentRepository;
import com.emall.payment.repository.InMemoryPaymentSettlementRepository;
import com.emall.payment.security.PaymentCallbackVerifier;
import com.emall.payment.security.PaymentCallbackReplayGuard;
import com.emall.payment.security.PaymentSecurityProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PaymentServiceTest {

    @Test
    void reconcilesMatchedPaymentStatement() {
        PaymentService paymentService = newPaymentService();
        var payment = paymentService.create("request-1", 1001L, 2001L, new BigDecimal("99.00"), "alipay");
        long paymentId = payment.paymentId();
        paymentService.callback(payment.channelTradeNo(), paymentId, new BigDecimal("99.00"));
        long statementId = paymentService.ingestChannelStatement("alipay", payment.channelTradeNo(), paymentId,
                new BigDecimal("99.00"), StatementType.PAYMENT, Instant.now()).statementId();

        PaymentReconciliationRecord record = paymentService.reconcileStatement(statementId);

        assertThat(record.status()).isEqualTo(ReconciliationStatus.MATCHED);
        assertThat(paymentService.findUnreconciledStatements(10)).isEmpty();
    }

    @Test
    void recordsAmountMismatchForPaymentStatement() {
        PaymentService paymentService = newPaymentService();
        var payment = paymentService.create("request-2", 1002L, 2002L, new BigDecimal("99.00"), "wechat");
        long paymentId = payment.paymentId();
        paymentService.callback(payment.channelTradeNo(), paymentId, new BigDecimal("99.00"));
        long statementId = paymentService.ingestChannelStatement("wechat", payment.channelTradeNo(), paymentId,
                new BigDecimal("98.00"), StatementType.PAYMENT, Instant.now()).statementId();

        PaymentReconciliationRecord record = paymentService.reconcileStatement(statementId);

        assertThat(record.status()).isEqualTo(ReconciliationStatus.AMOUNT_MISMATCH);
    }

    @Test
    void reconcilesMatchedRefundStatement() {
        PaymentService paymentService = newPaymentService();
        var payment = paymentService.create("request-3", 1003L, 2003L, new BigDecimal("35.50"), "alipay");
        long paymentId = payment.paymentId();
        paymentService.callback(payment.channelTradeNo(), paymentId, new BigDecimal("35.50"));
        paymentService.refund(paymentId);
        PaymentRefundOrder refund = paymentService.findCreatedRefunds(1).get(0);
        paymentService.submitRefund(refund.refundId());
        paymentService.refreshRefund(refund.refundId());
        long statementId = paymentService.ingestChannelStatement("alipay", payment.channelTradeNo(), paymentId,
                new BigDecimal("35.50"), StatementType.REFUND, Instant.now()).statementId();

        PaymentReconciliationRecord record = paymentService.reconcileStatement(statementId);

        assertThat(record.status()).isEqualTo(ReconciliationStatus.MATCHED);
    }

    @Test
    void rejectsSameRequestIdWithDifferentPaymentPayload() {
        PaymentService paymentService = newPaymentService();
        paymentService.create("request-4", 1004L, 2004L, new BigDecimal("35.50"), "alipay");

        assertThatThrownBy(() -> paymentService.create("request-4", 1004L, 2004L, new BigDecimal("35.50"), "wechat"))
                .hasMessageContaining("idempotency key already used");
    }

    @Test
    void rejectsClientAmountsAndOwnersThatDoNotMatchServerOrderSnapshot() {
        PaymentService paymentService = newPaymentService();

        assertThatThrownBy(() -> paymentService.create("wrong-amount", 1001L, 2001L, new BigDecimal("0.01"), "alipay"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("payable amount");
        assertThatThrownBy(() -> paymentService.create("wrong-owner", 1001L, 9999L, new BigDecimal("99.00"), "alipay"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("does not belong");
    }

    @Test
    void rejectsUnsupportedServerOrderCurrency() {
        PaymentService paymentService = newPaymentServiceWithOrderClient(new CurrencyOrderClient());

        assertThatThrownBy(
                () -> paymentService.create("wrong-currency", 1001L, 2001L, new BigDecimal("99.00"), "alipay"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("currency");
    }

    @Test
    void verifiesIndependentCallbacksAcrossPaymentChannels() {
        Instant now = Instant.parse("2026-05-19T00:00:00Z");
        PaymentSecurityProperties properties = new PaymentSecurityProperties();
        properties.setCallbackSecrets(Map.of("default", "test-payment-callback-secret-32-bytes"));
        PaymentCallbackVerifier verifier = new PaymentCallbackVerifier(properties, Clock.fixed(now, ZoneOffset.UTC));
        PaymentService paymentService = newPaymentService(verifier);
        var alipay = paymentService.create("request-channel-1", 1007L, 2007L, new BigDecimal("12.00"), "alipay");
        var wechat = paymentService.create("request-channel-2", 1008L, 2008L, new BigDecimal("12.00"), "wechat");
        String alipaySignature = verifier.sign("alipay", alipay.channelTradeNo(), alipay.paymentId(),
                new BigDecimal("12.00"), now, "nonce-a");
        String wechatSignature = verifier.sign("wechat", wechat.channelTradeNo(), wechat.paymentId(),
                new BigDecimal("12.00"), now, "nonce-b");

        assertThat(paymentService.callback(new PaymentCallbackCommand("alipay", alipay.channelTradeNo(),
                alipay.paymentId(), new BigDecimal("12.00"), now, "nonce-a", alipaySignature)).status())
                .isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(paymentService.callback(new PaymentCallbackCommand("wechat", wechat.channelTradeNo(),
                wechat.paymentId(), new BigDecimal("12.00"), now, "nonce-b", wechatSignature)).status())
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void persistsRefundOrderStateMachineWhenRefundingPayment() {
        InMemoryPaymentSettlementRepository settlementRepository = new InMemoryPaymentSettlementRepository();
        PaymentService paymentService = new PaymentService(new InMemoryPaymentRepository(), settlementRepository,
                new InMemoryOutboxRepository(), new SnowflakeIdGenerator(1), new NoopOrderClient());
        var payment = paymentService.create("request-refund", 1009L, 2009L, new BigDecimal("19.00"), "alipay");
        long paymentId = payment.paymentId();
        paymentService.callback(payment.channelTradeNo(), paymentId, new BigDecimal("19.00"));

        paymentService.refund(paymentId);
        long refundId = settlementRepository.findRefundByRequestId("refund-" + paymentId).orElseThrow().refundId();
        paymentService.submitRefund(refundId);
        paymentService.refreshRefund(refundId);

        assertThat(settlementRepository.findRefundByRequestId("refund-" + paymentId)).isPresent().get()
                .extracting(refund -> refund.status()).isEqualTo(PaymentRefundStatus.SUCCEEDED);
    }

    @Test
    void retriesUnconfirmedOrderAfterPaymentCallbackFailure() {
        RecordingOrderClient orderClient = new RecordingOrderClient();
        PaymentService paymentService = newPaymentServiceWithOrderClient(orderClient);
        var payment = paymentService.create("request-retry-confirm", 1010L, 2010L, new BigDecimal("21.00"), "alipay");
        long paymentId = payment.paymentId();

        PaymentStatus callbackStatus =
                paymentService.callback(payment.channelTradeNo(), paymentId, new BigDecimal("21.00")).status();
        assertThat(paymentService.findSucceededButUnconfirmed(10)).extracting(candidate -> candidate.paymentId())
                .contains(paymentId);

        orderClient.confirmOrders = true;
        var confirmed = paymentService.retryOrderConfirmation(paymentId);

        assertThat(callbackStatus).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(confirmed.orderConfirmed()).isTrue();
        assertThat(orderClient.payOrderCalls).isEqualTo(2);
    }

    @Test
    void verifiesSignedPaymentCallback() {
        Instant now = Instant.parse("2026-05-19T00:00:00Z");
        PaymentSecurityProperties properties = new PaymentSecurityProperties();
        properties.setCallbackSecrets(Map.of("default", "test-payment-callback-secret-32-bytes"));
        PaymentCallbackVerifier verifier = new PaymentCallbackVerifier(properties, Clock.fixed(now, ZoneOffset.UTC));
        PaymentService paymentService = newPaymentService(verifier);
        var payment = paymentService.create("request-5", 1005L, 2005L, new BigDecimal("66.00"), "alipay");
        long paymentId = payment.paymentId();
        String signature =
                verifier.sign("alipay", payment.channelTradeNo(), paymentId, new BigDecimal("66.00"), now, "nonce-5");

        assertThat(paymentService.callback(new PaymentCallbackCommand("alipay", payment.channelTradeNo(), paymentId,
                new BigDecimal("66.00"), now, "nonce-5", signature)).status().name()).isEqualTo("SUCCEEDED");
        assertThatThrownBy(() -> paymentService.callback(new PaymentCallbackCommand("alipay", "trade-6", paymentId,
                new BigDecimal("66.00"), now, "nonce-6", "bad-signature"))).hasMessageContaining("signature");
    }

    @Test
    void rejectsSignedCallbacksWithWrongChannelAmountTradeOrReplayedNonce() {
        Instant now = Instant.parse("2026-05-19T00:00:00Z");
        PaymentSecurityProperties properties = securityProperties();
        PaymentCallbackVerifier verifier = new PaymentCallbackVerifier(properties, Clock.fixed(now, ZoneOffset.UTC));
        PaymentService paymentService = newPaymentService(verifier);
        var payment = paymentService.create("callback-validation", 1007L, 2007L, new BigDecimal("12.00"), "alipay");

        assertRejectedCallback(paymentService, verifier, payment.paymentId(), "wechat", payment.channelTradeNo(),
                new BigDecimal("12.00"), now, "nonce-channel", "confirmation");
        assertRejectedCallback(paymentService, verifier, payment.paymentId(), "alipay", payment.channelTradeNo(),
                new BigDecimal("11.00"), now, "nonce-amount", "confirmation");
        assertRejectedCallback(paymentService, verifier, payment.paymentId(), "alipay", "unknown-trade",
                new BigDecimal("12.00"), now, "nonce-trade", "confirmation");

        String signature = verifier.sign("alipay", payment.channelTradeNo(), payment.paymentId(),
                new BigDecimal("12.00"), now, "nonce-replay");
        paymentService.callback(new PaymentCallbackCommand("alipay", payment.channelTradeNo(), payment.paymentId(),
                new BigDecimal("12.00"), now, "nonce-replay", signature));
        String replaySignature = verifier.sign("alipay", payment.channelTradeNo(), payment.paymentId(),
                new BigDecimal("11.00"), now, "nonce-replay");
        assertThatThrownBy(() -> paymentService.callback(new PaymentCallbackCommand("alipay", payment.channelTradeNo(),
                payment.paymentId(), new BigDecimal("11.00"), now, "nonce-replay", replaySignature)))
                .hasMessageContaining("nonce");
    }

    @Test
    void queuesRefundBeforeCallingChannelAndRecoversWhenCallbackWasLost() {
        InMemoryPaymentSettlementRepository settlements = new InMemoryPaymentSettlementRepository();
        ControlledPaymentChannelClient channel = new ControlledPaymentChannelClient();
        PaymentService paymentService = newPaymentService(settlements, channel);
        var payment = paymentService.create("async-refund", 1009L, 2009L, new BigDecimal("19.00"), "alipay");
        paymentService.callback(payment.channelTradeNo(), payment.paymentId(), payment.amount());

        PaymentStatus queued = paymentService.refund(payment.paymentId()).status();
        PaymentRefundOrder refund = settlements.findRefundByRequestId("refund-" + payment.paymentId()).orElseThrow();

        assertThat(queued).isEqualTo(PaymentStatus.REFUNDING);
        assertThat(refund.status()).isEqualTo(PaymentRefundStatus.CREATED);
        assertThat(channel.refundRequests).isZero();
        assertThat(paymentService.submitRefund(refund.refundId()).status()).isEqualTo(PaymentStatus.REFUNDING);
        assertThat(paymentService.refreshRefund(refund.refundId()).status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(channel.refundRequests).isEqualTo(1);
    }

    @Test
    void restoresPaymentAfterDefinitiveChannelRefundFailure() {
        InMemoryPaymentSettlementRepository settlements = new InMemoryPaymentSettlementRepository();
        ControlledPaymentChannelClient channel = new ControlledPaymentChannelClient();
        channel.refundRequestStatus = ChannelOperationStatus.FAILED;
        PaymentService paymentService = newPaymentService(settlements, channel);
        var payment = paymentService.create("failed-refund", 1010L, 2010L, new BigDecimal("21.00"), "alipay");
        paymentService.callback(payment.channelTradeNo(), payment.paymentId(), payment.amount());
        paymentService.refund(payment.paymentId());
        PaymentRefundOrder refund = settlements.findRefundByRequestId("refund-" + payment.paymentId()).orElseThrow();

        assertThat(paymentService.submitRefund(refund.refundId()).status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(settlements.findRefundById(refund.refundId()).orElseThrow().status())
                .isEqualTo(PaymentRefundStatus.FAILED);
    }

    @Test
    void retriesCreatedRefundAfterChannelTimeoutWithoutDuplicatingLocalState() {
        InMemoryPaymentSettlementRepository settlements = new InMemoryPaymentSettlementRepository();
        ControlledPaymentChannelClient channel = new ControlledPaymentChannelClient();
        channel.refundFailuresRemaining = 1;
        PaymentService paymentService = newPaymentService(settlements, channel);
        var payment = paymentService.create("timeout-refund", 1010L, 2010L, new BigDecimal("21.00"), "alipay");
        paymentService.callback(payment.channelTradeNo(), payment.paymentId(), payment.amount());
        paymentService.refund(payment.paymentId());
        PaymentRefundOrder refund = settlements.findRefundByRequestId("refund-" + payment.paymentId()).orElseThrow();

        assertThatThrownBy(() -> paymentService.submitRefund(refund.refundId())).isInstanceOf(BusinessException.class)
                .hasMessageContaining("timeout");
        assertThat(settlements.findRefundById(refund.refundId()).orElseThrow().status())
                .isEqualTo(PaymentRefundStatus.CREATED);
        assertThat(paymentService.submitRefund(refund.refundId()).status()).isEqualTo(PaymentStatus.REFUNDING);
        assertThat(paymentService.refreshRefund(refund.refundId()).status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(channel.refundRequests).isEqualTo(2);
    }

    @Test
    void redactsPaymentSecretsInDiagnosticStrings() {
        Instant now = Instant.parse("2026-05-19T00:00:00Z");
        PaymentCallbackCommand command = new PaymentCallbackCommand("alipay", "trade-sensitive-123456", 1001L,
                new BigDecimal("66.00"), now, "nonce-sensitive", "signature-sensitive");
        PaymentService paymentService = newPaymentService();
        var payment = paymentService.create("request-6", 1006L, 2006L, new BigDecimal("66.00"), "alipay");
        long paymentId = payment.paymentId();

        String paymentText =
                paymentService.callback(payment.channelTradeNo(), paymentId, new BigDecimal("66.00")).toString();

        assertThat(command.toString()).doesNotContain("signature-sensitive", "nonce-sensitive",
                "trade-sensitive-123456");
        assertThat(paymentText).doesNotContain("trade-sensitive-123456");
    }

    private PaymentService newPaymentService() {
        return newPaymentService(null);
    }

    private PaymentService newPaymentService(PaymentCallbackVerifier verifier) {
        PaymentCallbackVerifier callbackVerifier = verifier;
        if (callbackVerifier == null) {
            PaymentSecurityProperties properties = new PaymentSecurityProperties();
            callbackVerifier = new PaymentCallbackVerifier(properties, Clock.systemUTC());
        }
        return new PaymentService(new InMemoryPaymentRepository(), new InMemoryPaymentSettlementRepository(),
                new InMemoryOutboxRepository(), new SnowflakeIdGenerator(1), new NoopOrderClient(), callbackVerifier);
    }

    private PaymentService newPaymentService(InMemoryPaymentSettlementRepository settlements,
            PaymentChannelClient channel) {
        PaymentSecurityProperties properties = securityProperties();
        return new PaymentService(new InMemoryPaymentRepository(), settlements, new InMemoryOutboxRepository(),
                new SnowflakeIdGenerator(1), new NoopOrderClient(),
                new PaymentCallbackVerifier(properties, Clock.systemUTC()), new PaymentCallbackReplayGuard(properties),
                channel);
    }

    private PaymentSecurityProperties securityProperties() {
        PaymentSecurityProperties properties = new PaymentSecurityProperties();
        properties.setCallbackSecrets(Map.of("default", "test-payment-callback-secret-32-bytes"));
        return properties;
    }

    private void assertRejectedCallback(PaymentService paymentService, PaymentCallbackVerifier verifier, long paymentId,
            String channel, String tradeNo, BigDecimal amount, Instant timestamp, String nonce,
            String expectedMessage) {
        String signature = verifier.sign(channel, tradeNo, paymentId, amount, timestamp, nonce);
        assertThatThrownBy(() -> paymentService
                .callback(new PaymentCallbackCommand(channel, tradeNo, paymentId, amount, timestamp, nonce, signature)))
                .hasMessageContaining(expectedMessage);
    }

    private PaymentService newPaymentServiceWithOrderClient(OrderClient orderClient) {
        return new PaymentService(new InMemoryPaymentRepository(), new InMemoryPaymentSettlementRepository(),
                new InMemoryOutboxRepository(), new SnowflakeIdGenerator(1), orderClient);
    }

    private static class NoopOrderClient extends OrderClient {
        private NoopOrderClient() {
            super(RestClient.builder().baseUrl("http://localhost").build());
        }

        @Override
        public boolean payOrder(long orderId) {
            return true;
        }

        @Override
        public OrderPaymentSnapshot paymentSnapshot(long orderId) {
            return new OrderPaymentSnapshot(orderId, orderId + 1000, amountFor(orderId), "CNY", "CREATED");
        }

        @Override
        public boolean confirmPayment(long orderId, long paymentId, BigDecimal paidAmount, String currency,
                String channelTradeNo) {
            return true;
        }

        private BigDecimal amountFor(long orderId) {
            return switch ((int) orderId) {
                case 1001, 1002 -> new BigDecimal("99.00");
                case 1003, 1004 -> new BigDecimal("35.50");
                case 1005, 1006 -> new BigDecimal("66.00");
                case 1007, 1008 -> new BigDecimal("12.00");
                case 1009 -> new BigDecimal("19.00");
                case 1010 -> new BigDecimal("21.00");
                default -> throw new IllegalArgumentException("unexpected test order: " + orderId);
            };
        }
    }

    private static final class CurrencyOrderClient extends NoopOrderClient {
        @Override
        public OrderPaymentSnapshot paymentSnapshot(long orderId) {
            return new OrderPaymentSnapshot(orderId, 2001L, new BigDecimal("99.00"), "USD", "CREATED");
        }
    }

    private static final class RecordingOrderClient extends OrderClient {
        private boolean confirmOrders;
        private int payOrderCalls;

        private RecordingOrderClient() {
            super(RestClient.builder().baseUrl("http://localhost").build());
        }

        @Override
        public boolean payOrder(long orderId) {
            payOrderCalls++;
            return confirmOrders;
        }

        @Override
        public OrderPaymentSnapshot paymentSnapshot(long orderId) {
            return new OrderPaymentSnapshot(orderId, 2010L, new BigDecimal("21.00"), "CNY", "CREATED");
        }

        @Override
        public boolean confirmPayment(long orderId, long paymentId, BigDecimal paidAmount, String currency,
                String channelTradeNo) {
            payOrderCalls++;
            return confirmOrders;
        }
    }

    private static final class ControlledPaymentChannelClient implements PaymentChannelClient {
        private final Map<String, ChannelPaymentResult> payments = new java.util.concurrent.ConcurrentHashMap<>();
        private ChannelOperationStatus refundRequestStatus = ChannelOperationStatus.PROCESSING;
        private int refundFailuresRemaining;
        private int refundRequests;

        @Override
        public ChannelPaymentResult createPayment(String requestId, long paymentId, long orderId, BigDecimal amount,
                String currency, String channel) {
            ChannelPaymentResult result = new ChannelPaymentResult("controlled-pay-" + paymentId,
                    ChannelOperationStatus.PROCESSING, amount, currency, "created");
            payments.put(channel + ':' + result.channelTradeNo(), result);
            return result;
        }

        @Override
        public ChannelPaymentResult queryPayment(String channel, String channelTradeNo) {
            ChannelPaymentResult result = payments.get(channel + ':' + channelTradeNo);
            return result == null
                    ? new ChannelPaymentResult(channelTradeNo, ChannelOperationStatus.FAILED, BigDecimal.ZERO, "CNY",
                            "not found")
                    : new ChannelPaymentResult(channelTradeNo, ChannelOperationStatus.SUCCEEDED, result.amount(),
                            result.currency(), "confirmed");
        }

        @Override
        public ChannelRefundResult requestRefund(String requestId, long refundId, String channel, String channelTradeNo,
                BigDecimal amount, String currency) {
            refundRequests++;
            if (refundFailuresRemaining > 0) {
                refundFailuresRemaining--;
                throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE, "payment channel timeout");
            }
            return new ChannelRefundResult("controlled-refund-" + refundId, refundRequestStatus, "requested");
        }

        @Override
        public ChannelRefundResult queryRefund(String channel, String channelRefundNo) {
            return new ChannelRefundResult(channelRefundNo, ChannelOperationStatus.SUCCEEDED, "confirmed");
        }
    }
}
