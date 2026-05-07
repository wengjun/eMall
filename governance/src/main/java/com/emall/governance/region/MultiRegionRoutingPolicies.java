package com.emall.governance.region;

import java.util.List;

public final class MultiRegionRoutingPolicies {
    private MultiRegionRoutingPolicies() {
    }

    public static MultiRegionRoutingPolicy baseline(String eastBaseUrl, String westBaseUrl) {
        List<RegionEndpoint> orderEndpoints = endpoints(eastBaseUrl + "/order", westBaseUrl + "/order");
        List<RegionEndpoint> inventoryEndpoints = endpoints(eastBaseUrl + "/inventory", westBaseUrl + "/inventory");
        List<RegionEndpoint> paymentEndpoints = endpoints(eastBaseUrl + "/payment", westBaseUrl + "/payment");
        return new MultiRegionRoutingPolicy(List.of(
                new DomainOwnershipRule(DomainType.ORDER, WriteStrategy.PARTITIONED_SINGLE_WRITER, "us-east-1", 1024,
                        orderEndpoints),
                new DomainOwnershipRule(DomainType.INVENTORY, WriteStrategy.PARTITIONED_SINGLE_WRITER, "us-east-1",
                        4096, inventoryEndpoints),
                new DomainOwnershipRule(DomainType.PAYMENT, WriteStrategy.GLOBAL_SINGLE_WRITER, "us-east-1", 1,
                        paymentEndpoints)));
    }

    private static List<RegionEndpoint> endpoints(String eastUrl, String westUrl) {
        return List.of(new RegionEndpoint("us-east-1", eastUrl, RegionStatus.ACTIVE, 10),
                new RegionEndpoint("us-west-2", westUrl, RegionStatus.ACTIVE, 20));
    }
}
