package com.emall.common.rpc;

import java.io.Serializable;

public record ReserveInventoryCommand(String requestId, long skuId, int quantity) implements Serializable {
}
