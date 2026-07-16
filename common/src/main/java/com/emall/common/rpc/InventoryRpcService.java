package com.emall.common.rpc;

public interface InventoryRpcService {
    InventoryReservationView reserve(ReserveInventoryCommand command);

    InventoryReservationView getReservation(String requestId);

    InventoryReservationView confirm(String requestId);

    InventoryReservationView release(String requestId);
}
