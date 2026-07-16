package com.emall.search.repository;

import java.time.Instant;
import java.util.List;

record SearchCursorState(String pitId, List<Object> sortValues, String queryFingerprint, long seen, Instant expiresAt) {
    SearchCursorState {
        sortValues = List.copyOf(sortValues);
    }
}
