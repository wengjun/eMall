package com.emall.governance.region;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MultiRegionRoutingPolicy {
    private final Map<DomainType, DomainOwnershipRule> rules;

    public MultiRegionRoutingPolicy(List<DomainOwnershipRule> rules) {
        EnumMap<DomainType, DomainOwnershipRule> indexedRules = new EnumMap<>(DomainType.class);
        for (DomainOwnershipRule rule : rules) {
            indexedRules.put(rule.domain(), rule);
        }
        this.rules = Map.copyOf(indexedRules);
    }

    public RoutingDecision decide(DomainType domain, TrafficIntent intent, String callerRegion, long partitionKey) {
        DomainOwnershipRule rule = rule(domain);
        String ownerRegion = ownerRegion(rule, partitionKey);
        RegionEndpoint target = intent == TrafficIntent.WRITE
                ? writeEndpoint(rule, ownerRegion)
                : readEndpoint(rule, callerRegion, ownerRegion);
        boolean crossRegion = !target.regionCode().equals(callerRegion);
        return new RoutingDecision(domain, intent, ownerRegion, target.regionCode(), target.serviceUrl(), crossRegion,
                reason(intent, target.regionCode(), ownerRegion, callerRegion));
    }

    private DomainOwnershipRule rule(DomainType domain) {
        DomainOwnershipRule rule = rules.get(domain);
        if (rule == null) {
            throw new IllegalArgumentException("missing multi-region ownership rule for " + domain);
        }
        return rule;
    }

    private String ownerRegion(DomainOwnershipRule rule, long partitionKey) {
        if (rule.writeStrategy() == WriteStrategy.GLOBAL_SINGLE_WRITER) {
            return rule.primaryRegion();
        }
        if (rule.writeStrategy() == WriteStrategy.ACTIVE_ACTIVE_READ_LOCAL) {
            return rule.primaryRegion();
        }
        int partition = Math.floorMod(Long.hashCode(partitionKey), rule.partitionCount());
        List<RegionEndpoint> ownershipEndpoints = ownershipEndpoints(rule);
        return ownershipEndpoints.get(partition % ownershipEndpoints.size()).regionCode();
    }

    private RegionEndpoint writeEndpoint(DomainOwnershipRule rule, String ownerRegion) {
        return rule.endpoints().stream().filter(endpoint -> endpoint.regionCode().equals(ownerRegion))
                .filter(RegionEndpoint::canServeWrite).findFirst()
                .orElseGet(() -> failoverWriteEndpoint(rule, ownerRegion));
    }

    private RegionEndpoint failoverWriteEndpoint(DomainOwnershipRule rule, String ownerRegion) {
        if (rule.writeStrategy() != WriteStrategy.GLOBAL_SINGLE_WRITER) {
            throw new IllegalStateException("owner region is unavailable for " + rule.domain());
        }
        return activeEndpoints(rule).stream().min(Comparator.comparingInt(RegionEndpoint::priority))
                .orElseThrow(() -> new IllegalStateException("no active region available for " + rule.domain()));
    }

    private RegionEndpoint readEndpoint(DomainOwnershipRule rule, String callerRegion, String ownerRegion) {
        Optional<RegionEndpoint> local =
                rule.endpoints().stream().filter(endpoint -> endpoint.regionCode().equals(callerRegion))
                        .filter(RegionEndpoint::canServeRead).findFirst();
        if (local.isPresent()) {
            return local.get();
        }
        return rule.endpoints().stream().filter(endpoint -> endpoint.regionCode().equals(ownerRegion))
                .filter(RegionEndpoint::canServeRead).findFirst()
                .orElseGet(() -> activeEndpoints(rule).stream().min(Comparator.comparingInt(RegionEndpoint::priority))
                        .orElseThrow(() -> new IllegalStateException("no readable region available")));
    }

    private List<RegionEndpoint> activeEndpoints(DomainOwnershipRule rule) {
        return rule.endpoints().stream().filter(RegionEndpoint::canServeWrite)
                .sorted(Comparator.comparingInt(RegionEndpoint::priority).thenComparing(RegionEndpoint::regionCode))
                .toList();
    }

    private List<RegionEndpoint> ownershipEndpoints(DomainOwnershipRule rule) {
        return rule.endpoints().stream()
                .sorted(Comparator.comparingInt(RegionEndpoint::priority).thenComparing(RegionEndpoint::regionCode))
                .toList();
    }

    private String reason(TrafficIntent intent, String targetRegion, String ownerRegion, String callerRegion) {
        if (intent == TrafficIntent.READ && targetRegion.equals(callerRegion)) {
            return "local read";
        }
        if (targetRegion.equals(ownerRegion)) {
            return "owner region";
        }
        return "failover region";
    }
}
