CREATE TABLE event_field_classification (
    classification_id BIGINT PRIMARY KEY,
    event_name VARCHAR(128) NOT NULL,
    version INT NOT NULL,
    field_name VARCHAR(128) NOT NULL,
    sensitivity VARCHAR(64) NOT NULL,
    required TINYINT(1) NOT NULL,
    exported_to_warehouse TINYINT(1) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_event_field_classification (event_name, version, field_name),
    KEY idx_event_field_sensitivity (sensitivity)
);
