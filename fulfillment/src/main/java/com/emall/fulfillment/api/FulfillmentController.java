package com.emall.fulfillment.api;

import com.emall.common.api.ApiResponse;
import com.emall.fulfillment.domain.CarrierRoute;
import com.emall.fulfillment.domain.FulfillmentOrder;
import com.emall.fulfillment.domain.TrackingEvent;
import com.emall.fulfillment.domain.WarehouseNode;
import com.emall.fulfillment.service.FulfillmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fulfillment")
public class FulfillmentController {
    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FulfillmentOrder> allocate(@Valid @RequestBody AllocateRequest request) {
        return ApiResponse.ok(fulfillmentService.allocate(request.orderId(), request.userId(), request.skuId(),
                request.quantity(), request.warehouseCode()));
    }

    @PostMapping("/orders/plan")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FulfillmentOrder> allocateWithPlan(@Valid @RequestBody AllocateWithPlanRequest request) {
        return ApiResponse.ok(fulfillmentService.allocateWithPlan(request.orderId(), request.userId(), request.skuId(),
                request.quantity(), request.destinationRegionCode()));
    }

    @GetMapping("/orders/{fulfillmentId}")
    public ApiResponse<FulfillmentOrder> get(@PathVariable long fulfillmentId) {
        return ApiResponse.ok(fulfillmentService.get(fulfillmentId));
    }

    @GetMapping("/orders/by-order/{orderId}")
    public ApiResponse<FulfillmentOrder> getByOrderId(@PathVariable long orderId) {
        return ApiResponse.ok(fulfillmentService.getByOrderId(orderId));
    }

    @PostMapping("/orders/{fulfillmentId}/ship")
    public ApiResponse<FulfillmentOrder> ship(@PathVariable long fulfillmentId,
            @Valid @RequestBody ShipRequest request) {
        return ApiResponse.ok(fulfillmentService.ship(fulfillmentId, request.carrier(), request.trackingNo()));
    }

    @PostMapping("/orders/{fulfillmentId}/deliver")
    public ApiResponse<FulfillmentOrder> deliver(@PathVariable long fulfillmentId) {
        return ApiResponse.ok(fulfillmentService.deliver(fulfillmentId));
    }

    @PostMapping("/warehouses")
    public ApiResponse<WarehouseNode> upsertWarehouse(@Valid @RequestBody UpsertWarehouseRequest request) {
        return ApiResponse.ok(fulfillmentService.upsertWarehouse(request.warehouseCode(), request.regionCode(),
                request.priority(), request.dailyCapacity(), request.enabled()));
    }

    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseNode>> findEnabledWarehouses() {
        return ApiResponse.ok(fulfillmentService.findEnabledWarehouses());
    }

    @PostMapping("/carrier-routes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CarrierRoute> createCarrierRoute(@Valid @RequestBody CreateCarrierRouteRequest request) {
        return ApiResponse
                .ok(fulfillmentService.createCarrierRoute(request.carrierCode(), request.originWarehouseCode(),
                        request.destinationRegionCode(), request.priority(), request.baseCost(), request.slaHours()));
    }

    @GetMapping("/carrier-routes/{originWarehouseCode}/{destinationRegionCode}")
    public ApiResponse<List<CarrierRoute>> findCarrierRoutes(@PathVariable String originWarehouseCode,
            @PathVariable String destinationRegionCode) {
        return ApiResponse.ok(fulfillmentService.findCarrierRoutes(originWarehouseCode, destinationRegionCode));
    }

    @PostMapping("/orders/{fulfillmentId}/tracking-events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TrackingEvent> ingestTrackingEvent(@PathVariable long fulfillmentId,
            @Valid @RequestBody TrackingEventRequest request) {
        return ApiResponse
                .ok(fulfillmentService.ingestTrackingEvent(fulfillmentId, request.carrierCode(), request.trackingNo(),
                        request.eventCode(), request.eventTime(), request.location(), request.description()));
    }

    @GetMapping("/orders/{fulfillmentId}/tracking-events")
    public ApiResponse<List<TrackingEvent>> findTrackingEvents(@PathVariable long fulfillmentId) {
        return ApiResponse.ok(fulfillmentService.findTrackingEvents(fulfillmentId));
    }

    public record AllocateRequest(@Positive long orderId, @Positive long userId, @Positive long skuId,
            @Positive int quantity, @NotBlank String warehouseCode) {
    }

    public record AllocateWithPlanRequest(@Positive long orderId, @Positive long userId, @Positive long skuId,
            @Positive int quantity, @NotBlank String destinationRegionCode) {
    }

    public record ShipRequest(@NotBlank String carrier, @NotBlank String trackingNo) {
    }

    public record UpsertWarehouseRequest(@NotBlank String warehouseCode, @NotBlank String regionCode,
            @Min(0) int priority, @Positive int dailyCapacity, boolean enabled) {
    }

    public record CreateCarrierRouteRequest(@NotBlank String carrierCode, @NotBlank String originWarehouseCode,
            @NotBlank String destinationRegionCode, @Min(0) int priority,
            @NotNull @DecimalMin("0.00") BigDecimal baseCost, @Positive int slaHours) {
    }

    public record TrackingEventRequest(@NotBlank String carrierCode, @NotBlank String trackingNo,
            @NotBlank String eventCode, @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant eventTime,
            String location, String description) {
    }
}
