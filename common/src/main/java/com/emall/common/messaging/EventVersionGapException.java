package com.emall.common.messaging;

public class EventVersionGapException extends RuntimeException {
    private final long currentVersion;
    private final long requestedVersion;

    public EventVersionGapException(String claimId, long currentVersion, long requestedVersion) {
        super("aggregate event version gap for " + claimId + ": current=" + currentVersion + ", requested="
                + requestedVersion);
        this.currentVersion = currentVersion;
        this.requestedVersion = requestedVersion;
    }

    public long currentVersion() {
        return currentVersion;
    }

    public long requestedVersion() {
        return requestedVersion;
    }
}
