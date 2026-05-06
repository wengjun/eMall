package com.emall.catalog;

import java.math.BigDecimal;
import java.time.Instant;

enum ListingStatus {
    DRAFT,
    REVIEWING,
    APPROVED,
    REJECTED,
    PUBLISHED,
    OFFLINE
}

record CategoryNode(long categoryId, long parentId, String categoryCode, String name, boolean leaf) {
}

record AttributeTemplate(long templateId, long categoryId, String requiredAttributes, String optionalAttributes) {
}

record BrandAuthorization(long authorizationId, long merchantId, String brandCode, boolean active, Instant createdAt) {
}

record Spu(long spuId, long merchantId, String title, long categoryId, String brandCode, ListingStatus status,
           int qualityScore, Instant createdAt, Instant updatedAt) {
    Spu changeStatus(ListingStatus nextStatus, int nextQualityScore) {
        return new Spu(spuId, merchantId, title, categoryId, brandCode, nextStatus, nextQualityScore, createdAt,
                Instant.now());
    }
}

record Sku(long skuId, long spuId, String skuCode, BigDecimal price, String attributes, boolean saleable,
           Instant createdAt, Instant updatedAt) {
}

record ListingViolation(long violationId, long spuId, String violationType, String reason, Instant createdAt) {
}

record ListingReview(long spuId, ListingStatus status, int qualityScore, String reason) {
}
