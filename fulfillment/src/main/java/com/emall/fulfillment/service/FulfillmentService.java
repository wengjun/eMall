package com.emall.fulfillment.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.fulfillment.domain.CarrierRoute;
import com.emall.fulfillment.domain.FulfillmentOrder;
import com.emall.fulfillment.domain.ShipmentStatus;
import com.emall.fulfillment.domain.TrackingEvent;
import com.emall.fulfillment.domain.WarehouseNode;
import com.emall.fulfillment.repository.FulfillmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FulfillmentService {
    private final FulfillmentRepository fulfillmentRepository;
    private final SnowflakeIdGenerator idGenerator;

    public FulfillmentService(FulfillmentRepository fulfillmentRepository, SnowflakeIdGenerator idGenerator) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public synchronized FulfillmentOrder allocate(long orderId, long userId, long skuId, int quantity,
                                                  String warehouseCode) {
        return fulfillmentRepository.findByOrderId(orderId)
                .orElseGet(() -> allocateOnce(orderId, userId, skuId, quantity, warehouseCode));
    }

    @Transactional
    public synchronized FulfillmentOrder allocateWithPlan(long orderId, long userId, long skuId, int quantity,
                                                          String destinationRegionCode) {
        return fulfillmentRepository.findByOrderId(orderId)
                .orElseGet(() -> allocateWithRoutePlan(orderId, userId, skuId, quantity, destinationRegionCode));
    }

    public FulfillmentOrder get(long fulfillmentId) {
        return fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "fulfillment order not found"));
    }

    public FulfillmentOrder getByOrderId(long orderId) {
        return fulfillmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "fulfillment order not found"));
    }

    @Transactional
    public WarehouseNode upsertWarehouse(String warehouseCode, String regionCode, int priority, int dailyCapacity,
                                         boolean enabled) {
        Instant now = Instant.now();
        Instant createdAt = fulfillmentRepository.findWarehouse(warehouseCode)
                .map(WarehouseNode::createdAt)
                .orElse(now);
        return fulfillmentRepository.saveWarehouse(new WarehouseNode(warehouseCode, regionCode, priority,
                dailyCapacity, enabled, createdAt, now));
    }

    public List<WarehouseNode> findEnabledWarehouses() {
        return fulfillmentRepository.findEnabledWarehouses();
    }

    @Transactional
    public CarrierRoute createCarrierRoute(String carrierCode, String originWarehouseCode,
                                           String destinationRegionCode, int priority, BigDecimal baseCost,
                                           int slaHours) {
        if (baseCost.signum() < 0 || slaHours <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "carrier route cost and SLA are invalid");
        }
        Instant now = Instant.now();
        return fulfillmentRepository.saveCarrierRoute(new CarrierRoute(idGenerator.nextId(), carrierCode,
                originWarehouseCode, destinationRegionCode, priority, baseCost.setScale(2, RoundingMode.HALF_UP),
                slaHours, true, now, now));
    }

    public List<CarrierRoute> findCarrierRoutes(String originWarehouseCode, String destinationRegionCode) {
        return fulfillmentRepository.findActiveCarrierRoutes(originWarehouseCode, destinationRegionCode);
    }

    @Transactional
    public FulfillmentOrder ship(long fulfillmentId, String carrier, String trackingNo) {
        FulfillmentOrder order = get(fulfillmentId);
        if (order.status() != ShipmentStatus.ALLOCATED) {
            throw new BusinessException(ErrorCode.CONFLICT, "shipment cannot be shipped from " + order.status());
        }
        return fulfillmentRepository.save(order.ship(carrier, trackingNo));
    }

    @Transactional
    public FulfillmentOrder deliver(long fulfillmentId) {
        FulfillmentOrder order = get(fulfillmentId);
        if (order.status() != ShipmentStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.CONFLICT, "shipment cannot be delivered from " + order.status());
        }
        return fulfillmentRepository.save(order.deliver());
    }

    @Transactional
    public TrackingEvent ingestTrackingEvent(long fulfillmentId, String carrierCode, String trackingNo,
                                             String eventCode, Instant eventTime, String location,
                                             String description) {
        FulfillmentOrder order = get(fulfillmentId);
        TrackingEvent event = fulfillmentRepository.saveTrackingEvent(new TrackingEvent(idGenerator.nextId(),
                fulfillmentId, carrierCode, trackingNo, eventCode, eventTime, location, description, Instant.now()));
        applyTrackingStatus(order, carrierCode, trackingNo, eventCode);
        return event;
    }

    public List<TrackingEvent> findTrackingEvents(long fulfillmentId) {
        get(fulfillmentId);
        return fulfillmentRepository.findTrackingEvents(fulfillmentId);
    }

    private FulfillmentOrder allocateOnce(long orderId, long userId, long skuId, int quantity, String warehouseCode) {
        Instant now = Instant.now();
        return fulfillmentRepository.save(FulfillmentOrder.allocated(idGenerator.nextId(), orderId, userId, skuId,
                quantity, null, warehouseCode, null, 0, now));
    }

    private FulfillmentOrder allocateWithRoutePlan(long orderId, long userId, long skuId, int quantity,
                                                   String destinationRegionCode) {
        WarehouseNode warehouse = chooseWarehouse(destinationRegionCode);
        CarrierRoute route = chooseCarrierRoute(warehouse.warehouseCode(), destinationRegionCode);
        Instant now = Instant.now();
        return fulfillmentRepository.save(FulfillmentOrder.allocated(idGenerator.nextId(), orderId, userId, skuId,
                quantity, destinationRegionCode, warehouse.warehouseCode(), route.carrierCode(), route.slaHours(),
                now));
    }

    private WarehouseNode chooseWarehouse(String destinationRegionCode) {
        List<WarehouseNode> warehouses = fulfillmentRepository.findEnabledWarehouses();
        return warehouses.stream()
                .filter(warehouse -> warehouse.regionCode().equals(destinationRegionCode))
                .min(Comparator.comparingInt(WarehouseNode::priority)
                        .thenComparing(WarehouseNode::warehouseCode))
                .or(() -> warehouses.stream()
                        .min(Comparator.comparingInt(WarehouseNode::priority)
                                .thenComparing(WarehouseNode::warehouseCode)))
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "no enabled warehouse available"));
    }

    private CarrierRoute chooseCarrierRoute(String warehouseCode, String destinationRegionCode) {
        return fulfillmentRepository.findActiveCarrierRoutes(warehouseCode, destinationRegionCode)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "no active carrier route available"));
    }

    private void applyTrackingStatus(FulfillmentOrder order, String carrierCode, String trackingNo, String eventCode) {
        if ("SHIPPED".equalsIgnoreCase(eventCode) && order.status() == ShipmentStatus.ALLOCATED) {
            fulfillmentRepository.save(order.ship(carrierCode, trackingNo));
            return;
        }
        if (!"DELIVERED".equalsIgnoreCase(eventCode)) {
            return;
        }
        FulfillmentOrder current = order;
        if (current.status() == ShipmentStatus.ALLOCATED) {
            current = fulfillmentRepository.save(current.ship(carrierCode, trackingNo));
        }
        if (current.status() == ShipmentStatus.SHIPPED) {
            fulfillmentRepository.save(current.deliver());
        }
    }
}
