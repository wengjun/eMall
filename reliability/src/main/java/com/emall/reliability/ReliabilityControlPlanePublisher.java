package com.emall.reliability;

import com.emall.common.controlplane.ControlPlaneClient;
import com.emall.common.controlplane.ControlPlaneCommands;
import com.emall.common.controlplane.ControlPlaneIdempotencyKeys;
import com.emall.common.controlplane.ControlPlaneProperties;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class ReliabilityControlPlanePublisher {
    private static final String MODULE = "reliability";

    private final ControlPlaneClient client;
    private final ControlPlaneProperties properties;

    ReliabilityControlPlanePublisher(ControlPlaneClient client, ControlPlaneProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    void runCapacityRehearsal(CapacityRehearsal rehearsal) {
        String resourceId = Long.toString(rehearsal.rehearsalId());
        String key = key("run-capacity-rehearsal", resourceId);
        Map<String, Object> desired = Map.of("serviceName", rehearsal.serviceName(), "targetQps", rehearsal.targetQps(),
                "peakConcurrency", rehearsal.peakConcurrency(), "rehearsalId", rehearsal.rehearsalId());
        client.submit(ControlPlaneCommands.infrastructure(key, MODULE, "run-capacity-rehearsal", "capacity-rehearsal",
                resourceId, desired));
    }

    void publishSlo(SloObjective slo) {
        String resourceId = Long.toString(slo.sloId());
        String key = key("publish-slo", resourceId);
        String service = slo.serviceName();
        String availabilityExpression = "1 - (sum(rate(http_server_requests_seconds_count{application=\"" + service
                + "\",outcome!=\"SUCCESS\"}[5m])) / sum(rate(http_server_requests_seconds_count{application=\""
                + service + "\"}[5m]))) < " + slo.availabilityTarget();
        String latencyExpression =
                "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application=\"" + service
                        + "\"}[5m])) by (le)) > " + (slo.latencyP95Ms() / 1000.0);
        List<Map<String, Object>> rules =
                List.of(alert("SloAvailabilityBurn_" + service, availabilityExpression, service, "availability"),
                        alert("SloLatencyP95_" + service, latencyExpression, service, "latency"));
        Map<String, Object> manifest = Map.of("apiVersion", "monitoring.coreos.com/v1", "kind", "PrometheusRule",
                "metadata", Map.of("name", "emall-slo-" + service), "spec",
                Map.of("groups", List.of(Map.of("name", "emall-slo-" + service, "rules", rules))));
        client.submit(ControlPlaneCommands.kubernetesResource(key, MODULE, "publish-slo", "prometheus-rule", resourceId,
                "monitoring.coreos.com/v1", "prometheusrules", properties.getKubernetes().getNamespace(),
                "emall-slo-" + service, manifest));
    }

    void scheduleChaos(ChaosSchedule chaos) {
        String resourceId = Long.toString(chaos.chaosId());
        String key = key("schedule-chaos", resourceId);
        Map<String, Object> desired = Map.of("chaosId", chaos.chaosId(), "serviceName", chaos.serviceName(),
                "drillType", chaos.drillType(), "blastRadiusPercent", chaos.blastRadiusPercent(), "scheduledAt",
                chaos.scheduledAt().toString());
        client.submit(ControlPlaneCommands.infrastructure(key, MODULE, "schedule-chaos", "chaos-experiment", resourceId,
                desired));
    }

    private Map<String, Object> alert(String name, String expression, String service, String objective) {
        return Map.of("alert", name, "expr", expression, "for", "5m", "labels",
                Map.of("severity", "critical", "service", service, "slo", objective));
    }

    private String key(String action, String suffix) {
        return ControlPlaneIdempotencyKeys.currentOrDeterministic(MODULE, action, suffix);
    }
}
