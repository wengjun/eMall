package com.emall.supplychain;

import com.emall.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/supply-chain")
class SupplyChainController {
    private final SupplyChainService supplyChainService;

    SupplyChainController(SupplyChainService supplyChainService) {
        this.supplyChainService = supplyChainService;
    }

    @PostMapping("/receipts")
    ApiResponse<WarehouseReceipt> receiveStock(@Valid @RequestBody ReceiveStockRequest request) {
        return ApiResponse.ok(supplyChainService.receiveStock(request.skuId(), request.warehouseCode(),
                request.batchNo(), request.quantity(), request.expiresOn()));
    }

    @PatchMapping("/receipts/{receiptId}/shelve")
    ApiResponse<WarehouseReceipt> shelve(@PathVariable long receiptId) {
        return ApiResponse.ok(supplyChainService.shelve(receiptId));
    }

    @PostMapping("/transfers")
    ApiResponse<InventoryTransfer> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        return ApiResponse.ok(supplyChainService.createTransfer(request.skuId(), request.fromWarehouse(),
                request.toWarehouse(), request.quantity()));
    }

    @PatchMapping("/transfers/{transferId}/status")
    ApiResponse<InventoryTransfer> changeTransferStatus(@PathVariable long transferId,
            @Valid @RequestBody ChangeTransferStatusRequest request) {
        return ApiResponse.ok(supplyChainService.changeTransferStatus(transferId, request.status()));
    }

    @PostMapping("/waybills")
    ApiResponse<LogisticsWaybill> createWaybill(@Valid @RequestBody CreateWaybillRequest request) {
        return ApiResponse.ok(supplyChainService.createWaybill(request.orderId(), request.carrierCode(),
                request.routeCode(), request.slaHours()));
    }

    @PatchMapping("/waybills/{waybillId}/exceptions")
    ApiResponse<LogisticsWaybill> reportException(@PathVariable long waybillId,
            @Valid @RequestBody ReportExceptionRequest request) {
        return ApiResponse.ok(supplyChainService.reportDeliveryException(waybillId, request.reason()));
    }

    @PatchMapping("/waybills/{waybillId}/delivery")
    ApiResponse<LogisticsWaybill> confirmDelivery(@PathVariable long waybillId) {
        return ApiResponse.ok(supplyChainService.confirmDelivery(waybillId));
    }

    @GetMapping("/summary")
    ApiResponse<SupplyChainSummary> summary(@RequestParam String warehouseCode) {
        return ApiResponse.ok(supplyChainService.summary(warehouseCode));
    }

    record ReceiveStockRequest(@Positive long skuId, @NotBlank String warehouseCode, @NotBlank String batchNo,
            @Positive int quantity, LocalDate expiresOn) {
    }

    record CreateTransferRequest(@Positive long skuId, @NotBlank String fromWarehouse, @NotBlank String toWarehouse,
            @Positive int quantity) {
    }

    record ChangeTransferStatusRequest(TransferStatus status) {
    }

    record CreateWaybillRequest(@Positive long orderId, @NotBlank String carrierCode, @NotBlank String routeCode,
            @Positive int slaHours) {
    }

    record ReportExceptionRequest(@NotBlank String reason) {
    }
}
