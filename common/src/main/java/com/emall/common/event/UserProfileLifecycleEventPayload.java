package com.emall.common.event;

import java.util.Map;

public record UserProfileLifecycleEventPayload(long accountId, String bindingHash, String status,
        long identityEventVersion) implements VersionedEventPayload {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @Override
    public int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    @Override
    public Map<String, Object> toMap() {
        return Map.of("accountId", accountId, "bindingHash", bindingHash, "status", status, "identityEventVersion",
                identityEventVersion);
    }

    public static UserProfileLifecycleEventPayload from(OutboxEvent event) {
        EventPayloadValues.requireSupported(event, 1, CURRENT_SCHEMA_VERSION);
        Map<String, Object> payload = event.payload();
        return new UserProfileLifecycleEventPayload(EventPayloadValues.requiredLong(payload, "accountId"),
                EventPayloadValues.optionalString(payload, "bindingHash", ""),
                EventPayloadValues.requiredString(payload, "status"),
                EventPayloadValues.requiredLong(payload, "identityEventVersion"));
    }
}
