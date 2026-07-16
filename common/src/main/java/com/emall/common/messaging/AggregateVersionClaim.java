package com.emall.common.messaging;

public record AggregateVersionClaim(String claimId, String eventId, long previousVersion, long claimedVersion,
        boolean inserted, boolean accepted) {
    public static AggregateVersionClaim rejected(String claimId, String eventId, long currentVersion,
            long requestedVersion) {
        return new AggregateVersionClaim(claimId, eventId, currentVersion, requestedVersion, false, false);
    }
}
