package com.emall.common.event;

import java.util.Map;

public interface VersionedEventPayload {
    int schemaVersion();

    Map<String, Object> toMap();
}
