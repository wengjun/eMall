package com.emall.fulfillment.repository;

import com.emall.fulfillment.domain.CarrierRoute;
import com.emall.fulfillment.domain.FulfillmentOrder;
import com.emall.fulfillment.domain.ShipmentStatus;
import com.emall.fulfillment.domain.TrackingEvent;
import com.emall.fulfillment.domain.WarehouseNode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcFulfillmentRepository implements FulfillmentRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcFulfillmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FulfillmentOrder save(FulfillmentOrder order) {
        jdbcTemplate.update("""
                INSERT INTO fulfillment_order
                    (fulfillment_id, order_id, user_id, sku_id, quantity, destination_region_code, warehouse_code,
                    planned_carrier, estimated_sla_hours, carrier, tracking_no, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE destination_region_code = VALUES(destination_region_code),
                    warehouse_code = VALUES(warehouse_code), planned_carrier = VALUES(planned_carrier),
                    estimated_sla_hours = VALUES(estimated_sla_hours), carrier = VALUES(carrier),
                    tracking_no = VALUES(tracking_no), status = VALUES(status), updated_at = VALUES(updated_at)
                """,
                order.fulfillmentId(), order.orderId(), order.userId(), order.skuId(), order.quantity(),
                order.destinationRegionCode(), order.warehouseCode(), order.plannedCarrier(), order.estimatedSlaHours(),
                order.carrier(), order.trackingNo(), order.status().name(), Timestamp.from(order.createdAt()),
                Timestamp.from(order.updatedAt()));
        return order;
    }

    @Override
    public Optional<FulfillmentOrder> findById(long fulfillmentId) {
        return jdbcTemplate.query("SELECT * FROM fulfillment_order WHERE fulfillment_id = ?", this::map,
                fulfillmentId).stream().findFirst();
    }

    @Override
    public Optional<FulfillmentOrder> findByOrderId(long orderId) {
        return jdbcTemplate.query("SELECT * FROM fulfillment_order WHERE order_id = ?", this::map, orderId)
                .stream()
                .findFirst();
    }

    @Override
    public WarehouseNode saveWarehouse(WarehouseNode warehouse) {
        jdbcTemplate.update("""
                INSERT INTO fulfillment_warehouse
                    (warehouse_code, region_code, priority, daily_capacity, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE region_code = VALUES(region_code), priority = VALUES(priority),
                    daily_capacity = VALUES(daily_capacity), enabled = VALUES(enabled), updated_at = VALUES(updated_at)
                """,
                warehouse.warehouseCode(), warehouse.regionCode(), warehouse.priority(), warehouse.dailyCapacity(),
                warehouse.enabled(), Timestamp.from(warehouse.createdAt()), Timestamp.from(warehouse.updatedAt()));
        return warehouse;
    }

    @Override
    public Optional<WarehouseNode> findWarehouse(String warehouseCode) {
        return jdbcTemplate.query("SELECT * FROM fulfillment_warehouse WHERE warehouse_code = ?",
                this::mapWarehouse, warehouseCode).stream().findFirst();
    }

    @Override
    public List<WarehouseNode> findEnabledWarehouses() {
        return jdbcTemplate.query("""
                SELECT * FROM fulfillment_warehouse
                WHERE enabled = TRUE
                ORDER BY region_code ASC, priority ASC, warehouse_code ASC
                """, this::mapWarehouse);
    }

    @Override
    public CarrierRoute saveCarrierRoute(CarrierRoute route) {
        jdbcTemplate.update("""
                INSERT INTO fulfillment_carrier_route
                    (route_id, carrier_code, origin_warehouse_code, destination_region_code, priority, base_cost,
                    sla_hours, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE carrier_code = VALUES(carrier_code),
                    origin_warehouse_code = VALUES(origin_warehouse_code),
                    destination_region_code = VALUES(destination_region_code), priority = VALUES(priority),
                    base_cost = VALUES(base_cost), sla_hours = VALUES(sla_hours), active = VALUES(active),
                    updated_at = VALUES(updated_at)
                """,
                route.routeId(), route.carrierCode(), route.originWarehouseCode(), route.destinationRegionCode(),
                route.priority(), route.baseCost(), route.slaHours(), route.active(), Timestamp.from(route.createdAt()),
                Timestamp.from(route.updatedAt()));
        return route;
    }

    @Override
    public Optional<CarrierRoute> findCarrierRoute(long routeId) {
        return jdbcTemplate.query("SELECT * FROM fulfillment_carrier_route WHERE route_id = ?",
                this::mapCarrierRoute, routeId).stream().findFirst();
    }

    @Override
    public List<CarrierRoute> findActiveCarrierRoutes(String originWarehouseCode, String destinationRegionCode) {
        return jdbcTemplate.query("""
                SELECT * FROM fulfillment_carrier_route
                WHERE origin_warehouse_code = ? AND destination_region_code = ? AND active = TRUE
                ORDER BY priority ASC, base_cost ASC, route_id ASC
                """, this::mapCarrierRoute, originWarehouseCode, destinationRegionCode);
    }

    @Override
    public TrackingEvent saveTrackingEvent(TrackingEvent event) {
        jdbcTemplate.update("""
                INSERT INTO fulfillment_tracking_event
                    (event_id, fulfillment_id, carrier_code, tracking_no, event_code, event_time, location,
                    description, received_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE location = VALUES(location), description = VALUES(description),
                    received_at = VALUES(received_at)
                """,
                event.eventId(), event.fulfillmentId(), event.carrierCode(), event.trackingNo(), event.eventCode(),
                Timestamp.from(event.eventTime()), event.location(), event.description(),
                Timestamp.from(event.receivedAt()));
        return event;
    }

    @Override
    public List<TrackingEvent> findTrackingEvents(long fulfillmentId) {
        return jdbcTemplate.query("""
                SELECT * FROM fulfillment_tracking_event
                WHERE fulfillment_id = ?
                ORDER BY event_time ASC, event_id ASC
                """, this::mapTrackingEvent, fulfillmentId);
    }

    private FulfillmentOrder map(ResultSet rs, int rowNum) throws SQLException {
        return new FulfillmentOrder(
                rs.getLong("fulfillment_id"),
                rs.getLong("order_id"),
                rs.getLong("user_id"),
                rs.getLong("sku_id"),
                rs.getInt("quantity"),
                rs.getString("destination_region_code"),
                rs.getString("warehouse_code"),
                rs.getString("planned_carrier"),
                rs.getInt("estimated_sla_hours"),
                rs.getString("carrier"),
                rs.getString("tracking_no"),
                ShipmentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private WarehouseNode mapWarehouse(ResultSet rs, int rowNum) throws SQLException {
        return new WarehouseNode(
                rs.getString("warehouse_code"),
                rs.getString("region_code"),
                rs.getInt("priority"),
                rs.getInt("daily_capacity"),
                rs.getBoolean("enabled"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private CarrierRoute mapCarrierRoute(ResultSet rs, int rowNum) throws SQLException {
        return new CarrierRoute(
                rs.getLong("route_id"),
                rs.getString("carrier_code"),
                rs.getString("origin_warehouse_code"),
                rs.getString("destination_region_code"),
                rs.getInt("priority"),
                rs.getBigDecimal("base_cost"),
                rs.getInt("sla_hours"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private TrackingEvent mapTrackingEvent(ResultSet rs, int rowNum) throws SQLException {
        return new TrackingEvent(
                rs.getLong("event_id"),
                rs.getLong("fulfillment_id"),
                rs.getString("carrier_code"),
                rs.getString("tracking_no"),
                rs.getString("event_code"),
                rs.getTimestamp("event_time").toInstant(),
                rs.getString("location"),
                rs.getString("description"),
                rs.getTimestamp("received_at").toInstant());
    }
}
