CREATE TABLE quality_alert (
    alert_id BIGINT PRIMARY KEY,
    dataset_id BIGINT NOT NULL,
    check_id BIGINT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    detail VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_quality_alert_dataset_status (dataset_id, status, created_at)
);

CREATE TABLE field_lineage (
    lineage_id BIGINT PRIMARY KEY,
    upstream_dataset_id BIGINT NOT NULL,
    upstream_field VARCHAR(128) NOT NULL,
    downstream_dataset_id BIGINT NOT NULL,
    downstream_field VARCHAR(128) NOT NULL,
    sensitivity VARCHAR(64) NOT NULL,
    transform_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_field_lineage_downstream (downstream_dataset_id, downstream_field),
    KEY idx_field_lineage_sensitivity (sensitivity)
);
