package com.emall.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FinanceServiceTest {
    private final InMemoryFinanceRepository repository = new InMemoryFinanceRepository();
    private final FinanceService service = new FinanceService(repository, new SnowflakeIdGenerator(42L));

    @Test
    void postsLedgerEntriesAndCreatesSettlementArtifacts() {
        FinanceAccount account = service.createAccount(AccountType.MERCHANT, 1001L, "usd");
        service.postEntry(account.accountId(), "order", "order-1", BigDecimal.ZERO, new BigDecimal("100.00"));
        FinanceAccount frozen = service.freezeFunds(account.accountId(), new BigDecimal("20.00"));
        SettlementBatch batch = service.createSettlementBatch(1001L, new BigDecimal("80.00"),
                new BigDecimal("5.00"), LocalDate.now());
        InvoiceDocument invoice = service.issueInvoice(1001L, new BigDecimal("80.00"), "tax-1");
        service.recordClearingFile("card", LocalDate.now(), new BigDecimal("100.00"), true);
        service.openChargeback(9001L, new BigDecimal("10.00"), "customer dispute");

        assertThat(frozen.frozenAmount()).isEqualByComparingTo("20.00");
        assertThat(batch.status()).isEqualTo(SettlementStatus.CREATED);
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(service.summary().openChargebacks()).isEqualTo(1);
    }
}
