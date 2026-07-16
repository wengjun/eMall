package com.emall.common.controlplane;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ControlPlaneCommands {
    private ControlPlaneCommands() {
    }

    public static ControlPlaneCommand nacosConfig(String idempotencyKey, String module, String action,
            String resourceType, String resourceId, String dataId, String group, String namespace, Object content) {
        Map<String, Object> desired = new LinkedHashMap<>();
        desired.put("dataId", dataId);
        desired.put("group", group);
        desired.put("namespace", namespace);
        desired.put("content", content);
        return new ControlPlaneCommand(idempotencyKey, module, ControlPlaneTarget.NACOS_CONFIG, action, resourceType,
                resourceId, desired);
    }

    public static ControlPlaneCommand kubernetesResource(String idempotencyKey, String module, String action,
            String resourceType, String resourceId, String apiVersion, String plural, String namespace, String name,
            Map<String, Object> manifest) {
        Map<String, Object> desired = new LinkedHashMap<>();
        desired.put("apiVersion", apiVersion);
        desired.put("plural", plural);
        desired.put("namespace", namespace);
        desired.put("name", name);
        desired.put("manifest", manifest);
        return new ControlPlaneCommand(idempotencyKey, module, ControlPlaneTarget.KUBERNETES_RESOURCE, action,
                resourceType, resourceId, desired);
    }

    public static ControlPlaneCommand kafkaConsumerOffsets(String idempotencyKey, String module, String action,
            String resourceId, String topic, String consumerGroup, long fromOffset, long toOffset) {
        Map<String, Object> desired =
                Map.of("topic", topic, "consumerGroup", consumerGroup, "fromOffset", fromOffset, "toOffset", toOffset);
        return new ControlPlaneCommand(idempotencyKey, module, ControlPlaneTarget.KAFKA_CONSUMER_OFFSETS, action,
                "kafka-consumer-offsets", resourceId, desired);
    }

    public static ControlPlaneCommand infrastructure(String idempotencyKey, String module, String action,
            String resourceType, String resourceId, Map<String, Object> desiredState) {
        return new ControlPlaneCommand(idempotencyKey, module, ControlPlaneTarget.INFRASTRUCTURE_API, action,
                resourceType, resourceId, desiredState);
    }
}
