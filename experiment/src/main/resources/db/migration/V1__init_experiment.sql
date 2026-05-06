CREATE TABLE experiment_definition (
    experiment_id BIGINT NOT NULL,
    scene VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    mutual_exclusion_group VARCHAR(64) NOT NULL,
    traffic_percent INT NOT NULL,
    control_variant VARCHAR(64) NOT NULL,
    treatment_variant VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (experiment_id),
    KEY idx_experiment_scene_status (scene, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE experiment_guardrail (
    metric_id BIGINT NOT NULL,
    experiment_id BIGINT NOT NULL,
    metric_name VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    threshold_value DECIMAL(18, 6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (metric_id),
    KEY idx_experiment_guardrail_experiment (experiment_id, metric_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE experiment_metric (
    metric_record_id BIGINT NOT NULL,
    experiment_id BIGINT NOT NULL,
    variant VARCHAR(64) NOT NULL,
    metric_name VARCHAR(64) NOT NULL,
    metric_value DECIMAL(18, 6) NOT NULL,
    recorded_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (metric_record_id),
    KEY idx_experiment_metric_experiment (experiment_id, metric_name, variant)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
