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
    public static final String FLASH_SALE_ORDER_QUEUED = "flash-sale.order.queued";
    public static final String ACCOUNT_REGISTERED = "identity.account.registered";
    public static final String ACCOUNT_ACTIVATED = "identity.account.activated";
    public static final String ACCOUNT_SUSPENDED = "identity.account.suspended";
    public static final String ACCOUNT_RESTORED = "identity.account.restored";
    public static final String ACCOUNT_CLOSED = "identity.account.closed";
    public static final String ACCOUNT_DELETION_REQUESTED = "identity.account.deletion-requested";
    public static final String ACCOUNT_DELETED = "identity.account.deleted";
    public static final String ACCOUNT_RECONCILIATION_REQUESTED = "identity.account.reconciliation-requested";
    public static final String USER_PROFILE_READY = "user.profile.ready";
    public static final String USER_PROFILE_DELETION_COMPLETED = "user.profile.deletion-completed";
    public static final String USER_PROFILE_RECONCILED = "user.profile.reconciled";

    private EventTypes() {
    }
}
