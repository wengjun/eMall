package com.emall.merchant.repository;

import com.emall.merchant.domain.CommissionRule;
import com.emall.merchant.domain.Invoice;
import com.emall.merchant.domain.Merchant;
import com.emall.merchant.domain.Settlement;
import com.emall.merchant.domain.Store;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryMerchantRepository implements MerchantRepository {
    private final ConcurrentMap<Long, Merchant> merchants = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Store> stores = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CommissionRule> commissionRules = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Settlement> settlements = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Invoice> invoices = new ConcurrentHashMap<>();

    @Override
    public Merchant saveMerchant(Merchant merchant) {
        merchants.put(merchant.merchantId(), merchant);
        return merchant;
    }

    @Override
    public Optional<Merchant> findMerchant(long merchantId) {
        return Optional.ofNullable(merchants.get(merchantId));
    }

    @Override
    public Store saveStore(Store store) {
        stores.put(store.storeId(), store);
        return store;
    }

    @Override
    public Optional<Store> findStore(long storeId) {
        return Optional.ofNullable(stores.get(storeId));
    }

    @Override
    public List<Store> findStoresByMerchant(long merchantId) {
        return stores.values().stream().filter(store -> store.merchantId() == merchantId)
                .sorted(Comparator.comparing(Store::createdAt).reversed()).toList();
    }

    @Override
    public CommissionRule saveCommissionRule(CommissionRule rule) {
        commissionRules.put(rule.ruleId(), rule);
        return rule;
    }

    @Override
    public Optional<CommissionRule> findActiveCommissionRule(long merchantId) {
        return commissionRules.values().stream().filter(rule -> rule.merchantId() == merchantId && rule.active())
                .max(Comparator.comparing(CommissionRule::effectiveFrom));
    }

    @Override
    public Settlement saveSettlement(Settlement settlement) {
        settlements.put(settlement.settlementId(), settlement);
        return settlement;
    }

    @Override
    public Optional<Settlement> findSettlement(long settlementId) {
        return Optional.ofNullable(settlements.get(settlementId));
    }

    @Override
    public List<Settlement> findSettlementsByMerchant(long merchantId) {
        return settlements.values().stream().filter(settlement -> settlement.merchantId() == merchantId)
                .sorted(Comparator.comparing(Settlement::createdAt).reversed()).toList();
    }

    @Override
    public Invoice saveInvoice(Invoice invoice) {
        invoices.put(invoice.invoiceId(), invoice);
        return invoice;
    }

    @Override
    public List<Invoice> findInvoicesByMerchant(long merchantId) {
        return invoices.values().stream().filter(invoice -> invoice.merchantId() == merchantId)
                .sorted(Comparator.comparing(Invoice::createdAt).reversed()).toList();
    }
}
