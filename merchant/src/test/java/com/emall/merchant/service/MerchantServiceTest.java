package com.emall.merchant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.merchant.domain.Invoice;
import com.emall.merchant.domain.Merchant;
import com.emall.merchant.domain.MerchantStatus;
import com.emall.merchant.domain.Settlement;
import com.emall.merchant.domain.Store;
import com.emall.merchant.domain.StoreStatus;
import com.emall.merchant.repository.InMemoryMerchantRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MerchantServiceTest {

    @Test
    void createsStoreSettlementAndInvoiceForMerchant() {
        MerchantService merchantService =
                new MerchantService(new InMemoryMerchantRepository(), new SnowflakeIdGenerator(1L));
        Merchant merchant = merchantService.registerMerchant("market seller", "seller@example.com");
        merchantService.changeMerchantStatus(merchant.merchantId(), MerchantStatus.ACTIVE);
        Store store = merchantService.createStore(merchant.merchantId(), "seller store", "default marketplace store");
        merchantService.changeStoreStatus(store.storeId(), StoreStatus.ACTIVE);
        merchantService.createCommissionRule(merchant.merchantId(), new BigDecimal("0.100000"), Instant.now());

        Settlement settlement = merchantService.createSettlement(merchant.merchantId(), new BigDecimal("1000.00"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"));
        Settlement paid = merchantService.paySettlement(settlement.settlementId());
        Invoice invoice = merchantService.issueInvoice(paid.settlementId(), "seller invoice");

        assertThat(settlement.commissionAmount()).isEqualByComparingTo("100.00");
        assertThat(settlement.netAmount()).isEqualByComparingTo("900.00");
        assertThat(invoice.amount()).isEqualByComparingTo("900.00");
        assertThat(merchantService.findStores(merchant.merchantId())).hasSize(1);
        assertThat(merchantService.findInvoices(merchant.merchantId())).hasSize(1);
    }
}
