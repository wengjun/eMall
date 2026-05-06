ALTER TABLE fulfillment_order
    ADD COLUMN destination_region_code VARCHAR(64),
    ADD COLUMN planned_carrier VARCHAR(64),
    ADD COLUMN estimated_sla_hours INT NOT NULL DEFAULT 0,
    ADD KEY idx_fulfillment_destination_region (destination_region_code),
    ADD KEY idx_fulfillment_warehouse_status (warehouse_code, status);

CREATE TABLE fulfillment_warehouse (
    warehouse_code VARCHAR(64) NOT NULL,
    region_code VARCHAR(64) NOT NULL,
    priority INT NOT NULL,
    daily_capacity INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (warehouse_code),
    KEY idx_fulfillment_warehouse_region_enabled (region_code, enabled, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fulfillment_carrier_route (
    route_id BIGINT NOT NULL,
    carrier_code VARCHAR(64) NOT NULL,
    origin_warehouse_code VARCHAR(64) NOT NULL,
    destination_region_code VARCHAR(64) NOT NULL,
    priority INT NOT NULL,
    base_cost DECIMAL(18, 2) NOT NULL,
    sla_hours INT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (route_id),
    KEY idx_fulfillment_route_lookup (origin_warehouse_code, destination_region_code, active, priority),
    KEY idx_fulfillment_route_carrier (carrier_code, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fulfillment_tracking_event (
    event_id BIGINT NOT NULL,
    fulfillment_id BIGINT NOT NULL,
    carrier_code VARCHAR(64) NOT NULL,
    tracking_no VARCHAR(128) NOT NULL,
    event_code VARCHAR(64) NOT NULL,
    event_time TIMESTAMP(6) NOT NULL,
    location VARCHAR(256),
    description VARCHAR(512),
    received_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_tracking_event_fulfillment_time (fulfillment_id, event_time),
    KEY idx_tracking_event_tracking_no (carrier_code, tracking_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
