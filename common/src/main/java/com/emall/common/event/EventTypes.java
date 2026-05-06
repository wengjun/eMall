package com.emall.common.event;

public final class EventTypes {
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_CONFIRMED = "inventory.confirmed";
    public static final String INVENTORY_RELEASED = "inventory.released";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_REFUNDED = "payment.refunded";
    public static final String PRODUCT_CHANGED = "product.changed";

    private EventTypes() {
    }
}
