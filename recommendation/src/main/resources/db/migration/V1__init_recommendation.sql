CREATE TABLE recommendation_user_preference (
    user_id BIGINT NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    affinity_score INT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id, category_code),
    KEY idx_recommendation_preference_user_score (user_id, affinity_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE recommendation_item_feature (
    sku_id BIGINT NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    base_score DECIMAL(18, 6) NOT NULL,
    popularity_score DECIMAL(18, 6) NOT NULL,
    active BOOLEAN NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sku_id),
    KEY idx_recommendation_item_category_active (category_code, active, popularity_score),
    KEY idx_recommendation_item_active_popularity (active, popularity_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE recommendation_behavior_event (
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    category_code VARCHAR(64) NOT NULL,
    behavior_type VARCHAR(32) NOT NULL,
    weight INT NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_recommendation_behavior_user_time (user_id, occurred_at),
    KEY idx_recommendation_behavior_sku_time (sku_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE recommendation_experiment (
    experiment_id BIGINT NOT NULL,
    scene VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    traffic_percent INT NOT NULL,
    control_strategy VARCHAR(64) NOT NULL,
    treatment_strategy VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (experiment_id),
    KEY idx_recommendation_experiment_scene_status (scene, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
