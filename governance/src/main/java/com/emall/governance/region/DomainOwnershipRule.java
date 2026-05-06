package com.emall.governance.region;

import java.util.List;

public record DomainOwnershipRule(
        DomainType domain,
        WriteStrategy writeStrategy,
        String primaryRegion,
        int partitionCount,
        List<RegionEndpoint> endpoints
) {
    public DomainOwnershipRule {
        endpoints = List.copyOf(endpoints);
        if (partitionCount <= 0) {
            throw new IllegalArgumentException("partitionCount must be positive");
        }
    }
}
