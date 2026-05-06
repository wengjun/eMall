CREATE TABLE merchant (
    merchant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    contact_email VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (merchant_id),
    KEY idx_merchant_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant_store (
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (store_id),
    KEY idx_store_merchant_status (merchant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant_commission_rule (
    rule_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    rate DECIMAL(8, 6) NOT NULL,
    active BOOLEAN NOT NULL,
    effective_from TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (rule_id),
    KEY idx_commission_merchant_active (merchant_id, active, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant_settlement (
    settlement_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    gross_amount DECIMAL(18, 2) NOT NULL,
    commission_amount DECIMAL(18, 2) NOT NULL,
    net_amount DECIMAL(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    period_start TIMESTAMP(6) NOT NULL,
    period_end TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (settlement_id),
    KEY idx_settlement_merchant_period (merchant_id, period_start, period_end),
    KEY idx_settlement_status_updated (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant_invoice (
    invoice_id BIGINT NOT NULL,
    settlement_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    invoice_title VARCHAR(256) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (invoice_id),
    KEY idx_invoice_merchant_status (merchant_id, status),
    KEY idx_invoice_settlement (settlement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
