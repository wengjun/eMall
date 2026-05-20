package com.emall.governance.region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class MultiRegionRoutingPolicyTest {
    @Test
    void routesPartitionedWritesToStableOwnerRegion() {
        MultiRegionRoutingPolicy policy =
                MultiRegionRoutingPolicies.baseline("http://east.internal", "http://west.internal");

        RoutingDecision first = policy.decide(DomainType.ORDER, TrafficIntent.WRITE, "us-east-1", 10001L);
        RoutingDecision second = policy.decide(DomainType.ORDER, TrafficIntent.WRITE, "us-west-2", 10001L);

        assertThat(first.ownerRegion()).isEqualTo(second.ownerRegion());
        assertThat(first.targetRegion()).isEqualTo(second.targetRegion());
        assertThat(first.reason()).isEqualTo("owner region");
    }

    @Test
    void servesReadsLocallyWhenRegionIsReadable() {
        MultiRegionRoutingPolicy policy =
                MultiRegionRoutingPolicies.baseline("http://east.internal", "http://west.internal");

        RoutingDecision decision = policy.decide(DomainType.ORDER, TrafficIntent.READ, "us-west-2", 10001L);

        assertThat(decision.targetRegion()).isEqualTo("us-west-2");
        assertThat(decision.crossRegion()).isFalse();
        assertThat(decision.reason()).isEqualTo("local read");
    }

    @Test
    void failsOverGlobalSingleWriterWhenPrimaryIsOffline() {
        MultiRegionRoutingPolicy policy = new MultiRegionRoutingPolicy(
                List.of(new DomainOwnershipRule(DomainType.PAYMENT, WriteStrategy.GLOBAL_SINGLE_WRITER, "us-east-1", 1,
                        List.of(new RegionEndpoint("us-east-1", "http://east/payment", RegionStatus.OFFLINE, 10),
                                new RegionEndpoint("us-west-2", "http://west/payment", RegionStatus.ACTIVE, 20)))));

        RoutingDecision decision = policy.decide(DomainType.PAYMENT, TrafficIntent.WRITE, "us-east-1", 10001L);

        assertThat(decision.ownerRegion()).isEqualTo("us-east-1");
        assertThat(decision.targetRegion()).isEqualTo("us-west-2");
        assertThat(decision.reason()).isEqualTo("failover region");
    }

    @Test
    void rejectsPartitionedWritesWhenOwnerRegionIsUnavailable() {
        MultiRegionRoutingPolicy policy = new MultiRegionRoutingPolicy(
                List.of(new DomainOwnershipRule(DomainType.ORDER, WriteStrategy.PARTITIONED_SINGLE_WRITER, "us-east-1",
                        1, List.of(new RegionEndpoint("us-east-1", "http://east/order", RegionStatus.OFFLINE, 10),
                                new RegionEndpoint("us-west-2", "http://west/order", RegionStatus.ACTIVE, 20)))));

        assertThatThrownBy(() -> policy.decide(DomainType.ORDER, TrafficIntent.WRITE, "us-west-2", 10001L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("owner region is unavailable");
    }

    @Test
    void treatsReadOnlyRegionAsReadableButNotWritable() {
        MultiRegionRoutingPolicy policy = new MultiRegionRoutingPolicy(
                List.of(new DomainOwnershipRule(DomainType.PRODUCT, WriteStrategy.GLOBAL_SINGLE_WRITER, "us-east-1", 1,
                        List.of(new RegionEndpoint("us-east-1", "http://east/product", RegionStatus.READ_ONLY, 10),
                                new RegionEndpoint("us-west-2", "http://west/product", RegionStatus.ACTIVE, 20)))));

        RoutingDecision read = policy.decide(DomainType.PRODUCT, TrafficIntent.READ, "us-east-1", 10001L);
        RoutingDecision write = policy.decide(DomainType.PRODUCT, TrafficIntent.WRITE, "us-east-1", 10001L);

        assertThat(read.targetRegion()).isEqualTo("us-east-1");
        assertThat(read.reason()).isEqualTo("local read");
        assertThat(write.targetRegion()).isEqualTo("us-west-2");
        assertThat(write.reason()).isEqualTo("failover region");
    }

    @Test
    void rejectsFailedRegionForReadsAndWrites() {
        MultiRegionRoutingPolicy policy = new MultiRegionRoutingPolicy(
                List.of(new DomainOwnershipRule(DomainType.PAYMENT, WriteStrategy.GLOBAL_SINGLE_WRITER, "us-east-1", 1,
                        List.of(new RegionEndpoint("us-east-1", "http://east/payment", RegionStatus.FAILED, 10),
                                new RegionEndpoint("us-west-2", "http://west/payment", RegionStatus.ACTIVE, 20)))));

        RoutingDecision read = policy.decide(DomainType.PAYMENT, TrafficIntent.READ, "us-east-1", 10001L);
        RoutingDecision write = policy.decide(DomainType.PAYMENT, TrafficIntent.WRITE, "us-east-1", 10001L);

        assertThat(read.targetRegion()).isEqualTo("us-west-2");
        assertThat(read.reason()).isEqualTo("failover region");
        assertThat(write.targetRegion()).isEqualTo("us-west-2");
        assertThat(write.reason()).isEqualTo("failover region");
    }
}
