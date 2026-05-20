package com.emall.common.metrics;

public final class BusinessMetricNames {
    public static final String ORDER_CREATED = "emall_order_created_total";
    public static final String ORDER_PAID = "emall_order_paid_total";
    public static final String ORDER_CANCELLED = "emall_order_cancelled_total";
    public static final String INVENTORY_RESERVED = "emall_inventory_reserved_total";
    public static final String INVENTORY_REJECTED = "emall_inventory_rejected_total";
    public static final String INVENTORY_CONFIRMED = "emall_inventory_confirmed_total";
    public static final String INVENTORY_RELEASED = "emall_inventory_released_total";
    public static final String PAYMENT_SUCCEEDED = "emall_payment_succeeded_total";
    public static final String PAYMENT_REFUNDED = "emall_payment_refunded_total";
    public static final String PAYMENT_ORDER_CONFIRM_FAILED = "emall_payment_order_confirm_failed_total";
    public static final String OUTBOX_CLAIMED = "emall_outbox_claimed_total";
    public static final String OUTBOX_PUBLISHED = "emall_outbox_published_total";
    public static final String OUTBOX_FAILED = "emall_outbox_failed_total";
    public static final String FLASH_SALE_TOKEN_ISSUED = "emall_flash_sale_token_issued_total";
    public static final String FLASH_SALE_QUEUE_ENQUEUED = "emall_flash_sale_queue_enqueued_total";
    public static final String FLASH_SALE_QUEUE_REJECTED = "emall_flash_sale_queue_rejected_total";
    public static final String SEARCH_PRODUCT_EVENT_INDEXED = "emall_search_product_event_indexed_total";
    public static final String SEARCH_PRODUCT_EVENT_DUPLICATED = "emall_search_product_event_duplicated_total";
    public static final String SEARCH_PRODUCT_EVENT_FAILED = "emall_search_product_event_failed_total";
    public static final String SEARCH_PRODUCT_EVENT_DEAD = "emall_search_product_event_dead_total";
    public static final String MESSAGE_CONSUMED = "emall_message_consumed_total";
    public static final String MESSAGE_DUPLICATED = "emall_message_duplicated_total";
    public static final String MESSAGE_FAILED = "emall_message_failed_total";
    public static final String MESSAGE_DEAD = "emall_message_dead_total";
    public static final String COUPON_RESERVED = "emall_coupon_reserved_total";
    public static final String COUPON_CONFIRMED = "emall_coupon_confirmed_total";
    public static final String COUPON_RELEASED = "emall_coupon_released_total";
    public static final String ORDER_PENDING_RETRY = "emall_order_pending_retry_total";

    private BusinessMetricNames() {
    }
}
