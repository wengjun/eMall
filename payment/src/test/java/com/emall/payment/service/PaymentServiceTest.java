package com.emall.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.payment.domain.PaymentRefundStatus;
import com.emall.payment.domain.PaymentReconciliationRecord;
import com.emall.payment.domain.PaymentStatus;
import com.emall.payment.domain.ReconciliationStatus;
import com.emall.payment.domain.StatementType;
import com.emall.payment.integration.OrderClient;
import com.emall.payment.repository.InMemoryOutboxRepository;
import com.emall.payment.repository.InMemoryPaymentRepository;
import com.emall.payment.repository.InMemoryPaymentSettlementRepository;
import com.emall.payment.security.PaymentCallbackVerifier;
import com.emall.payment.security.PaymentSecurityProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PaymentServiceTest {

    @Test
    void reconcilesMatchedPaymentStatement() {
        PaymentService paymentService = newPaymentService();
        long paymentId =
                paymentService.create("request-1", 1001L, 2001L, new BigDecimal("99.00"), "alipay").paymentId();
        paymentService.callback("trade-1", paymentId, new BigDecimal("99.00"));
        long statementId = paymentService.ingestChannelStatement("alipay", "trade-1", paymentId,
                new BigDecimal("99.00"), StatementType.PAYMENT, Instant.now()).statementId();

        PaymentReconciliationRecord record = paymentService.reconcileStatement(statementId);

        assertThat(record.status()).isEqualTo(ReconciliationStatus.MATCHED);
        assertThat(paymentService.findUnreconciledStatements(10)).isEmpty();
    }

    @Test
    void recordsAmountMismatchForPaymentStatement() {
        PaymentService paymentService = newPaymentService();
        long paymentId =
                paymentService.create("request-2", 1002L, 2002L, new BigDecimal("99.00"), "wechat").paymentId();
        paymentService.callback("trade-2", paymentId, new BigDecimal("99.00"));
        long statementId = paymentService.ingestChannelStatement("wechat", "trade-2", paymentId,
                new BigDecimal("98.00"), StatementType.PAYMENT, Instant.now()).statementId();

        PaymentReconciliationRecord record = paymentService.reconcileStatement(statementId);

        assertThat(record.status()).isEqualTo(ReconciliationStatus.AMOUNT_MISMATCH);
    }

    @Test
    void reconcilesMatchedRefundStatement() {
        PaymentService paymentService = newPaymentService();
        long paymentId =
                paymentService.create("request-3", 1003L, 2003L, new BigDecimal("35.50"), "alipay").paymentId();
        paymentService.callback("trade-3", paymentId, new BigDecimal("35.50"));
        paymentService.refund(paymentId);
        long statementId = paymentService.ingestChannelStatement("alipay", "trade-3", paymentId,
                new BigDecimal("35.50"), StatementType.REFUND, Instant.now()).statementId();

        PaymentReconciliationRecord record = paymentService.reconcileStatement(statementId);

        assertThat(record.status()).isEqualTo(ReconciliationStatus.MATCHED);
    }

    @Test
    void rejectsSameRequestIdWithDifferentPaymentPayload() {
        PaymentService paymentService = newPaymentService();
        paymentService.create("request-4", 1004L, 2004L, new BigDecimal("35.50"), "alipay");

        assertThatThrownBy(() -> paymentService.create("request-4", 1005L, 2004L, new BigDecimal("35.50"), "alipay"))
                .hasMessageContaining("idempotency key already used");
    }

    @Test
    void allowsSameChannelTradeNoAcrossDifferentPaymentChannels() {
        Instant now = Instant.parse("2026-05-19T00:00:00Z");
        PaymentSecurityProperties properties = new PaymentSecurityProperties();
        properties.setCallbackSecret("test-secret");
        PaymentCallbackVerifier verifier = new PaymentCallbackVerifier(properties, Clock.fixed(now, ZoneOffset.UTC));
        PaymentService paymentService = newPaymentService(verifier);
        long alipayPaymentId =
                paymentService.create("request-channel-1", 1007L, 2007L, new BigDecimal("12.00"), "alipay").paymentId();
        long wechatPaymentId =
                paymentService.create("request-channel-2", 1008L, 2008L, new BigDecimal("12.00"), "wechat").paymentId();
        String alipaySignature =
                verifier.sign("alipay", "shared-trade", alipayPaymentId, new BigDecimal("12.00"), now, "nonce-a");
        String wechatSignature =
                verifier.sign("wechat", "shared-trade", wechatPaymentId, new BigDecimal("12.00"), now, "nonce-b");

        assertThat(paymentService.callback(new PaymentCallbackCommand("alipay", "shared-trade", alipayPaymentId,
                new BigDecimal("12.00"), now, "nonce-a", alipaySignature)).status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(paymentService.callback(new PaymentCallbackCommand("wechat", "shared-trade", wechatPaymentId,
                new BigDecimal("12.00"), now, "nonce-b", wechatSignature)).status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void persistsRefundOrderStateMachineWhenRefundingPayment() {
        InMemoryPaymentSettlementRepository settlementRepository = new InMemoryPaymentSettlementRepository();
        PaymentService paymentService = new PaymentService(new InMemoryPaymentRepository(), settlementRepository,
                new InMemoryOutboxRepository(), new SnowflakeIdGenerator(1), new NoopOrderClient());
        long paymentId =
                paymentService.create("request-refund", 1009L, 2009L, new BigDecimal("19.00"), "alipay").paymentId();
        paymentService.callback("trade-refund", paymentId, new BigDecimal("19.00"));

        paymentService.refund(paymentId);

        assertThat(settlementRepository.findRefundByRequestId("refund-" + paymentId)).isPresent().get()
                .extracting(refund -> refund.status()).isEqualTo(PaymentRefundStatus.SUCCEEDED);
    }

    @Test
    void retriesUnconfirmedOrderAfterPaymentCallbackFailure() {
        RecordingOrderClient orderClient = new RecordingOrderClient();
        PaymentService paymentService = newPaymentServiceWithOrderClient(orderClient);
        long paymentId = paymentService.create("request-retry-confirm", 1010L, 2010L, new BigDecimal("21.00"), "alipay")
                .paymentId();

        PaymentStatus callbackStatus =
                paymentService.callback("trade-retry-confirm", paymentId, new BigDecimal("21.00")).status();
        assertThat(paymentService.findSucceededButUnconfirmed(10)).extracting(payment -> payment.paymentId())
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
        properties.setCallbackSecret("test-secret");
        PaymentCallbackVerifier verifier = new PaymentCallbackVerifier(properties, Clock.fixed(now, ZoneOffset.UTC));
        PaymentService paymentService = newPaymentService(verifier);
        long paymentId =
                paymentService.create("request-5", 1005L, 2005L, new BigDecimal("66.00"), "alipay").paymentId();
        String signature = verifier.sign("alipay", "trade-5", paymentId, new BigDecimal("66.00"), now, "nonce-5");

        assertThat(paymentService.callback(new PaymentCallbackCommand("alipay", "trade-5", paymentId,
                new BigDecimal("66.00"), now, "nonce-5", signature)).status().name()).isEqualTo("SUCCEEDED");
        assertThatThrownBy(() -> paymentService.callback(new PaymentCallbackCommand("alipay", "trade-6", paymentId,
                new BigDecimal("66.00"), now, "nonce-6", "bad-signature"))).hasMessageContaining("signature");
    }

    @Test
    void redactsPaymentSecretsInDiagnosticStrings() {
        Instant now = Instant.parse("2026-05-19T00:00:00Z");
        PaymentCallbackCommand command = new PaymentCallbackCommand("alipay", "trade-sensitive-123456", 1001L,
                new BigDecimal("66.00"), now, "nonce-sensitive", "signature-sensitive");
        PaymentService paymentService = newPaymentService();
        long paymentId =
                paymentService.create("request-6", 1006L, 2006L, new BigDecimal("66.00"), "alipay").paymentId();

        String paymentText =
                paymentService.callback("trade-sensitive-123456", paymentId, new BigDecimal("66.00")).toString();

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

    private PaymentService newPaymentServiceWithOrderClient(OrderClient orderClient) {
        return new PaymentService(new InMemoryPaymentRepository(), new InMemoryPaymentSettlementRepository(),
                new InMemoryOutboxRepository(), new SnowflakeIdGenerator(1), orderClient);
    }

    private static final class NoopOrderClient extends OrderClient {
        private NoopOrderClient() {
            super(RestClient.builder().baseUrl("http://localhost").build());
        }

        @Override
        public boolean payOrder(long orderId) {
            return true;
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
    }
}
