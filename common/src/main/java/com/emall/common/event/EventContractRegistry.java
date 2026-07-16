package com.emall.common.event;

import java.util.Map;
import java.util.function.Consumer;

public final class EventContractRegistry {
    private static final Map<String, Consumer<OutboxEvent>> VALIDATORS = Map.ofEntries(
            Map.entry(EventTypes.ORDER_CREATED, event -> OrderEventPayload.from(event)),
            Map.entry(EventTypes.ORDER_PAID, event -> OrderEventPayload.from(event)),
            Map.entry(EventTypes.ORDER_CANCELLED, event -> OrderEventPayload.from(event)),
            Map.entry(EventTypes.INVENTORY_RESERVED, event -> InventoryReservationEventPayload.from(event)),
            Map.entry(EventTypes.INVENTORY_CONFIRMED, event -> InventoryReservationEventPayload.from(event)),
            Map.entry(EventTypes.INVENTORY_RELEASED, event -> InventoryReservationEventPayload.from(event)),
            Map.entry(EventTypes.PAYMENT_SUCCEEDED, event -> PaymentEventPayload.from(event)),
            Map.entry(EventTypes.PAYMENT_REFUNDED, event -> PaymentEventPayload.from(event)),
            Map.entry(EventTypes.PRODUCT_CHANGED, event -> ProductChangedEventPayload.from(event)),
            Map.entry(EventTypes.FLASH_SALE_ORDER_QUEUED, event -> FlashSaleOrderQueuedEventPayload.from(event)),
            Map.entry(EventTypes.ACCOUNT_REGISTERED, event -> AccountLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.ACCOUNT_ACTIVATED, event -> AccountLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.ACCOUNT_SUSPENDED, event -> AccountLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.ACCOUNT_RESTORED, event -> AccountLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.ACCOUNT_CLOSED, event -> AccountLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.ACCOUNT_DELETION_REQUESTED, event -> AccountLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.ACCOUNT_DELETED, event -> AccountLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.ACCOUNT_RECONCILIATION_REQUESTED, event -> AccountLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.USER_PROFILE_READY, event -> UserProfileLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.USER_PROFILE_DELETION_COMPLETED,
                    event -> UserProfileLifecycleEventPayload.from(event)),
            Map.entry(EventTypes.USER_PROFILE_RECONCILED, event -> UserProfileLifecycleEventPayload.from(event)));

    private EventContractRegistry() {
    }

    public static void validate(OutboxEvent event) {
        try {
            validateContract(event);
        } catch (EventContractException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new EventContractException(exception.getMessage(), exception);
        }
    }

    private static void validateContract(OutboxEvent event) {
        if (event.eventId() == null || event.eventId().isBlank() || event.aggregateType() == null
                || event.aggregateType().isBlank() || event.aggregateId() == null || event.aggregateId().isBlank()
                || event.aggregateVersion() < 0 || event.occurredAt() == null || event.producer() == null
                || event.producer().isBlank() || event.producerVersion() == null || event.producerVersion().isBlank()) {
            throw new EventContractException("event envelope metadata is incomplete");
        }
        if (event.aggregateVersion() == 0 && !event.legacyContract()) {
            throw new EventContractException("new event has no aggregate version");
        }
        Consumer<OutboxEvent> validator = VALIDATORS.get(event.eventType());
        if (validator == null) {
            throw new EventContractException("event type is not registered: " + event.eventType());
        }
        validator.accept(event);
    }

    public static Map<String, Integer> currentSchemaVersions() {
        return Map.ofEntries(Map.entry(EventTypes.ORDER_CREATED, OrderEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ORDER_PAID, OrderEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ORDER_CANCELLED, OrderEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.INVENTORY_RESERVED, InventoryReservationEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.INVENTORY_CONFIRMED, InventoryReservationEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.INVENTORY_RELEASED, InventoryReservationEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.PAYMENT_SUCCEEDED, PaymentEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.PAYMENT_REFUNDED, PaymentEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.PRODUCT_CHANGED, ProductChangedEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.FLASH_SALE_ORDER_QUEUED, FlashSaleOrderQueuedEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ACCOUNT_REGISTERED, AccountLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ACCOUNT_ACTIVATED, AccountLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ACCOUNT_SUSPENDED, AccountLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ACCOUNT_RESTORED, AccountLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ACCOUNT_CLOSED, AccountLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ACCOUNT_DELETION_REQUESTED, AccountLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ACCOUNT_DELETED, AccountLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.ACCOUNT_RECONCILIATION_REQUESTED,
                        AccountLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.USER_PROFILE_READY, UserProfileLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.USER_PROFILE_DELETION_COMPLETED,
                        UserProfileLifecycleEventPayload.CURRENT_SCHEMA_VERSION),
                Map.entry(EventTypes.USER_PROFILE_RECONCILED, UserProfileLifecycleEventPayload.CURRENT_SCHEMA_VERSION));
    }
}
