package com.emall.governance.region;

public record RegionEndpoint(String regionCode, String serviceUrl, RegionStatus status, int priority) {
    public boolean canServeRead() {
        return status == RegionStatus.ACTIVE || status == RegionStatus.DEGRADED;
    }

    public boolean canServeWrite() {
        return status == RegionStatus.ACTIVE;
    }
}
