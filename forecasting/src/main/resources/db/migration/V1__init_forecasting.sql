CREATE TABLE demand_signal (
    signal_id BIGINT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    region_code VARCHAR(64) NOT NULL,
    sold_quantity INT NOT NULL,
    page_views INT NOT NULL,
    signal_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_demand_signal_sku_region (sku_id, region_code)
);

CREATE TABLE demand_forecast (
    forecast_id BIGINT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    region_code VARCHAR(64) NOT NULL,
    forecast_quantity INT NOT NULL,
    stockout_risk VARCHAR(32) NOT NULL,
    forecast_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_demand_forecast_sku_region (sku_id, region_code),
    KEY idx_demand_forecast_risk (stockout_risk)
);

CREATE TABLE replenishment_plan (
    plan_id BIGINT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    warehouse_code VARCHAR(64) NOT NULL,
    recommended_quantity INT NOT NULL,
    priority VARCHAR(32) NOT NULL,
    plan_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_replenishment_plan_warehouse (warehouse_code),
    KEY idx_replenishment_plan_priority (priority)
);

CREATE TABLE capacity_forecast (
    capacity_forecast_id BIGINT PRIMARY KEY,
    warehouse_code VARCHAR(64) NOT NULL,
    forecast_orders INT NOT NULL,
    worker_hours INT NOT NULL,
    pressure_level VARCHAR(32) NOT NULL,
    forecast_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_capacity_forecast_warehouse (warehouse_code),
    KEY idx_capacity_forecast_pressure (pressure_level)
);
