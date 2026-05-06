CREATE TABLE promotion_campaign (
    campaign_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    promotion_type VARCHAR(32) NOT NULL,
    threshold_amount DECIMAL(18, 2) NOT NULL,
    benefit_value DECIMAL(18, 2) NOT NULL,
    budget_amount DECIMAL(18, 2) NOT NULL,
    used_budget DECIMAL(18, 2) NOT NULL,
    priority INT NOT NULL,
    stackable BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (campaign_id),
    KEY idx_promotion_campaign_status_time (status, starts_at, ends_at),
    KEY idx_promotion_campaign_priority (status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
