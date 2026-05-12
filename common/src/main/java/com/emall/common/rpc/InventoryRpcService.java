package com.emall.common.rpc;

public interface InventoryRpcService {
    InventoryReservationView reserve(ReserveInventoryCommand command);

    InventoryReservationView confirm(String requestId);

    InventoryReservationView release(String requestId);
}
