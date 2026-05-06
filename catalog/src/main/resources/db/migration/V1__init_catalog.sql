CREATE TABLE catalog_category (
    category_id BIGINT NOT NULL,
    parent_id BIGINT NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    leaf BOOLEAN NOT NULL,
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_catalog_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE catalog_attribute_template (
    template_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    required_attributes VARCHAR(512) NOT NULL,
    optional_attributes VARCHAR(512) NOT NULL,
    PRIMARY KEY (template_id),
    UNIQUE KEY uk_catalog_template_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE catalog_brand_authorization (
    authorization_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    brand_code VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (authorization_id),
    UNIQUE KEY uk_catalog_brand_auth (merchant_id, brand_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE catalog_spu (
    spu_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    category_id BIGINT NOT NULL,
    brand_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    quality_score INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (spu_id),
    KEY idx_catalog_spu_merchant_status (merchant_id, status),
    KEY idx_catalog_spu_category_status (category_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE catalog_sku (
    sku_id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    sku_code VARCHAR(128) NOT NULL,
    price DECIMAL(18, 2) NOT NULL,
    attributes VARCHAR(512) NOT NULL,
    saleable BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sku_id),
    KEY idx_catalog_sku_spu (spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE catalog_listing_violation (
    violation_id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    violation_type VARCHAR(64) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (violation_id),
    KEY idx_catalog_violation_spu (spu_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
