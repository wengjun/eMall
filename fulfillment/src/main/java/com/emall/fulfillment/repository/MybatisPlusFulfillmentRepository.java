package com.emall.fulfillment.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.fulfillment.domain.CarrierRoute;
import com.emall.fulfillment.domain.FulfillmentOrder;
import com.emall.fulfillment.domain.ShipmentStatus;
import com.emall.fulfillment.domain.TrackingEvent;
import com.emall.fulfillment.domain.WarehouseNode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusFulfillmentRepository implements FulfillmentRepository {
    private final FulfillmentOrderMapper orderMapper;
    private final FulfillmentWarehouseMapper warehouseMapper;
    private final FulfillmentCarrierRouteMapper carrierRouteMapper;
    private final FulfillmentTrackingEventMapper trackingEventMapper;

    public MybatisPlusFulfillmentRepository(FulfillmentOrderMapper orderMapper,
            FulfillmentWarehouseMapper warehouseMapper, FulfillmentCarrierRouteMapper carrierRouteMapper,
            FulfillmentTrackingEventMapper trackingEventMapper) {
        this.orderMapper = orderMapper;
        this.warehouseMapper = warehouseMapper;
        this.carrierRouteMapper = carrierRouteMapper;
        this.trackingEventMapper = trackingEventMapper;
    }

    @Override
    public FulfillmentOrder save(FulfillmentOrder order) {
        FulfillmentOrderEntity entity = toEntity(order);
        try {
            orderMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            orderMapper.update(null, new UpdateWrapper<FulfillmentOrderEntity>()
                    .set("destination_region_code", entity.getDestinationRegionCode())
                    .set("warehouse_code", entity.getWarehouseCode()).set("planned_carrier", entity.getPlannedCarrier())
                    .set("estimated_sla_hours", entity.getEstimatedSlaHours()).set("carrier", entity.getCarrier())
                    .set("tracking_no", entity.getTrackingNo()).set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt()).eq("fulfillment_id", entity.getFulfillmentId()));
        }
        return order;
    }

    @Override
    public Optional<FulfillmentOrder> findById(long fulfillmentId) {
        return Optional.ofNullable(orderMapper.selectById(fulfillmentId)).map(this::toDomain);
    }

    @Override
    public Optional<FulfillmentOrder> findByOrderId(long orderId) {
        return Optional
                .ofNullable(orderMapper.selectOne(new QueryWrapper<FulfillmentOrderEntity>().eq("order_id", orderId)))
                .map(this::toDomain);
    }

    @Override
    public WarehouseNode saveWarehouse(WarehouseNode warehouse) {
        FulfillmentWarehouseEntity entity = toEntity(warehouse);
        try {
            warehouseMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            warehouseMapper.update(null,
                    new UpdateWrapper<FulfillmentWarehouseEntity>().set("region_code", entity.getRegionCode())
                            .set("priority", entity.getPriority()).set("daily_capacity", entity.getDailyCapacity())
                            .set("enabled", entity.getEnabled()).set("updated_at", entity.getUpdatedAt())
                            .eq("warehouse_code", entity.getWarehouseCode()));
        }
        return warehouse;
    }

    @Override
    public Optional<WarehouseNode> findWarehouse(String warehouseCode) {
        return Optional.ofNullable(warehouseMapper.selectById(warehouseCode)).map(this::toDomain);
    }

    @Override
    public List<WarehouseNode> findEnabledWarehouses() {
        return warehouseMapper.selectList(new QueryWrapper<FulfillmentWarehouseEntity>().eq("enabled", true)
                .orderByAsc("region_code", "priority", "warehouse_code")).stream().map(this::toDomain).toList();
    }

    @Override
    public CarrierRoute saveCarrierRoute(CarrierRoute route) {
        FulfillmentCarrierRouteEntity entity = toEntity(route);
        try {
            carrierRouteMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            carrierRouteMapper.update(null,
                    new UpdateWrapper<FulfillmentCarrierRouteEntity>().set("carrier_code", entity.getCarrierCode())
                            .set("origin_warehouse_code", entity.getOriginWarehouseCode())
                            .set("destination_region_code", entity.getDestinationRegionCode())
                            .set("priority", entity.getPriority()).set("base_cost", entity.getBaseCost())
                            .set("sla_hours", entity.getSlaHours()).set("active", entity.getActive())
                            .set("updated_at", entity.getUpdatedAt()).eq("route_id", entity.getRouteId()));
        }
        return route;
    }

    @Override
    public Optional<CarrierRoute> findCarrierRoute(long routeId) {
        return Optional.ofNullable(carrierRouteMapper.selectById(routeId)).map(this::toDomain);
    }

    @Override
    public List<CarrierRoute> findActiveCarrierRoutes(String originWarehouseCode, String destinationRegionCode) {
        return carrierRouteMapper.selectList(new QueryWrapper<FulfillmentCarrierRouteEntity>()
                .eq("origin_warehouse_code", originWarehouseCode).eq("destination_region_code", destinationRegionCode)
                .eq("active", true).orderByAsc("priority", "base_cost", "route_id")).stream().map(this::toDomain)
                .toList();
    }

    @Override
    public TrackingEvent saveTrackingEvent(TrackingEvent event) {
        FulfillmentTrackingEventEntity entity = toEntity(event);
        try {
            trackingEventMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            trackingEventMapper.update(null,
                    new UpdateWrapper<FulfillmentTrackingEventEntity>().set("location", entity.getLocation())
                            .set("description", entity.getDescription()).set("received_at", entity.getReceivedAt())
                            .eq("event_id", entity.getEventId()));
        }
        return event;
    }

    @Override
    public List<TrackingEvent> findTrackingEvents(long fulfillmentId) {
        return trackingEventMapper.selectList(new QueryWrapper<FulfillmentTrackingEventEntity>()
                .eq("fulfillment_id", fulfillmentId).orderByAsc("event_time", "event_id")).stream().map(this::toDomain)
                .toList();
    }

    private FulfillmentOrderEntity toEntity(FulfillmentOrder order) {
        FulfillmentOrderEntity entity = new FulfillmentOrderEntity();
        entity.setFulfillmentId(order.fulfillmentId());
        entity.setOrderId(order.orderId());
        entity.setUserId(order.userId());
        entity.setSkuId(order.skuId());
        entity.setQuantity(order.quantity());
        entity.setDestinationRegionCode(order.destinationRegionCode());
        entity.setWarehouseCode(order.warehouseCode());
        entity.setPlannedCarrier(order.plannedCarrier());
        entity.setEstimatedSlaHours(order.estimatedSlaHours());
        entity.setCarrier(order.carrier());
        entity.setTrackingNo(order.trackingNo());
        entity.setStatus(order.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(order.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(order.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private FulfillmentOrder toDomain(FulfillmentOrderEntity entity) {
        return new FulfillmentOrder(entity.getFulfillmentId(), entity.getOrderId(), entity.getUserId(),
                entity.getSkuId(), entity.getQuantity(), entity.getDestinationRegionCode(), entity.getWarehouseCode(),
                entity.getPlannedCarrier(), entity.getEstimatedSlaHours(), entity.getCarrier(), entity.getTrackingNo(),
                ShipmentStatus.valueOf(entity.getStatus()), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private FulfillmentWarehouseEntity toEntity(WarehouseNode warehouse) {
        FulfillmentWarehouseEntity entity = new FulfillmentWarehouseEntity();
        entity.setWarehouseCode(warehouse.warehouseCode());
        entity.setRegionCode(warehouse.regionCode());
        entity.setPriority(warehouse.priority());
        entity.setDailyCapacity(warehouse.dailyCapacity());
        entity.setEnabled(warehouse.enabled());
        entity.setCreatedAt(LocalDateTime.ofInstant(warehouse.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(warehouse.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private WarehouseNode toDomain(FulfillmentWarehouseEntity entity) {
        return new WarehouseNode(entity.getWarehouseCode(), entity.getRegionCode(), entity.getPriority(),
                entity.getDailyCapacity(), entity.getEnabled(), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private FulfillmentCarrierRouteEntity toEntity(CarrierRoute route) {
        FulfillmentCarrierRouteEntity entity = new FulfillmentCarrierRouteEntity();
        entity.setRouteId(route.routeId());
        entity.setCarrierCode(route.carrierCode());
        entity.setOriginWarehouseCode(route.originWarehouseCode());
        entity.setDestinationRegionCode(route.destinationRegionCode());
        entity.setPriority(route.priority());
        entity.setBaseCost(route.baseCost());
        entity.setSlaHours(route.slaHours());
        entity.setActive(route.active());
        entity.setCreatedAt(LocalDateTime.ofInstant(route.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(route.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private CarrierRoute toDomain(FulfillmentCarrierRouteEntity entity) {
        return new CarrierRoute(entity.getRouteId(), entity.getCarrierCode(), entity.getOriginWarehouseCode(),
                entity.getDestinationRegionCode(), entity.getPriority(), entity.getBaseCost(), entity.getSlaHours(),
                entity.getActive(), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private FulfillmentTrackingEventEntity toEntity(TrackingEvent event) {
        FulfillmentTrackingEventEntity entity = new FulfillmentTrackingEventEntity();
        entity.setEventId(event.eventId());
        entity.setFulfillmentId(event.fulfillmentId());
        entity.setCarrierCode(event.carrierCode());
        entity.setTrackingNo(event.trackingNo());
        entity.setEventCode(event.eventCode());
        entity.setEventTime(LocalDateTime.ofInstant(event.eventTime(), ZoneOffset.UTC));
        entity.setLocation(event.location());
        entity.setDescription(event.description());
        entity.setReceivedAt(LocalDateTime.ofInstant(event.receivedAt(), ZoneOffset.UTC));
        return entity;
    }

    private TrackingEvent toDomain(FulfillmentTrackingEventEntity entity) {
        return new TrackingEvent(entity.getEventId(), entity.getFulfillmentId(), entity.getCarrierCode(),
                entity.getTrackingNo(), entity.getEventCode(), entity.getEventTime().toInstant(ZoneOffset.UTC),
                entity.getLocation(), entity.getDescription(), entity.getReceivedAt().toInstant(ZoneOffset.UTC));
    }
}
