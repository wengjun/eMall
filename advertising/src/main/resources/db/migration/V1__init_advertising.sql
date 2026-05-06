CREATE TABLE advertising_campaign (
    campaign_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    daily_budget DECIMAL(18, 2) NOT NULL,
    used_budget DECIMAL(18, 2) NOT NULL,
    bid_amount DECIMAL(18, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (campaign_id),
    KEY idx_ad_campaign_status_time (status, starts_at, ends_at),
    KEY idx_ad_campaign_merchant (merchant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE advertising_creative (
    creative_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    target_url VARCHAR(512) NOT NULL,
    active BOOLEAN NOT NULL,
    PRIMARY KEY (creative_id),
    KEY idx_ad_creative_campaign (campaign_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE advertising_keyword_target (
    target_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    keyword VARCHAR(128) NOT NULL,
    bid_multiplier DECIMAL(18, 6) NOT NULL,
    active BOOLEAN NOT NULL,
    PRIMARY KEY (target_id),
    KEY idx_ad_target_keyword (keyword, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE advertising_event (
    event_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    creative_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    cost DECIMAL(18, 2) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_ad_event_campaign_time (campaign_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
