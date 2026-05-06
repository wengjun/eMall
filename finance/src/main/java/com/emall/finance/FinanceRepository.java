package com.emall.finance;

import java.util.List;
import java.util.Optional;

interface FinanceRepository {
    FinanceAccount saveAccount(FinanceAccount account);

    Optional<FinanceAccount> findAccount(long accountId);

    List<FinanceAccount> findAccounts();

    LedgerEntry saveEntry(LedgerEntry entry);

    List<LedgerEntry> findEntries(long accountId);

    SettlementBatch saveSettlementBatch(SettlementBatch batch);

    Optional<SettlementBatch> findSettlementBatch(long batchId);

    List<SettlementBatch> findSettlementBatches(long merchantId);

    InvoiceDocument saveInvoice(InvoiceDocument invoice);

    Optional<InvoiceDocument> findInvoice(long invoiceId);

    ClearingFile saveClearingFile(ClearingFile file);

    ChargebackCase saveChargeback(ChargebackCase chargeback);

    Optional<ChargebackCase> findChargeback(long chargebackId);

    List<ChargebackCase> findChargebacks();
}
