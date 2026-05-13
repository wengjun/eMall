package com.emall.finance;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusFinanceRepository implements FinanceRepository {
    private final FinanceMapper financeMapper;

    MybatisPlusFinanceRepository(FinanceMapper financeMapper) {
        this.financeMapper = financeMapper;
    }

    @Override
    public FinanceAccount saveAccount(FinanceAccount account) {
        financeMapper.saveAccount(account);
        return account;
    }

    @Override
    public Optional<FinanceAccount> findAccount(long accountId) {
        return Optional.ofNullable(financeMapper.findAccount(accountId));
    }

    @Override
    public List<FinanceAccount> findAccounts() {
        return financeMapper.findAccounts();
    }

    @Override
    public LedgerEntry saveEntry(LedgerEntry entry) {
        financeMapper.saveEntry(entry);
        return entry;
    }

    @Override
    public List<LedgerEntry> findEntries(long accountId) {
        return financeMapper.findEntries(accountId);
    }

    @Override
    public SettlementBatch saveSettlementBatch(SettlementBatch batch) {
        financeMapper.saveSettlementBatch(batch);
        return batch;
    }

    @Override
    public Optional<SettlementBatch> findSettlementBatch(long batchId) {
        return Optional.ofNullable(financeMapper.findSettlementBatch(batchId));
    }

    @Override
    public List<SettlementBatch> findSettlementBatches(long merchantId) {
        return financeMapper.findSettlementBatches(merchantId);
    }

    @Override
    public InvoiceDocument saveInvoice(InvoiceDocument invoice) {
        financeMapper.saveInvoice(invoice);
        return invoice;
    }

    @Override
    public Optional<InvoiceDocument> findInvoice(long invoiceId) {
        return Optional.ofNullable(financeMapper.findInvoice(invoiceId));
    }

    @Override
    public ClearingFile saveClearingFile(ClearingFile file) {
        financeMapper.saveClearingFile(file);
        return file;
    }

    @Override
    public ChargebackCase saveChargeback(ChargebackCase chargeback) {
        financeMapper.saveChargeback(chargeback);
        return chargeback;
    }

    @Override
    public Optional<ChargebackCase> findChargeback(long chargebackId) {
        return Optional.ofNullable(financeMapper.findChargeback(chargebackId));
    }

    @Override
    public List<ChargebackCase> findChargebacks() {
        return financeMapper.findChargebacks();
    }
}
