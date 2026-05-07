package com.emall.catalog;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CatalogService {
    private final CatalogRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    CatalogService(CatalogRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    CategoryNode createCategory(long parentId, String categoryCode, String name, boolean leaf) {
        return repository
                .saveCategory(new CategoryNode(idGenerator.nextId(), parentId, normalize(categoryCode), name, leaf));
    }

    @Transactional
    AttributeTemplate upsertTemplate(long categoryId, String requiredAttributes, String optionalAttributes) {
        requireCategory(categoryId);
        return repository.saveTemplate(
                new AttributeTemplate(idGenerator.nextId(), categoryId, requiredAttributes, optionalAttributes));
    }

    @Transactional
    BrandAuthorization authorizeBrand(long merchantId, String brandCode) {
        return repository.saveBrandAuthorization(
                new BrandAuthorization(idGenerator.nextId(), merchantId, normalize(brandCode), true, Instant.now()));
    }

    @Transactional
    Spu createSpu(long merchantId, String title, long categoryId, String brandCode) {
        requireCategory(categoryId);
        if (!repository.hasBrandAuthorization(merchantId, normalize(brandCode))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "merchant is not authorized for brand");
        }
        Instant now = Instant.now();
        return repository.saveSpu(new Spu(idGenerator.nextId(), merchantId, title, categoryId, normalize(brandCode),
                ListingStatus.DRAFT, 0, now, now));
    }

    @Transactional
    Sku createSku(long spuId, String skuCode, BigDecimal price, String attributes) {
        requireSpu(spuId);
        if (price.signum() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sku price must be positive");
        }
        Instant now = Instant.now();
        return repository
                .saveSku(new Sku(idGenerator.nextId(), spuId, normalize(skuCode), price, attributes, false, now, now));
    }

    @Transactional
    ListingReview reviewListing(long spuId, boolean approved, int qualityScore, String reason) {
        Spu spu = requireSpu(spuId);
        List<Sku> skus = repository.findSkus(spuId);
        if (approved && skus.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "approved listing must have at least one sku");
        }
        ListingStatus nextStatus = approved ? ListingStatus.APPROVED : ListingStatus.REJECTED;
        if (!approved) {
            repository.saveViolation(
                    new ListingViolation(idGenerator.nextId(), spuId, "listing-review", reason, Instant.now()));
        }
        repository.saveSpu(spu.changeStatus(nextStatus, Math.max(0, Math.min(qualityScore, 100))));
        return new ListingReview(spuId, nextStatus, Math.max(0, Math.min(qualityScore, 100)), reason);
    }

    @Transactional
    Spu publish(long spuId) {
        Spu spu = requireSpu(spuId);
        if (spu.status() != ListingStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "listing must be approved before publishing");
        }
        return repository.saveSpu(spu.changeStatus(ListingStatus.PUBLISHED, spu.qualityScore()));
    }

    Spu getSpu(long spuId) {
        return requireSpu(spuId);
    }

    List<Sku> findSkus(long spuId) {
        requireSpu(spuId);
        return repository.findSkus(spuId);
    }

    List<ListingViolation> findViolations(long spuId) {
        requireSpu(spuId);
        return repository.findViolations(spuId);
    }

    private CategoryNode requireCategory(long categoryId) {
        return repository.findCategory(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "catalog category not found"));
    }

    private Spu requireSpu(long spuId) {
        return repository.findSpu(spuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "catalog spu not found"));
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "catalog value must not be blank");
        }
        return normalized;
    }
}
