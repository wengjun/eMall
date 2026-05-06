CREATE TABLE inventory_item (
    sku_id BIGINT NOT NULL,
    total INT NOT NULL,
    reserved INT NOT NULL,
    sold INT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_bucket (
    sku_id BIGINT NOT NULL,
    bucket_no INT NOT NULL,
    total INT NOT NULL,
    reserved INT NOT NULL,
    sold INT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sku_id, bucket_no),
    KEY idx_inventory_bucket_available (sku_id, reserved, bucket_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inventory_reservation (
    request_id VARCHAR(128) NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    bucket_no INT,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (request_id),
    KEY idx_reservation_status_expire (status, expires_at),
    KEY idx_reservation_sku_status (sku_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_event (
    event_id VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL,
    next_retry_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_outbox_publishable (status, next_retry_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE internal_operation_audit (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    service_name VARCHAR(64) NOT NULL,
    operation VARCHAR(128) NOT NULL,
    operator VARCHAR(128) NOT NULL,
    trace_id VARCHAR(128),
    affected INT NOT NULL,
    success BOOLEAN NOT NULL,
    message VARCHAR(512) NOT NULL,
    executed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (audit_id),
    KEY idx_internal_operation_time (operation, executed_at),
    KEY idx_internal_operator_time (operator, executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO inventory_item (sku_id, total, reserved, sold, updated_at)
VALUES
    (10001, 1000000, 0, 0, UTC_TIMESTAMP(6)),
    (10002, 1000000, 0, 0, UTC_TIMESTAMP(6));
