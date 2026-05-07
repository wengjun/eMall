package com.emall.fulfillment.repository;

import com.emall.fulfillment.domain.CarrierRoute;
import com.emall.fulfillment.domain.FulfillmentOrder;
import com.emall.fulfillment.domain.TrackingEvent;
import com.emall.fulfillment.domain.WarehouseNode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
public class InMemoryFulfillmentRepository implements FulfillmentRepository {
    private final ConcurrentMap<Long, FulfillmentOrder> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Long> idByOrderId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WarehouseNode> warehouses = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CarrierRoute> carrierRoutes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, TrackingEvent> trackingEvents = new ConcurrentHashMap<>();

    @Override
    public FulfillmentOrder save(FulfillmentOrder order) {
        byId.put(order.fulfillmentId(), order);
        idByOrderId.put(order.orderId(), order.fulfillmentId());
        return order;
    }

    @Override
    public Optional<FulfillmentOrder> findById(long fulfillmentId) {
        return Optional.ofNullable(byId.get(fulfillmentId));
    }

    @Override
    public Optional<FulfillmentOrder> findByOrderId(long orderId) {
        Long fulfillmentId = idByOrderId.get(orderId);
        return fulfillmentId == null ? Optional.empty() : findById(fulfillmentId);
    }

    @Override
    public WarehouseNode saveWarehouse(WarehouseNode warehouse) {
        warehouses.put(warehouse.warehouseCode(), warehouse);
        return warehouse;
    }

    @Override
    public Optional<WarehouseNode> findWarehouse(String warehouseCode) {
        return Optional.ofNullable(warehouses.get(warehouseCode));
    }

    @Override
    public List<WarehouseNode> findEnabledWarehouses() {
        return warehouses.values().stream().filter(WarehouseNode::enabled)
                .sorted(Comparator.comparing(WarehouseNode::regionCode).thenComparingInt(WarehouseNode::priority))
                .toList();
    }

    @Override
    public CarrierRoute saveCarrierRoute(CarrierRoute route) {
        carrierRoutes.put(route.routeId(), route);
        return route;
    }

    @Override
    public Optional<CarrierRoute> findCarrierRoute(long routeId) {
        return Optional.ofNullable(carrierRoutes.get(routeId));
    }

    @Override
    public List<CarrierRoute> findActiveCarrierRoutes(String originWarehouseCode, String destinationRegionCode) {
        return carrierRoutes.values().stream().filter(CarrierRoute::active)
                .filter(route -> route.originWarehouseCode().equals(originWarehouseCode))
                .filter(route -> route.destinationRegionCode().equals(destinationRegionCode))
                .sorted(Comparator.comparingInt(CarrierRoute::priority).thenComparing(CarrierRoute::baseCost)).toList();
    }

    @Override
    public TrackingEvent saveTrackingEvent(TrackingEvent event) {
        trackingEvents.put(event.eventId(), event);
        return event;
    }

    @Override
    public List<TrackingEvent> findTrackingEvents(long fulfillmentId) {
        return trackingEvents.values().stream().filter(event -> event.fulfillmentId() == fulfillmentId)
                .sorted(Comparator.comparing(TrackingEvent::eventTime).thenComparingLong(TrackingEvent::eventId))
                .toList();
    }
}
