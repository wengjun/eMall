package com.emall.catalog;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface CatalogMapper {
    @Insert("""
            INSERT INTO catalog_category (category_id, parent_id, category_code, name, leaf)
            VALUES (#{category.categoryId}, #{category.parentId}, #{category.categoryCode}, #{category.name},
                #{category.leaf})
            ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), leaf = VALUES(leaf)
            """)
    int saveCategory(@Param("category") CategoryNode category);

    @Select("""
            SELECT category_id, parent_id, category_code, name, leaf
            FROM catalog_category
            WHERE category_id = #{categoryId}
            """)
    CategoryNode findCategory(@Param("categoryId") long categoryId);

    @Insert("""
            INSERT INTO catalog_attribute_template
                (template_id, category_id, required_attributes, optional_attributes)
            VALUES (#{template.templateId}, #{template.categoryId}, #{template.requiredAttributes},
                #{template.optionalAttributes})
            ON DUPLICATE KEY UPDATE required_attributes = VALUES(required_attributes),
                optional_attributes = VALUES(optional_attributes)
            """)
    int saveTemplate(@Param("template") AttributeTemplate template);

    @Select("""
            SELECT template_id, category_id, required_attributes, optional_attributes
            FROM catalog_attribute_template
            WHERE category_id = #{categoryId}
            """)
    AttributeTemplate findTemplate(@Param("categoryId") long categoryId);

    @Insert("""
            INSERT INTO catalog_brand_authorization
                (authorization_id, merchant_id, brand_code, active, created_at)
            VALUES (#{authorization.authorizationId}, #{authorization.merchantId}, #{authorization.brandCode},
                #{authorization.active}, #{authorization.createdAt})
            ON DUPLICATE KEY UPDATE active = VALUES(active)
            """)
    int saveBrandAuthorization(@Param("authorization") BrandAuthorization authorization);

    @Select("""
            SELECT COUNT(*) FROM catalog_brand_authorization
            WHERE merchant_id = #{merchantId} AND brand_code = #{brandCode} AND active = TRUE
            """)
    long countBrandAuthorization(@Param("merchantId") long merchantId, @Param("brandCode") String brandCode);

    @Insert("""
            INSERT INTO catalog_spu
                (spu_id, merchant_id, title, category_id, brand_code, status, quality_score,
                created_at, updated_at)
            VALUES (#{spu.spuId}, #{spu.merchantId}, #{spu.title}, #{spu.categoryId}, #{spu.brandCode},
                #{spu.status}, #{spu.qualityScore}, #{spu.createdAt}, #{spu.updatedAt})
            ON DUPLICATE KEY UPDATE title = VALUES(title), status = VALUES(status),
                quality_score = VALUES(quality_score), updated_at = VALUES(updated_at)
            """)
    int saveSpu(@Param("spu") Spu spu);

    @Select("""
            SELECT spu_id, merchant_id, title, category_id, brand_code, status, quality_score, created_at, updated_at
            FROM catalog_spu
            WHERE spu_id = #{spuId}
            """)
    Spu findSpu(@Param("spuId") long spuId);

    @Insert("""
            INSERT INTO catalog_sku (sku_id, spu_id, sku_code, price, attributes, saleable, created_at, updated_at)
            VALUES (#{sku.skuId}, #{sku.spuId}, #{sku.skuCode}, #{sku.price}, #{sku.attributes},
                #{sku.saleable}, #{sku.createdAt}, #{sku.updatedAt})
            ON DUPLICATE KEY UPDATE price = VALUES(price), attributes = VALUES(attributes),
                saleable = VALUES(saleable), updated_at = VALUES(updated_at)
            """)
    int saveSku(@Param("sku") Sku sku);

    @Select("""
            SELECT sku_id, spu_id, sku_code, price, attributes, saleable, created_at, updated_at
            FROM catalog_sku
            WHERE spu_id = #{spuId}
            """)
    List<Sku> findSkus(@Param("spuId") long spuId);

    @Insert("""
            INSERT INTO catalog_listing_violation (violation_id, spu_id, violation_type, reason, created_at)
            VALUES (#{violation.violationId}, #{violation.spuId}, #{violation.violationType},
                #{violation.reason}, #{violation.createdAt})
            """)
    int saveViolation(@Param("violation") ListingViolation violation);

    @Select("""
            SELECT violation_id, spu_id, violation_type, reason, created_at
            FROM catalog_listing_violation
            WHERE spu_id = #{spuId}
            """)
    List<ListingViolation> findViolations(@Param("spuId") long spuId);
}
