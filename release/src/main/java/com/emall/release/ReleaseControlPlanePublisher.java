package com.emall.release;

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
class ReleaseControlPlanePublisher {
    private static final String MODULE = "release";

    private final ControlPlaneClient client;
    private final ControlPlaneProperties properties;

    ReleaseControlPlanePublisher(ControlPlaneClient client, ControlPlaneProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    void publishToggles(String serviceName, List<FeatureToggle> toggles) {
        List<Map<String, Object>> values = toggles.stream().filter(toggle -> toggle.serviceName().equals(serviceName))
                .sorted(Comparator.comparing(FeatureToggle::flagKey)).map(this::toggle).toList();
        Map<String, Object> content = Map.of("schemaVersion", 1, "serviceName", serviceName, "toggles", values);
        String key = key("sync-feature-toggles", revision(content));
        ControlPlaneProperties.Nacos nacos = properties.getNacos();
        client.submit(ControlPlaneCommands.nacosConfig(key, MODULE, "sync-feature-toggles", "feature-toggles",
                serviceName, "emall-feature-toggles-" + serviceName + ".json", nacos.getGroup(), nacos.getNamespace(),
                content));
    }

    void publishTopics(List<MessageTopicGovernance> topics) {
        List<Map<String, Object>> values = topics.stream()
                .sorted(Comparator.comparing(MessageTopicGovernance::topicName)).map(this::topic).toList();
        Map<String, Object> content = Map.of("schemaVersion", 1, "topics", values);
        String key = key("sync-topic-governance", revision(content));
        ControlPlaneProperties.Nacos nacos = properties.getNacos();
        client.submit(ControlPlaneCommands.nacosConfig(key, MODULE, "sync-topic-governance", "topic-governance",
                "global", "emall-topic-governance.json", nacos.getGroup(), nacos.getNamespace(), content));
    }

    void publishRollout(RolloutPlan rollout) {
        String resourceId = Long.toString(rollout.rolloutId());
        String suffix = resourceId + '-' + rollout.status() + '-' + rollout.currentPercent() + '-' + rollout.version();
        String key = key("reconcile-rollout", suffix);
        if (rollout.status() == RolloutStatus.ROLLED_BACK) {
            client.submit(ControlPlaneCommands.infrastructure(key, MODULE, "rollback-rollout", "rollout", resourceId,
                    Map.of("namespace", properties.getKubernetes().getNamespace(), "name", rollout.serviceName(),
                            "rolloutId", rollout.rolloutId())));
            return;
        }
        Map<String, Object> manifest = rolloutManifest(rollout);
        client.submit(ControlPlaneCommands.kubernetesResource(key, MODULE, "reconcile-rollout", "rollout", resourceId,
                "argoproj.io/v1alpha1", "rollouts", properties.getKubernetes().getNamespace(), rollout.serviceName(),
                manifest));
    }

    void startReplay(ReplayPlan replay) {
        String resourceId = Long.toString(replay.replayId());
        String suffix = resourceId + '-' + replay.fromOffset() + '-' + replay.toOffset();
        client.submit(ControlPlaneCommands.kafkaConsumerOffsets(key("start-message-replay", suffix), MODULE,
                "start-message-replay", resourceId, replay.topicName(), replay.consumerGroup(), replay.fromOffset(),
                replay.toOffset()));
    }

    private Map<String, Object> rolloutManifest(RolloutPlan rollout) {
        Map<String, Object> container = Map.of("name", rollout.serviceName(), "image",
                "emall/" + rollout.serviceName() + ':' + rollout.version());
        Map<String, Object> podSpec = Map.of("containers", List.of(container));
        Map<String, Object> template = Map.of("spec", podSpec);
        Map<String, Object> strategy =
                Map.of("canary", Map.of("steps", List.of(Map.of("setWeight", rollout.currentPercent()))));
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("paused", rollout.status() == RolloutStatus.PAUSED || rollout.status() == RolloutStatus.PLANNED);
        spec.put("template", template);
        spec.put("strategy", strategy);
        return Map.of("apiVersion", "argoproj.io/v1alpha1", "kind", "Rollout", "metadata",
                Map.of("name", rollout.serviceName()), "spec", spec);
    }

    private Map<String, Object> toggle(FeatureToggle toggle) {
        return Map.of("flagKey", toggle.flagKey(), "status", toggle.status().name(), "rolloutPercent",
                toggle.rolloutPercent());
    }

    private Map<String, Object> topic(MessageTopicGovernance topic) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("topicName", topic.topicName());
        value.put("owner", topic.owner());
        value.put("schemaVersion", topic.schemaVersion());
        value.put("lagBudget", topic.lagBudget());
        value.put("status", topic.status().name());
        return value;
    }

    private String key(String action, String suffix) {
        return ControlPlaneIdempotencyKeys.currentOrDeterministic(MODULE, action, suffix);
    }

    private String revision(Map<String, Object> content) {
        return UUID.nameUUIDFromBytes(content.toString().getBytes(StandardCharsets.UTF_8)).toString();
    }
}
