package com.emall.traffic;

import com.emall.common.controlplane.ControlPlaneClient;
import com.emall.common.controlplane.ControlPlaneCommands;
import com.emall.common.controlplane.ControlPlaneIdempotencyKeys;
import com.emall.common.controlplane.ControlPlaneProperties;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TrafficControlPlanePublisher {
    private static final String MODULE = "traffic";

    private final ControlPlaneClient client;
    private final ControlPlaneProperties properties;

    TrafficControlPlanePublisher(ControlPlaneClient client, ControlPlaneProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    void publishRouting(List<UnitCell> units, List<ShardRoute> routes, List<TrafficShift> shifts) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("schemaVersion", 1);
        content.put("units", units.stream().sorted(Comparator.comparing(UnitCell::unitCode)).map(this::unit).toList());
        content.put("shardRoutes",
                routes.stream()
                        .sorted(Comparator.comparing(ShardRoute::domainName).thenComparingInt(ShardRoute::shardNo))
                        .map(this::route).toList());
        content.put("trafficShifts",
                shifts.stream().sorted(Comparator.comparingLong(TrafficShift::shiftId)).map(this::shift).toList());
        String key = ControlPlaneIdempotencyKeys.currentOrDeterministic(MODULE, "sync-routing", revision(content));
        ControlPlaneProperties.Nacos nacos = properties.getNacos();
        client.submit(ControlPlaneCommands.nacosConfig(key, MODULE, "sync-routing", "routing-directory", "multi-region",
                "emall-multi-region-routing.json", nacos.getGroup(), nacos.getNamespace(), content));
        String infrastructureKey = ControlPlaneIdempotencyKeys.currentOrDeterministic(MODULE,
                "reconcile-global-routing", revision(content));
        client.submit(ControlPlaneCommands.infrastructure(infrastructureKey, MODULE, "reconcile-global-routing",
                "global-traffic-policy", "multi-region", content));
    }

    void publishControlRules(List<TrafficControlRule> rules) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("schemaVersion", 1);
        content.put("rules",
                rules.stream().sorted(Comparator.comparingLong(TrafficControlRule::ruleId)).map(this::rule).toList());
        String key =
                ControlPlaneIdempotencyKeys.currentOrDeterministic(MODULE, "sync-sentinel-rules", revision(content));
        ControlPlaneProperties.Nacos nacos = properties.getNacos();
        client.submit(ControlPlaneCommands.nacosConfig(key, MODULE, "sync-sentinel-rules", "sentinel-rules", "global",
                "emall-sentinel-rules.json", nacos.getGroup(), nacos.getNamespace(), content));
        String infrastructureKey = ControlPlaneIdempotencyKeys.currentOrDeterministic(MODULE,
                "reconcile-sentinel-rules", revision(content));
        client.submit(ControlPlaneCommands.infrastructure(infrastructureKey, MODULE, "reconcile-sentinel-rules",
                "sentinel-rules", "global", content));
    }

    private Map<String, Object> unit(UnitCell unit) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("unitCode", unit.unitCode());
        value.put("regionCode", unit.regionCode());
        value.put("capacityWeight", unit.capacityWeight());
        value.put("status", unit.status().name());
        return value;
    }

    private Map<String, Object> route(ShardRoute route) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("domainName", route.domainName());
        value.put("shardNo", route.shardNo());
        value.put("unitCode", route.unitCode());
        value.put("databaseKey", route.databaseKey());
        return value;
    }

    private Map<String, Object> shift(TrafficShift shift) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("shiftId", shift.shiftId());
        value.put("sourceUnit", shift.sourceUnit());
        value.put("targetUnit", shift.targetUnit());
        value.put("percent", shift.percent());
        value.put("status", shift.status().name());
        value.put("reason", shift.reason());
        return value;
    }

    private Map<String, Object> rule(TrafficControlRule rule) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("ruleId", rule.ruleId());
        value.put("resource", rule.resource());
        value.put("type", rule.type().name());
        value.put("dimension", rule.dimension());
        value.put("matchValue", rule.matchValue());
        value.put("threshold", rule.threshold());
        value.put("unitCode", rule.unitCode());
        value.put("enabled", rule.enabled());
        return value;
    }

    private String revision(Map<String, Object> content) {
        return UUID.nameUUIDFromBytes(content.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }
}
