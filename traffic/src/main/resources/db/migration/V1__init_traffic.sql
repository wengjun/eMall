CREATE TABLE unit_cell (
    unit_id BIGINT PRIMARY KEY,
    unit_code VARCHAR(64) NOT NULL,
    region_code VARCHAR(64) NOT NULL,
    capacity_weight INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_unit_cell_code (unit_code)
);

CREATE TABLE shard_route (
    route_id BIGINT PRIMARY KEY,
    domain_name VARCHAR(64) NOT NULL,
    shard_no INT NOT NULL,
    unit_code VARCHAR(64) NOT NULL,
    database_key VARCHAR(128) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_shard_route_domain_shard (domain_name, shard_no)
);

CREATE TABLE traffic_shift (
    shift_id BIGINT PRIMARY KEY,
    source_unit VARCHAR(64) NOT NULL,
    target_unit VARCHAR(64) NOT NULL,
    percent INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_traffic_shift_status (status)
);
