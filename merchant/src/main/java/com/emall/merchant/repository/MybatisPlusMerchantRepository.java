package com.emall.merchant.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.merchant.domain.CommissionRule;
import com.emall.merchant.domain.Invoice;
import com.emall.merchant.domain.InvoiceStatus;
import com.emall.merchant.domain.Merchant;
import com.emall.merchant.domain.MerchantStatus;
import com.emall.merchant.domain.Settlement;
import com.emall.merchant.domain.SettlementStatus;
import com.emall.merchant.domain.Store;
import com.emall.merchant.domain.StoreStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusMerchantRepository implements MerchantRepository {
    private final MerchantMapper merchantMapper;
    private final StoreMapper storeMapper;
    private final CommissionRuleMapper commissionRuleMapper;
    private final SettlementMapper settlementMapper;
    private final InvoiceMapper invoiceMapper;

    public MybatisPlusMerchantRepository(MerchantMapper merchantMapper, StoreMapper storeMapper,
            CommissionRuleMapper commissionRuleMapper, SettlementMapper settlementMapper, InvoiceMapper invoiceMapper) {
        this.merchantMapper = merchantMapper;
        this.storeMapper = storeMapper;
        this.commissionRuleMapper = commissionRuleMapper;
        this.settlementMapper = settlementMapper;
        this.invoiceMapper = invoiceMapper;
    }

    @Override
    public Merchant saveMerchant(Merchant merchant) {
        MerchantEntity entity = toEntity(merchant);
        try {
            merchantMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            merchantMapper.update(null, new UpdateWrapper<MerchantEntity>()
                    .set("name", entity.getName())
                    .set("contact_email", entity.getContactEmail())
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("merchant_id", entity.getMerchantId()));
        }
        return merchant;
    }

    @Override
    public Optional<Merchant> findMerchant(long merchantId) {
        return Optional.ofNullable(merchantMapper.selectById(merchantId)).map(this::toDomain);
    }

    @Override
    public Store saveStore(Store store) {
        StoreEntity entity = toEntity(store);
        try {
            storeMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            storeMapper.update(null, new UpdateWrapper<StoreEntity>()
                    .set("name", entity.getName())
                    .set("description", entity.getDescription())
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("store_id", entity.getStoreId()));
        }
        return store;
    }

    @Override
    public Optional<Store> findStore(long storeId) {
        return Optional.ofNullable(storeMapper.selectById(storeId)).map(this::toDomain);
    }

    @Override
    public List<Store> findStoresByMerchant(long merchantId) {
        return storeMapper.selectList(new QueryWrapper<StoreEntity>()
                .eq("merchant_id", merchantId)
                .orderByDesc("created_at")).stream().map(this::toDomain).toList();
    }

    @Override
    public CommissionRule saveCommissionRule(CommissionRule rule) {
        CommissionRuleEntity entity = toEntity(rule);
        commissionRuleMapper.update(null, new UpdateWrapper<CommissionRuleEntity>()
                .set("active", false)
                .set("updated_at", entity.getUpdatedAt())
                .eq("merchant_id", entity.getMerchantId())
                .eq("active", true));
        try {
            commissionRuleMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            commissionRuleMapper.update(null, new UpdateWrapper<CommissionRuleEntity>()
                    .set("rate", entity.getRate())
                    .set("active", entity.getActive())
                    .set("effective_from", entity.getEffectiveFrom())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("rule_id", entity.getRuleId()));
        }
        return rule;
    }

    @Override
    public Optional<CommissionRule> findActiveCommissionRule(long merchantId) {
        return Optional.ofNullable(commissionRuleMapper.selectOne(new QueryWrapper<CommissionRuleEntity>()
                .eq("merchant_id", merchantId)
                .eq("active", true)
                .orderByDesc("effective_from")
                .last("LIMIT 1"))).map(this::toDomain);
    }

    @Override
    public Settlement saveSettlement(Settlement settlement) {
        SettlementEntity entity = toEntity(settlement);
        try {
            settlementMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            settlementMapper.update(null, new UpdateWrapper<SettlementEntity>()
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("settlement_id", entity.getSettlementId()));
        }
        return settlement;
    }

    @Override
    public Optional<Settlement> findSettlement(long settlementId) {
        return Optional.ofNullable(settlementMapper.selectById(settlementId)).map(this::toDomain);
    }

    @Override
    public List<Settlement> findSettlementsByMerchant(long merchantId) {
        return settlementMapper.selectList(new QueryWrapper<SettlementEntity>()
                .eq("merchant_id", merchantId)
                .orderByDesc("created_at")).stream().map(this::toDomain).toList();
    }

    @Override
    public Invoice saveInvoice(Invoice invoice) {
        InvoiceEntity entity = toEntity(invoice);
        try {
            invoiceMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            invoiceMapper.update(null, new UpdateWrapper<InvoiceEntity>()
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("invoice_id", entity.getInvoiceId()));
        }
        return invoice;
    }

    @Override
    public List<Invoice> findInvoicesByMerchant(long merchantId) {
        return invoiceMapper.selectList(new QueryWrapper<InvoiceEntity>()
                .eq("merchant_id", merchantId)
                .orderByDesc("created_at")).stream().map(this::toDomain).toList();
    }

    private MerchantEntity toEntity(Merchant merchant) {
        MerchantEntity entity = new MerchantEntity();
        entity.setMerchantId(merchant.merchantId());
        entity.setName(merchant.name());
        entity.setContactEmail(merchant.contactEmail());
        entity.setStatus(merchant.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(merchant.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(merchant.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private Merchant toDomain(MerchantEntity entity) {
        return new Merchant(entity.getMerchantId(), entity.getName(), entity.getContactEmail(),
                MerchantStatus.valueOf(entity.getStatus()), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private StoreEntity toEntity(Store store) {
        StoreEntity entity = new StoreEntity();
        entity.setStoreId(store.storeId());
        entity.setMerchantId(store.merchantId());
        entity.setName(store.name());
        entity.setDescription(store.description());
        entity.setStatus(store.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(store.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(store.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private Store toDomain(StoreEntity entity) {
        return new Store(entity.getStoreId(), entity.getMerchantId(), entity.getName(), entity.getDescription(),
                StoreStatus.valueOf(entity.getStatus()), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private CommissionRuleEntity toEntity(CommissionRule rule) {
        CommissionRuleEntity entity = new CommissionRuleEntity();
        entity.setRuleId(rule.ruleId());
        entity.setMerchantId(rule.merchantId());
        entity.setRate(rule.rate());
        entity.setActive(rule.active());
        entity.setEffectiveFrom(LocalDateTime.ofInstant(rule.effectiveFrom(), ZoneOffset.UTC));
        entity.setCreatedAt(LocalDateTime.ofInstant(rule.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(rule.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private CommissionRule toDomain(CommissionRuleEntity entity) {
        return new CommissionRule(entity.getRuleId(), entity.getMerchantId(), entity.getRate(), entity.getActive(),
                entity.getEffectiveFrom().toInstant(ZoneOffset.UTC), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private SettlementEntity toEntity(Settlement settlement) {
        SettlementEntity entity = new SettlementEntity();
        entity.setSettlementId(settlement.settlementId());
        entity.setMerchantId(settlement.merchantId());
        entity.setGrossAmount(settlement.grossAmount());
        entity.setCommissionAmount(settlement.commissionAmount());
        entity.setNetAmount(settlement.netAmount());
        entity.setStatus(settlement.status().name());
        entity.setPeriodStart(LocalDateTime.ofInstant(settlement.periodStart(), ZoneOffset.UTC));
        entity.setPeriodEnd(LocalDateTime.ofInstant(settlement.periodEnd(), ZoneOffset.UTC));
        entity.setCreatedAt(LocalDateTime.ofInstant(settlement.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(settlement.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private Settlement toDomain(SettlementEntity entity) {
        return new Settlement(entity.getSettlementId(), entity.getMerchantId(), entity.getGrossAmount(),
                entity.getCommissionAmount(), entity.getNetAmount(), SettlementStatus.valueOf(entity.getStatus()),
                entity.getPeriodStart().toInstant(ZoneOffset.UTC), entity.getPeriodEnd().toInstant(ZoneOffset.UTC),
                entity.getCreatedAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private InvoiceEntity toEntity(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setInvoiceId(invoice.invoiceId());
        entity.setSettlementId(invoice.settlementId());
        entity.setMerchantId(invoice.merchantId());
        entity.setAmount(invoice.amount());
        entity.setStatus(invoice.status().name());
        entity.setInvoiceTitle(invoice.invoiceTitle());
        entity.setCreatedAt(LocalDateTime.ofInstant(invoice.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(invoice.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private Invoice toDomain(InvoiceEntity entity) {
        return new Invoice(entity.getInvoiceId(), entity.getSettlementId(), entity.getMerchantId(), entity.getAmount(),
                InvoiceStatus.valueOf(entity.getStatus()), entity.getInvoiceTitle(),
                entity.getCreatedAt().toInstant(ZoneOffset.UTC), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
