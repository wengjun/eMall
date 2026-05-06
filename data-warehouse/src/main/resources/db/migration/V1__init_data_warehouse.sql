CREATE TABLE dataset_definition (
    dataset_id BIGINT PRIMARY KEY,
    layer VARCHAR(32) NOT NULL,
    dataset_name VARCHAR(128) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    description VARCHAR(512) NOT NULL,
    retention_days INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_dataset_definition_name (dataset_name)
);

CREATE TABLE table_partition (
    partition_id BIGINT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    partition_key VARCHAR(128) NOT NULL,
    partition_date DATE NOT NULL,
    row_count BIGINT NOT NULL,
    storage_bytes BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_table_partition_dataset (dataset_id, partition_date)
);

CREATE TABLE quality_check (
    check_id BIGINT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    check_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    detail VARCHAR(512) NOT NULL,
    checked_at TIMESTAMP(6) NOT NULL,
    KEY idx_quality_check_dataset (dataset_id),
    KEY idx_quality_check_status (status)
);

CREATE TABLE lineage_edge (
    lineage_id BIGINT PRIMARY KEY,
    upstream_dataset_id BIGINT NOT NULL,
    downstream_dataset_id BIGINT NOT NULL,
    transform_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_lineage_upstream (upstream_dataset_id),
    KEY idx_lineage_downstream (downstream_dataset_id)
);
