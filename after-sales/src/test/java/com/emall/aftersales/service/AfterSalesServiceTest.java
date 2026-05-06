package com.emall.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.aftersales.domain.AfterSalesRequest;
import com.emall.aftersales.domain.AfterSalesStatus;
import com.emall.aftersales.domain.AfterSalesType;
import com.emall.aftersales.repository.InMemoryAfterSalesRepository;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AfterSalesServiceTest {
    private final AfterSalesService afterSalesService = new AfterSalesService(
            new InMemoryAfterSalesRepository(),
            new SnowflakeIdGenerator(6));

    @Test
    void shouldApproveReceiveAndRefundReturnRequest() {
        AfterSalesRequest created = afterSalesService.create(90001L, 70001L, 30001L, 1,
                new BigDecimal("3799.00"), AfterSalesType.RETURN_AND_REFUND, "quality issue");

        AfterSalesRequest approved = afterSalesService.approve(created.requestId());
        AfterSalesRequest received = afterSalesService.receive(created.requestId());
        AfterSalesRequest refunded = afterSalesService.refund(created.requestId());

        assertThat(approved.status()).isEqualTo(AfterSalesStatus.APPROVED);
        assertThat(received.status()).isEqualTo(AfterSalesStatus.RECEIVED);
        assertThat(refunded.status()).isEqualTo(AfterSalesStatus.REFUNDED);
    }
}
