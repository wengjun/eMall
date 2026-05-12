package com.emall.inventory.rpc;

import com.emall.common.rpc.InventoryReservationView;
import com.emall.common.rpc.InventoryRpcService;
import com.emall.common.rpc.ReserveInventoryCommand;
import com.emall.inventory.domain.InventoryReservation;
import com.emall.inventory.service.InventoryService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@DubboService
@ConditionalOnProperty(name = "emall.rpc.protocol", havingValue = "dubbo")
public class InventoryDubboService implements InventoryRpcService {
    private final InventoryService inventoryService;

    public InventoryDubboService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public InventoryReservationView reserve(ReserveInventoryCommand command) {
        return toView(inventoryService.reserve(command.requestId(), command.skuId(), command.quantity()));
    }

    @Override
    public InventoryReservationView confirm(String requestId) {
        return toView(inventoryService.confirm(requestId));
    }

    @Override
    public InventoryReservationView release(String requestId) {
        return toView(inventoryService.release(requestId));
    }

    private InventoryReservationView toView(InventoryReservation reservation) {
        return new InventoryReservationView(reservation.requestId(), reservation.skuId(), reservation.quantity(),
                reservation.status().name(), reservation.reason(), reservation.expiresAt(), reservation.createdAt(),
                reservation.updatedAt());
    }
}
