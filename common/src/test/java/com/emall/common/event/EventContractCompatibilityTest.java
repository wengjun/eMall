package com.emall.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.stream.Stream;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class EventContractCompatibilityTest {
    private static final String CONTRACT_ROOT = "contracts/events/";
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldPackageSchemaForEveryRegisteredEventType() throws Exception {
        Map<String, String> schemas = Map.ofEntries(Map.entry(EventTypes.ORDER_CREATED, "order-event-v1.schema.json"),
                Map.entry(EventTypes.ORDER_PAID, "order-event-v1.schema.json"),
                Map.entry(EventTypes.ORDER_CANCELLED, "order-event-v1.schema.json"),
                Map.entry(EventTypes.INVENTORY_RESERVED, "inventory-reservation-v1.schema.json"),
                Map.entry(EventTypes.INVENTORY_CONFIRMED, "inventory-reservation-v1.schema.json"),
                Map.entry(EventTypes.INVENTORY_RELEASED, "inventory-reservation-v1.schema.json"),
                Map.entry(EventTypes.PAYMENT_SUCCEEDED, "payment-v1.schema.json"),
                Map.entry(EventTypes.PAYMENT_REFUNDED, "payment-v1.schema.json"),
                Map.entry(EventTypes.PRODUCT_CHANGED, "product-changed-v2.schema.json"),
                Map.entry(EventTypes.FLASH_SALE_ORDER_QUEUED, "flash-sale-order-queued-v1.schema.json"),
                Map.entry(EventTypes.ACCOUNT_REGISTERED, "account-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.ACCOUNT_ACTIVATED, "account-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.ACCOUNT_SUSPENDED, "account-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.ACCOUNT_RESTORED, "account-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.ACCOUNT_CLOSED, "account-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.ACCOUNT_DELETION_REQUESTED, "account-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.ACCOUNT_DELETED, "account-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.ACCOUNT_RECONCILIATION_REQUESTED, "account-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.USER_PROFILE_READY, "user-profile-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.USER_PROFILE_DELETION_COMPLETED, "user-profile-lifecycle-v1.schema.json"),
                Map.entry(EventTypes.USER_PROFILE_RECONCILED, "user-profile-lifecycle-v1.schema.json"));

        assertThat(schemas.keySet())
                .containsExactlyInAnyOrderElementsOf(EventContractRegistry.currentSchemaVersions().keySet());
        for (String schema : schemas.values()) {
            assertThat(readSchema(schema).path("type").asText()).isEqualTo("object");
        }
        assertThat(readSchema("envelope-v1.schema.json").path("required")).hasSize(10);
    }

    @Test
    void productV2MustRemainBackwardCompatibleWithV1() throws Exception {
        JsonNode versionOne = readSchema("product-changed-v1.schema.json");
        JsonNode versionTwo = readSchema("product-changed-v2.schema.json");
        Set<String> versionOneRequired = textValues(versionOne.path("required"));
        Set<String> versionTwoRequired = textValues(versionTwo.path("required"));
        Set<String> versionOneProperties = fieldNames(versionOne.path("properties"));
        Set<String> versionTwoProperties = fieldNames(versionTwo.path("properties"));

        assertThat(versionOneRequired).containsAll(versionTwoRequired);
        assertThat(versionTwoProperties).containsAll(versionOneProperties);
        for (String property : versionOneProperties) {
            assertThat(versionTwo.path("properties").path(property).path("type"))
                    .isEqualTo(versionOne.path("properties").path(property).path("type"));
        }
    }

    @Test
    void shouldUpcastProductV1AndRejectUnsupportedOrMalformedContracts() {
        Map<String, Object> payload = Map.of("skuId", 30001L, "title", "Phone", "category", "mobile", "price",
                new BigDecimal("3999.00"), "saleable", true);
        OutboxEvent versionOne = event(1, 8, "legacy-product", payload);

        assertThatCode(() -> EventContractRegistry.validate(versionOne)).doesNotThrowAnyException();
        assertThat(ProductChangedEventPayload.from(versionOne).status()).isEqualTo("UNKNOWN");
        assertThatThrownBy(() -> EventContractRegistry.validate(event(3, 9, "product", payload)))
                .isInstanceOf(EventContractException.class).hasMessageContaining("unsupported");
        assertThatThrownBy(() -> EventContractRegistry.validate(
                event(2, 9, "product", Map.of("skuId", 30001L, "title", "Phone", "category", "mobile", "price", 3999))))
                .isInstanceOf(EventContractException.class).hasMessageContaining("saleable");
    }

    @Test
    void shouldAllowOnlyLegacyEventsWithoutAggregateVersion() {
        Map<String, Object> payload = Map.of("orderId", 70001L);

        assertThatCode(() -> EventContractRegistry.validate(event(1, 0, "legacy", payload, EventTypes.ORDER_CREATED)))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                () -> EventContractRegistry.validate(event(1, 0, "order", payload, EventTypes.ORDER_CREATED)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no aggregate version");
    }

    @Test
    void shouldRejectLossyNumericAndBooleanConversions() {
        Map<String, Object> fractionalId =
                Map.of("skuId", 1.5, "title", "Phone", "category", "mobile", "price", 1, "saleable", true);
        Map<String, Object> invalidBoolean =
                Map.of("skuId", 1, "title", "Phone", "category", "mobile", "price", 1, "saleable", "yes");

        assertThatThrownBy(() -> ProductChangedEventPayload.from(event(2, 1, "product", fractionalId)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("skuId");
        assertThatThrownBy(() -> ProductChangedEventPayload.from(event(2, 1, "product", invalidBoolean)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("saleable");
    }

    @TestFactory
    Stream<DynamicTest> everyCoreEventMustRoundTripThroughItsRegisteredContract() {
        return coreEvents().stream().map(event -> dynamicTest(event.eventType(), () -> {
            OutboxEvent decoded = objectMapper.readValue(objectMapper.writeValueAsString(event), OutboxEvent.class);

            assertThatCode(() -> EventContractRegistry.validate(decoded)).doesNotThrowAnyException();
            assertThat(decoded.schemaVersion())
                    .isEqualTo(EventContractRegistry.currentSchemaVersions().get(decoded.eventType()));
            assertThat(decoded.aggregateVersion()).isOne();
        }));
    }

    private List<OutboxEvent> coreEvents() {
        OrderEventPayload order =
                new OrderEventPayload(70001L, 10001L, 30001L, 1, "WEB", "device-1", "direct", BigDecimal.TEN,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, "CNY", 1L, "", "reservation-1", "CREATED");
        InventoryReservationEventPayload inventory =
                new InventoryReservationEventPayload("reservation-1", 30001L, 1, 0, "RESERVED");
        PaymentEventPayload payment = new PaymentEventPayload(80001L, 70001L, 10001L, BigDecimal.TEN, "SUCCEEDED");
        ProductChangedEventPayload product = new ProductChangedEventPayload(30001L, 3000L, "Phone", "mobile",
                BigDecimal.TEN, "ON_SALE", true, Instant.parse("2026-07-15T00:00:00Z"));
        FlashSaleOrderQueuedEventPayload flashSale =
                new FlashSaleOrderQueuedEventPayload(90001L, 60001L, 10001L, 30001L, 1, "QUEUED");
        AccountLifecycleEventPayload account = new AccountLifecycleEventPayload(10001L, "13800000000", "Alice",
                "binding-hash", "ACTIVE", "contract-test");
        UserProfileLifecycleEventPayload profile =
                new UserProfileLifecycleEventPayload(10001L, "binding-hash", "NORMAL", 1L);
        return List.of(typedEvent(EventTypes.ORDER_CREATED, order), typedEvent(EventTypes.ORDER_PAID, order),
                typedEvent(EventTypes.ORDER_CANCELLED, order), typedEvent(EventTypes.INVENTORY_RESERVED, inventory),
                typedEvent(EventTypes.INVENTORY_CONFIRMED, inventory),
                typedEvent(EventTypes.INVENTORY_RELEASED, inventory), typedEvent(EventTypes.PAYMENT_SUCCEEDED, payment),
                typedEvent(EventTypes.PAYMENT_REFUNDED, payment), typedEvent(EventTypes.PRODUCT_CHANGED, product),
                typedEvent(EventTypes.FLASH_SALE_ORDER_QUEUED, flashSale),
                typedEvent(EventTypes.ACCOUNT_REGISTERED, account), typedEvent(EventTypes.ACCOUNT_ACTIVATED, account),
                typedEvent(EventTypes.ACCOUNT_SUSPENDED, account), typedEvent(EventTypes.ACCOUNT_RESTORED, account),
                typedEvent(EventTypes.ACCOUNT_CLOSED, account),
                typedEvent(EventTypes.ACCOUNT_DELETION_REQUESTED, account),
                typedEvent(EventTypes.ACCOUNT_DELETED, account),
                typedEvent(EventTypes.ACCOUNT_RECONCILIATION_REQUESTED, account),
                typedEvent(EventTypes.USER_PROFILE_READY, profile),
                typedEvent(EventTypes.USER_PROFILE_DELETION_COMPLETED, profile),
                typedEvent(EventTypes.USER_PROFILE_RECONCILED, profile));
    }

    private OutboxEvent typedEvent(String eventType, VersionedEventPayload payload) {
        return OutboxEvent
                .create("event-" + eventType, "Aggregate", "aggregate-1", eventType, "contract-test", "1.0.0", payload)
                .withAggregateVersion(1);
    }

    private OutboxEvent event(int schemaVersion, long aggregateVersion, String producer, Map<String, Object> payload) {
        return event(schemaVersion, aggregateVersion, producer, payload, EventTypes.PRODUCT_CHANGED);
    }

    private OutboxEvent event(int schemaVersion, long aggregateVersion, String producer, Map<String, Object> payload,
            String eventType) {
        Instant now = Instant.parse("2026-07-15T00:00:00Z");
        return new OutboxEvent("event-1", "Product", "30001", eventType, schemaVersion, aggregateVersion, producer,
                "1.0.0", now, "trace-1", "correlation-1", payload, OutboxStatus.NEW, 0, now, now, now, 1, null, null,
                null, null, null);
    }

    private JsonNode readSchema(String name) throws Exception {
        try (var input =
                EventContractCompatibilityTest.class.getClassLoader().getResourceAsStream(CONTRACT_ROOT + name)) {
            assertThat(input).as("packaged event schema %s", name).isNotNull();
            return objectMapper.readTree(input);
        }
    }

    private Set<String> textValues(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).collect(Collectors.toSet());
    }

    private Set<String> fieldNames(JsonNode object) {
        Iterator<String> names = object.fieldNames();
        return StreamSupport.stream(((Iterable<String>) () -> names).spliterator(), false).collect(Collectors.toSet());
    }
}
