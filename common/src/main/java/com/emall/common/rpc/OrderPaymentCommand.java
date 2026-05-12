package com.emall.common.rpc;

import java.io.Serializable;

public record OrderPaymentCommand(long orderId) implements Serializable {
}
