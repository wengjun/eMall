package com.emall.merchant.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.merchant.domain.CommissionRule;
import com.emall.merchant.domain.Invoice;
import com.emall.merchant.domain.InvoiceStatus;
import com.emall.merchant.domain.Merchant;
import com.emall.merchant.domain.MerchantStatus;
import com.emall.merchant.domain.Settlement;
import com.emall.merchant.domain.SettlementStatus;
import com.emall.merchant.domain.Store;
import com.emall.merchant.domain.StoreStatus;
import com.emall.merchant.repository.MerchantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.050000");

    private final MerchantRepository merchantRepository;
    private final SnowflakeIdGenerator idGenerator;

    public MerchantService(MerchantRepository merchantRepository, SnowflakeIdGenerator idGenerator) {
        this.merchantRepository = merchantRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public Merchant registerMerchant(String name, String contactEmail) {
        Instant now = Instant.now();
        return merchantRepository.saveMerchant(
                new Merchant(idGenerator.nextId(), name, contactEmail, MerchantStatus.PENDING_REVIEW, now, now));
    }

    public Merchant getMerchant(long merchantId) {
        return merchantRepository.findMerchant(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "merchant not found"));
    }

    @Transactional
    public Merchant changeMerchantStatus(long merchantId, MerchantStatus status) {
        Merchant merchant = getMerchant(merchantId);
        if (merchant.status() == MerchantStatus.CLOSED && status != MerchantStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONFLICT, "closed merchant cannot be reopened");
        }
        return merchantRepository.saveMerchant(merchant.changeStatus(status));
    }

    @Transactional
    public Store createStore(long merchantId, String name, String description) {
        Merchant merchant = getMerchant(merchantId);
        if (merchant.status() != MerchantStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "merchant must be active before creating stores");
        }
        Instant now = Instant.now();
        return merchantRepository
                .saveStore(new Store(idGenerator.nextId(), merchantId, name, description, StoreStatus.DRAFT, now, now));
    }

    public List<Store> findStores(long merchantId) {
        getMerchant(merchantId);
        return merchantRepository.findStoresByMerchant(merchantId);
    }

    @Transactional
    public Store changeStoreStatus(long storeId, StoreStatus status) {
        Store store = merchantRepository.findStore(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "store not found"));
        return merchantRepository.saveStore(store.changeStatus(status));
    }

    @Transactional
    public CommissionRule createCommissionRule(long merchantId, BigDecimal rate, Instant effectiveFrom) {
        getMerchant(merchantId);
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "commission rate must be between 0 and 1");
        }
        Instant now = Instant.now();
        return merchantRepository.saveCommissionRule(new CommissionRule(idGenerator.nextId(), merchantId,
                rate.setScale(6, RoundingMode.HALF_UP), true, effectiveFrom, now, now));
    }

    @Transactional
    public Settlement createSettlement(long merchantId, BigDecimal grossAmount, Instant periodStart,
            Instant periodEnd) {
        Merchant merchant = getMerchant(merchantId);
        if (merchant.status() == MerchantStatus.CLOSED) {
            throw new BusinessException(ErrorCode.CONFLICT, "closed merchant cannot be settled");
        }
        if (grossAmount.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "gross amount must be positive");
        }
        if (!periodStart.isBefore(periodEnd)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "settlement period is invalid");
        }
        BigDecimal rate = merchantRepository.findActiveCommissionRule(merchantId).map(CommissionRule::rate)
                .orElse(DEFAULT_COMMISSION_RATE);
        BigDecimal commission = grossAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = grossAmount.subtract(commission).setScale(2, RoundingMode.HALF_UP);
        Instant now = Instant.now();
        return merchantRepository.saveSettlement(
                new Settlement(idGenerator.nextId(), merchantId, grossAmount.setScale(2, RoundingMode.HALF_UP),
                        commission, netAmount, SettlementStatus.CREATED, periodStart, periodEnd, now, now));
    }

    public List<Settlement> findSettlements(long merchantId) {
        getMerchant(merchantId);
        return merchantRepository.findSettlementsByMerchant(merchantId);
    }

    @Transactional
    public Settlement paySettlement(long settlementId) {
        Settlement settlement = merchantRepository.findSettlement(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "settlement not found"));
        if (settlement.status() == SettlementStatus.PAID) {
            return settlement;
        }
        if (settlement.status() == SettlementStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.CONFLICT, "cancelled settlement cannot be paid");
        }
        return merchantRepository.saveSettlement(settlement.pay());
    }

    @Transactional
    public Invoice issueInvoice(long settlementId, String invoiceTitle) {
        Settlement settlement = merchantRepository.findSettlement(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "settlement not found"));
        if (settlement.status() != SettlementStatus.PAID) {
            throw new BusinessException(ErrorCode.CONFLICT, "invoice requires a paid settlement");
        }
        Instant now = Instant.now();
        return merchantRepository.saveInvoice(new Invoice(idGenerator.nextId(), settlementId, settlement.merchantId(),
                settlement.netAmount(), InvoiceStatus.ISSUED, invoiceTitle, now, now));
    }

    public List<Invoice> findInvoices(long merchantId) {
        getMerchant(merchantId);
        return merchantRepository.findInvoicesByMerchant(merchantId);
    }
}
