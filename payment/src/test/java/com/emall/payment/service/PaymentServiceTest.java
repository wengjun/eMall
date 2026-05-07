package com.emall.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.payment.domain.PaymentReconciliationRecord;
import com.emall.payment.domain.ReconciliationStatus;
import com.emall.payment.domain.StatementType;
import com.emall.payment.integration.OrderClient;
import com.emall.payment.repository.InMemoryOutboxRepository;
import com.emall.payment.repository.InMemoryPaymentRepository;
import com.emall.payment.repository.InMemoryPaymentSettlementRepository;
import java.math.BigDecimal;
import java.time.Instant;
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

    private PaymentService newPaymentService() {
        return new PaymentService(new InMemoryPaymentRepository(), new InMemoryPaymentSettlementRepository(),
                new InMemoryOutboxRepository(), new SnowflakeIdGenerator(1), new NoopOrderClient());
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
}
