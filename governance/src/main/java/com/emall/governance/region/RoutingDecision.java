package com.emall.governance.region;

public record RoutingDecision(
        DomainType domain,
        TrafficIntent intent,
        String ownerRegion,
        String targetRegion,
        String serviceUrl,
        boolean crossRegion,
        String reason
) {
}
