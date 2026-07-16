package com.emall.common.region;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.sharding.ShardAccessMode;
import com.emall.common.sharding.ShardRoutingProperties;
import com.emall.common.sharding.VirtualShardPlacement;
import com.emall.common.sharding.VirtualShardPlacementProvider;
import java.util.List;
import java.util.Objects;

public class OwnershipGuard {
    private final OwnershipProperties properties;
    private final ShardRoutingProperties shardRoutingProperties;
    private final VirtualShardPlacementProvider placementProvider;

    public OwnershipGuard(OwnershipProperties properties) {
        this(properties, null, null);
    }

    public OwnershipGuard(OwnershipProperties properties, ShardRoutingProperties shardRoutingProperties,
            VirtualShardPlacementProvider placementProvider) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.shardRoutingProperties = shardRoutingProperties;
        this.placementProvider = placementProvider;
    }

    public OwnershipDecision checkWrite(String domain, long partitionKey) {
        OwnershipDecision decision = decide(domain, partitionKey);
        if (!decision.accepted()) {
            throw new BusinessException(ErrorCode.CONFLICT, decision.reason());
        }
        return decision;
    }

    public OwnershipDecision decide(String domain, long partitionKey) {
        if (!properties.isEnabled()) {
            return accepted(domain, partitionKey, properties.getCurrentRegion(), properties.getCurrentRegion(),
                    properties.getCurrentCell(), properties.getCurrentCell(), RegionWriteStatus.ACTIVE);
        }
        Owner owner = resolveOwner(domain, partitionKey);
        String ownerRegion = owner.region();
        String ownerCell = owner.cell();
        RegionWriteStatus currentStatus =
                properties.getRegionStatuses().getOrDefault(properties.getCurrentRegion(), RegionWriteStatus.ACTIVE);
        if (currentStatus != RegionWriteStatus.ACTIVE) {
            return rejected(domain, partitionKey, ownerRegion, ownerCell, currentStatus,
                    "current region is not writable: " + currentStatus);
        }
        if (!properties.getCurrentRegion().equals(ownerRegion)) {
            return rejected(domain, partitionKey, ownerRegion, ownerCell, currentStatus,
                    "write rejected by region owner, ownerRegion=" + ownerRegion);
        }
        if (!properties.getCurrentCell().equals(ownerCell)) {
            return rejected(domain, partitionKey, ownerRegion, ownerCell, currentStatus,
                    "write rejected by cell owner, ownerCell=" + ownerCell);
        }
        return accepted(domain, partitionKey, properties.getCurrentRegion(), ownerRegion, properties.getCurrentCell(),
                ownerCell, currentStatus);
    }

    private Owner resolveOwner(String domain, long partitionKey) {
        if (shardRoutingProperties != null && shardRoutingProperties.isEnabled() && placementProvider != null) {
            int virtualShard = Math.floorMod(partitionKey, shardRoutingProperties.getVirtualShardCount());
            VirtualShardPlacement placement = placementProvider.resolve(shardRoutingProperties.mappingNamespace(),
                    virtualShard, ShardAccessMode.WRITE);
            return new Owner(placement.primary().regionId(), placement.primary().cellId());
        }
        OwnershipProperties.DomainOwnership domainOwnership = properties.domain(domain);
        return new Owner(ownerRegion(domainOwnership, partitionKey), ownerCell(domainOwnership, partitionKey));
    }

    private OwnershipDecision accepted(String domain, long partitionKey, String currentRegion, String ownerRegion,
            String currentCell, String ownerCell, RegionWriteStatus status) {
        return new OwnershipDecision(domain, partitionKey, currentRegion, ownerRegion, currentCell, ownerCell, true,
                status, "owner accepted");
    }

    private OwnershipDecision rejected(String domain, long partitionKey, String ownerRegion, String ownerCell,
            RegionWriteStatus status, String reason) {
        return new OwnershipDecision(domain, partitionKey, properties.getCurrentRegion(), ownerRegion,
                properties.getCurrentCell(), ownerCell, false, status, reason);
    }

    private String ownerRegion(OwnershipProperties.DomainOwnership domainOwnership, long partitionKey) {
        if (domainOwnership.getStrategy() == WriteOwnershipStrategy.GLOBAL_SINGLE_WRITER) {
            return domainOwnership.getPrimaryRegion();
        }
        return pick(domainOwnership.getOwnerRegions(), partitionKey, domainOwnership.getPrimaryRegion());
    }

    private String ownerCell(OwnershipProperties.DomainOwnership domainOwnership, long partitionKey) {
        return pick(domainOwnership.getOwnerCells(), partitionKey, properties.getCurrentCell());
    }

    private String pick(List<String> values, long partitionKey, String fallback) {
        List<String> normalized = values == null
                ? List.of()
                : values.stream().filter(value -> value != null && !value.isBlank()).toList();
        if (normalized.isEmpty()) {
            return fallback;
        }
        int index = Math.floorMod(Long.hashCode(partitionKey), normalized.size());
        return normalized.get(index);
    }

    public static OwnershipGuard noop() {
        OwnershipProperties noop = new OwnershipProperties();
        noop.setEnabled(false);
        return new OwnershipGuard(noop);
    }

    private record Owner(String region, String cell) {
    }
}
