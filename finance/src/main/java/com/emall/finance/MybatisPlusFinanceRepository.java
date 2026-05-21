package com.emall.finance;

import java.util.List;
import java.util.Optional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusFinanceRepository implements FinanceRepository {
    private final FinanceMapper financeMapper;
    private final FinanceAccountMapper accountMapper;
    private final LedgerEntryMapper entryMapper;
    private final SettlementBatchMapper settlementBatchMapper;
    private final InvoiceDocumentMapper invoiceMapper;
    private final ClearingFileMapper clearingFileMapper;
    private final ChargebackCaseMapper chargebackMapper;

    MybatisPlusFinanceRepository(FinanceMapper financeMapper, FinanceAccountMapper accountMapper,
            LedgerEntryMapper entryMapper, SettlementBatchMapper settlementBatchMapper,
            InvoiceDocumentMapper invoiceMapper, ClearingFileMapper clearingFileMapper,
            ChargebackCaseMapper chargebackMapper) {
        this.financeMapper = financeMapper;
        this.accountMapper = accountMapper;
        this.entryMapper = entryMapper;
        this.settlementBatchMapper = settlementBatchMapper;
        this.invoiceMapper = invoiceMapper;
        this.clearingFileMapper = clearingFileMapper;
        this.chargebackMapper = chargebackMapper;
    }

    @Override
    public FinanceAccount saveAccount(FinanceAccount account) {
        financeMapper.saveAccount(account);
        return account;
    }

    @Override
    public Optional<FinanceAccount> findAccount(long accountId) {
        return Optional.ofNullable(accountMapper.selectById(accountId));
    }

    @Override
    public List<FinanceAccount> findAccounts() {
        return accountMapper.selectList(null);
    }

    @Override
    public LedgerEntry saveEntry(LedgerEntry entry) {
        entryMapper.insert(entry);
        return entry;
    }

    @Override
    public List<LedgerEntry> findEntries(long accountId) {
        return entryMapper.selectList(new QueryWrapper<LedgerEntry>().eq("account_id", accountId));
    }

    @Override
    public SettlementBatch saveSettlementBatch(SettlementBatch batch) {
        financeMapper.saveSettlementBatch(batch);
        return batch;
    }

    @Override
    public Optional<SettlementBatch> findSettlementBatch(long batchId) {
        return Optional.ofNullable(settlementBatchMapper.selectById(batchId));
    }

    @Override
    public List<SettlementBatch> findSettlementBatches(long merchantId) {
        return settlementBatchMapper.selectList(new QueryWrapper<SettlementBatch>().eq("merchant_id", merchantId));
    }

    @Override
    public InvoiceDocument saveInvoice(InvoiceDocument invoice) {
        financeMapper.saveInvoice(invoice);
        return invoice;
    }

    @Override
    public Optional<InvoiceDocument> findInvoice(long invoiceId) {
        return Optional.ofNullable(invoiceMapper.selectById(invoiceId));
    }

    @Override
    public ClearingFile saveClearingFile(ClearingFile file) {
        clearingFileMapper.insert(file);
        return file;
    }

    @Override
    public ChargebackCase saveChargeback(ChargebackCase chargeback) {
        financeMapper.saveChargeback(chargeback);
        return chargeback;
    }

    @Override
    public Optional<ChargebackCase> findChargeback(long chargebackId) {
        return Optional.ofNullable(chargebackMapper.selectById(chargebackId));
    }

    @Override
    public List<ChargebackCase> findChargebacks() {
        return chargebackMapper.selectList(null);
    }
}
