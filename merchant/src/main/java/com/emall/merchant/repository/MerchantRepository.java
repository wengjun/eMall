package com.emall.merchant.repository;

import com.emall.merchant.domain.CommissionRule;
import com.emall.merchant.domain.Invoice;
import com.emall.merchant.domain.Merchant;
import com.emall.merchant.domain.Settlement;
import com.emall.merchant.domain.Store;
import java.util.List;
import java.util.Optional;

public interface MerchantRepository {
    Merchant saveMerchant(Merchant merchant);

    Optional<Merchant> findMerchant(long merchantId);

    Store saveStore(Store store);

    Optional<Store> findStore(long storeId);

    List<Store> findStoresByMerchant(long merchantId);

    CommissionRule saveCommissionRule(CommissionRule rule);

    Optional<CommissionRule> findActiveCommissionRule(long merchantId);

    Settlement saveSettlement(Settlement settlement);

    Optional<Settlement> findSettlement(long settlementId);

    List<Settlement> findSettlementsByMerchant(long merchantId);

    Invoice saveInvoice(Invoice invoice);

    List<Invoice> findInvoicesByMerchant(long merchantId);
}
