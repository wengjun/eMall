package com.emall.common.rpc;

import java.io.Serializable;

public record PriceQuoteCommand(long skuId, int quantity) implements Serializable {
}
