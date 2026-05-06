package com.emall.fulfillment.repository;

import com.emall.fulfillment.domain.CarrierRoute;
import com.emall.fulfillment.domain.FulfillmentOrder;
import com.emall.fulfillment.domain.TrackingEvent;
import com.emall.fulfillment.domain.WarehouseNode;
import java.util.List;
import java.util.Optional;

public interface FulfillmentRepository {
    FulfillmentOrder save(FulfillmentOrder order);

    Optional<FulfillmentOrder> findById(long fulfillmentId);

    Optional<FulfillmentOrder> findByOrderId(long orderId);

    WarehouseNode saveWarehouse(WarehouseNode warehouse);

    Optional<WarehouseNode> findWarehouse(String warehouseCode);

    List<WarehouseNode> findEnabledWarehouses();

    CarrierRoute saveCarrierRoute(CarrierRoute route);

    Optional<CarrierRoute> findCarrierRoute(long routeId);

    List<CarrierRoute> findActiveCarrierRoutes(String originWarehouseCode, String destinationRegionCode);

    TrackingEvent saveTrackingEvent(TrackingEvent event);

    List<TrackingEvent> findTrackingEvents(long fulfillmentId);
}
