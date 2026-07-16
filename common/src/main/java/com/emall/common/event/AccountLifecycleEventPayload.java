package com.emall.common.event;

import java.util.LinkedHashMap;
import java.util.Map;

public record AccountLifecycleEventPayload(long accountId, String subject, String displayName, String bindingHash,
        String status, String reason) implements VersionedEventPayload {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @Override
    public int schemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("accountId", accountId);
        values.put("subject", text(subject));
        values.put("displayName", text(displayName));
        values.put("bindingHash", text(bindingHash));
        values.put("status", status);
        values.put("reason", reason);
        return Map.copyOf(values);
    }

    public static AccountLifecycleEventPayload from(OutboxEvent event) {
        EventPayloadValues.requireSupported(event, 1, CURRENT_SCHEMA_VERSION);
        Map<String, Object> payload = event.payload();
        return new AccountLifecycleEventPayload(EventPayloadValues.requiredLong(payload, "accountId"),
                EventPayloadValues.optionalString(payload, "subject", ""),
                EventPayloadValues.optionalString(payload, "displayName", ""),
                EventPayloadValues.optionalString(payload, "bindingHash", ""),
                EventPayloadValues.requiredString(payload, "status"),
                EventPayloadValues.optionalString(payload, "reason", "unspecified"));
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
