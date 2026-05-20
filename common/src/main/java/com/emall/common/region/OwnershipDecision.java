package com.emall.common.region;

public record OwnershipDecision(String domain, long partitionKey, String currentRegion, String ownerRegion,
        String currentCell, String ownerCell, boolean accepted, RegionWriteStatus status, String reason) {
}
