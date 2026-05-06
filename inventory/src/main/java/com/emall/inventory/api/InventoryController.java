package com.emall.inventory.api;

import com.emall.common.api.ApiResponse;
import com.emall.inventory.domain.InventoryBucket;
import com.emall.inventory.domain.InventoryItem;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{skuId}")
    public ApiResponse<InventoryItem> getStock(@PathVariable long skuId) {
        return ApiResponse.ok(inventoryService.get(skuId));
    }

    @PostMapping("/{skuId}/stock")
    public ApiResponse<InventoryItem> addStock(@PathVariable long skuId, @Valid @RequestBody AddStockRequest request) {
        return ApiResponse.ok(inventoryService.addStock(skuId, request.quantity()));
    }

    @GetMapping("/{skuId}/buckets")
    public ApiResponse<List<InventoryBucket>> buckets(@PathVariable long skuId) {
        return ApiResponse.ok(inventoryService.buckets(skuId));
    }

    @PostMapping("/{skuId}/buckets")
    public ApiResponse<List<InventoryBucket>> initializeBuckets(@PathVariable long skuId,
                                                                @Valid @RequestBody InitBucketsRequest request) {
        return ApiResponse.ok(inventoryService.initializeBuckets(skuId, request.bucketCount()));
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<InventoryReservation> reserve(@Valid @RequestBody ReserveInventoryRequest request) {
        return ApiResponse.ok(inventoryService.reserve(request.requestId(), request.skuId(), request.quantity()));
    }

    @PostMapping("/reservations/{requestId}/confirm")
    public ApiResponse<InventoryReservation> confirm(@PathVariable String requestId) {
        return ApiResponse.ok(inventoryService.confirm(requestId));
    }

    @PostMapping("/reservations/{requestId}/release")
    public ApiResponse<InventoryReservation> release(@PathVariable String requestId) {
        return ApiResponse.ok(inventoryService.release(requestId));
    }

    public record AddStockRequest(@Positive int quantity) {
    }

    public record InitBucketsRequest(@Positive @Max(256) int bucketCount) {
    }

    public record ReserveInventoryRequest(@NotBlank String requestId, @Positive long skuId, @Positive int quantity) {
    }
}
