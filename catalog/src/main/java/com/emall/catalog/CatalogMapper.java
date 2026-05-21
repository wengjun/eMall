package com.emall.catalog;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface CatalogMapper {
    @Insert("""
            INSERT INTO catalog_category (category_id, parent_id, category_code, name, leaf)
            VALUES (#{category.categoryId}, #{category.parentId}, #{category.categoryCode}, #{category.name},
                #{category.leaf})
            ON DUPLICATE KEY UPDATE parent_id = VALUES(parent_id), name = VALUES(name), leaf = VALUES(leaf)
            """)
    int saveCategory(@Param("category") CategoryNode category);

    @Insert("""
            INSERT INTO catalog_attribute_template
                (template_id, category_id, required_attributes, optional_attributes)
            VALUES (#{template.templateId}, #{template.categoryId}, #{template.requiredAttributes},
                #{template.optionalAttributes})
            ON DUPLICATE KEY UPDATE required_attributes = VALUES(required_attributes),
                optional_attributes = VALUES(optional_attributes)
            """)
    int saveTemplate(@Param("template") AttributeTemplate template);

    @Insert("""
            INSERT INTO catalog_brand_authorization
                (authorization_id, merchant_id, brand_code, active, created_at)
            VALUES (#{authorization.authorizationId}, #{authorization.merchantId}, #{authorization.brandCode},
                #{authorization.active}, #{authorization.createdAt})
            ON DUPLICATE KEY UPDATE active = VALUES(active)
            """)
    int saveBrandAuthorization(@Param("authorization") BrandAuthorization authorization);

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

    @Insert("""
            INSERT INTO catalog_sku (sku_id, spu_id, sku_code, price, attributes, saleable, created_at, updated_at)
            VALUES (#{sku.skuId}, #{sku.spuId}, #{sku.skuCode}, #{sku.price}, #{sku.attributes},
                #{sku.saleable}, #{sku.createdAt}, #{sku.updatedAt})
            ON DUPLICATE KEY UPDATE price = VALUES(price), attributes = VALUES(attributes),
                saleable = VALUES(saleable), updated_at = VALUES(updated_at)
            """)
    int saveSku(@Param("sku") Sku sku);
}
