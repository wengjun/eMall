CREATE TABLE warehouse_receipt (
    receipt_id BIGINT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    warehouse_code VARCHAR(64) NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    expires_on DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_warehouse_receipt_warehouse (warehouse_code)
);

CREATE TABLE inventory_transfer (
    transfer_id BIGINT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    from_warehouse VARCHAR(64) NOT NULL,
    to_warehouse VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_inventory_transfer_from (from_warehouse),
    KEY idx_inventory_transfer_to (to_warehouse)
);

CREATE TABLE logistics_waybill (
    waybill_id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    carrier_code VARCHAR(64) NOT NULL,
    route_code VARCHAR(64) NOT NULL,
    sla_hours INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    exception_reason VARCHAR(512) NOT NULL DEFAULT '',
    delivered_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    KEY idx_logistics_waybill_order (order_id),
    KEY idx_logistics_waybill_status (status)
);
