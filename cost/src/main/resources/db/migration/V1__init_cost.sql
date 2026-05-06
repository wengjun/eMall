CREATE TABLE cost_signal (
    signal_id BIGINT NOT NULL,
    service_name VARCHAR(64) NOT NULL,
    signal_type VARCHAR(64) NOT NULL,
    metric_value DECIMAL(18, 6) NOT NULL,
    threshold_value DECIMAL(18, 6) NOT NULL,
    observed_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (signal_id),
    KEY idx_cost_signal_service_time (service_name, observed_at),
    KEY idx_cost_signal_type_time (signal_type, observed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cost_budget (
    budget_id BIGINT NOT NULL,
    service_name VARCHAR(64) NOT NULL,
    monthly_budget DECIMAL(18, 6) NOT NULL,
    current_spend DECIMAL(18, 6) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    alert_threshold_percent INT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (budget_id),
    UNIQUE KEY uk_cost_budget_service (service_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cost_optimization_action (
    action_id BIGINT NOT NULL,
    service_name VARCHAR(64) NOT NULL,
    signal_type VARCHAR(64) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority INT NOT NULL,
    description VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (action_id),
    KEY idx_cost_action_service_status (service_name, status, priority),
    KEY idx_cost_action_dedup (service_name, signal_type, action_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
