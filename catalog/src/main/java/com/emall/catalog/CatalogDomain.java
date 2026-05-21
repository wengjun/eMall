package com.emall.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

enum ListingStatus {
    DRAFT,
    REVIEWING,
    APPROVED,
    REJECTED,
    PUBLISHED,
    OFFLINE
}

@TableName("catalog_category")
record CategoryNode(@TableId(value = "category_id", type = IdType.INPUT) long categoryId, long parentId,
        String categoryCode, String name, boolean leaf) {
}

@TableName("catalog_attribute_template")
record AttributeTemplate(@TableId(value = "template_id", type = IdType.INPUT) long templateId, long categoryId,
        String requiredAttributes, String optionalAttributes) {
}

@TableName("catalog_brand_authorization")
record BrandAuthorization(@TableId(value = "authorization_id", type = IdType.INPUT) long authorizationId,
        long merchantId, String brandCode, boolean active, Instant createdAt) {
}

@TableName("catalog_spu")
record Spu(@TableId(value = "spu_id", type = IdType.INPUT) long spuId, long merchantId, String title,
        long categoryId, String brandCode, ListingStatus status, int qualityScore, Instant createdAt,
        Instant updatedAt) {
    Spu changeStatus(ListingStatus nextStatus, int nextQualityScore) {
        return new Spu(spuId, merchantId, title, categoryId, brandCode, nextStatus, nextQualityScore, createdAt,
                Instant.now());
    }
}

@TableName("catalog_sku")
record Sku(@TableId(value = "sku_id", type = IdType.INPUT) long skuId, long spuId, String skuCode, BigDecimal price,
        String attributes, boolean saleable, Instant createdAt, Instant updatedAt) {
}

@TableName("catalog_listing_violation")
record ListingViolation(@TableId(value = "violation_id", type = IdType.INPUT) long violationId, long spuId,
        String violationType, String reason, Instant createdAt) {
}

record ListingReview(long spuId, ListingStatus status, int qualityScore, String reason) {
}
