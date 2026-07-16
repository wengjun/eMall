package com.emall.release;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.controlplane.ControlPlaneAssertions;
import com.emall.common.controlplane.ControlPlaneClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReleaseService {
    private static final BigDecimal MAX_CANARY_ERROR_RATE = new BigDecimal("0.010000");
    private static final BigDecimal ROLLBACK_CANARY_ERROR_RATE = new BigDecimal("0.030000");
    private static final int MAX_CANARY_LATENCY_P95_MS = 500;
    private static final int ROLLBACK_CANARY_LATENCY_P95_MS = 1000;
    private static final BigDecimal MIN_CANARY_BUSINESS_SUCCESS_RATE = new BigDecimal("0.995000");
    private static final BigDecimal ROLLBACK_CANARY_BUSINESS_SUCCESS_RATE = new BigDecimal("0.980000");

    private final ReleaseRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final ReleaseControlPlanePublisher controlPlanePublisher;
    private final ControlPlaneClient controlPlaneClient;

    ReleaseService(ReleaseRepository repository, SnowflakeIdGenerator idGenerator,
            ReleaseControlPlanePublisher controlPlanePublisher, ControlPlaneClient controlPlaneClient) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.controlPlanePublisher = controlPlanePublisher;
        this.controlPlaneClient = controlPlaneClient;
    }

    @Transactional
    FeatureToggle createToggle(String flagKey, String serviceName, ToggleStatus status, int rolloutPercent) {
        requirePercent(rolloutPercent);
        Instant now = Instant.now();
        FeatureToggle toggle = repository.saveToggle(new FeatureToggle(idGenerator.nextId(), normalize(flagKey),
                normalize(serviceName), status, rolloutPercent, now, now));
        controlPlanePublisher.publishToggles(toggle.serviceName(), repository.findToggles());
        return toggle;
    }

    @Transactional
    FeatureToggle updateToggle(long toggleId, ToggleStatus status, int rolloutPercent) {
        requirePercent(rolloutPercent);
        FeatureToggle toggle = repository.findToggle(toggleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "feature toggle not found"));
        FeatureToggle saved = repository.saveToggle(toggle.change(status, rolloutPercent));
        controlPlanePublisher.publishToggles(saved.serviceName(), repository.findToggles());
        return saved;
    }

    @Transactional
    RolloutPlan createRollout(String serviceName, String version, String strategy, int currentPercent) {
        requirePercent(currentPercent);
        Instant now = Instant.now();
        return repository.saveRollout(new RolloutPlan(idGenerator.nextId(), normalize(serviceName), normalize(version),
                normalize(strategy), currentPercent, RolloutStatus.PLANNED, now, now));
    }

    @Transactional
    RolloutPlan changeRollout(long rolloutId, RolloutStatus status, int currentPercent) {
        requirePercent(currentPercent);
        RolloutPlan rollout = repository.findRollout(rolloutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "rollout plan not found"));
        if (status == RolloutStatus.COMPLETED) {
            if (rollout.currentPercent() != currentPercent) {
                throw new BusinessException(ErrorCode.CONFLICT,
                        "rollout percent must converge before marking the rollout complete");
            }
            ControlPlaneAssertions.requireSucceeded(controlPlaneClient, "release", "rollout", Long.toString(rolloutId));
        }
        if (status == RolloutStatus.RUNNING || status == RolloutStatus.COMPLETED) {
            requirePreTrafficGuardPassed(rollout);
        }
        RolloutPlan saved = repository.saveRollout(rollout.change(status, currentPercent));
        controlPlanePublisher.publishRollout(saved);
        return saved;
    }

    @Transactional
    MessageTopicGovernance registerTopic(String topicName, String owner, String schemaVersion, long lagBudget) {
        if (lagBudget <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "lag budget must be positive");
        }
        Instant now = Instant.now();
        MessageTopicGovernance topic =
                repository.saveTopic(new MessageTopicGovernance(idGenerator.nextId(), normalize(topicName),
                        normalize(owner), normalize(schemaVersion), lagBudget, TopicStatus.ACTIVE, now, now));
        controlPlanePublisher.publishTopics(repository.findTopics());
        return topic;
    }

    @Transactional
    MessageTopicGovernance changeTopicStatus(long topicId, TopicStatus status) {
        MessageTopicGovernance topic = repository.findTopic(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "message topic not found"));
        MessageTopicGovernance saved = repository.saveTopic(topic.changeStatus(status));
        controlPlanePublisher.publishTopics(repository.findTopics());
        return saved;
    }

    @Transactional
    ReplayPlan createReplay(String topicName, String consumerGroup, long fromOffset, long toOffset) {
        if (fromOffset < 0 || toOffset < fromOffset) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "invalid replay offset range");
        }
        Instant now = Instant.now();
        return repository.saveReplay(new ReplayPlan(idGenerator.nextId(), normalize(topicName),
                normalize(consumerGroup), fromOffset, toOffset, RolloutStatus.PLANNED, now, now));
    }

    @Transactional
    ReplayPlan changeReplayStatus(long replayId, RolloutStatus status) {
        ReplayPlan replay = repository.findReplay(replayId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "replay plan not found"));
        if (status == RolloutStatus.COMPLETED) {
            ControlPlaneAssertions.requireSucceeded(controlPlaneClient, "release", "kafka-consumer-offsets",
                    Long.toString(replayId));
        }
        ReplayPlan saved = repository.saveReplay(replay.changeStatus(status));
        if (status == RolloutStatus.RUNNING) {
            controlPlanePublisher.startReplay(saved);
        }
        return saved;
    }

    ReleaseSummary summary() {
        int flags =
                (int) repository.findToggles().stream().filter(toggle -> toggle.status() == ToggleStatus.ON).count();
        int rollouts = (int) repository.findRollouts().stream()
                .filter(rollout -> rollout.status() == RolloutStatus.RUNNING).count();
        int topics =
                (int) repository.findTopics().stream().filter(topic -> topic.status() == TopicStatus.ACTIVE).count();
        int replays = (int) repository.findReplays().stream()
                .filter(replay -> replay.status() == RolloutStatus.PLANNED || replay.status() == RolloutStatus.RUNNING)
                .count();
        return new ReleaseSummary(flags, rollouts, topics, replays);
    }

    @Transactional
    ReleaseGuardRecord evaluatePreTrafficGuard(long rolloutId, boolean sloPassed, boolean alertsClear,
            boolean capacityReady, boolean dependenciesHealthy) {
        RolloutPlan rollout = repository.findRollout(rolloutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "rollout plan not found"));
        boolean passed = sloPassed && alertsClear && capacityReady && dependenciesHealthy;
        ReleaseGuardDecision decision = passed ? ReleaseGuardDecision.PASS : ReleaseGuardDecision.BLOCK;
        RolloutStatus status = passed ? RolloutStatus.RUNNING : RolloutStatus.PAUSED;
        RolloutPlan saved = repository.saveRollout(rollout.change(status, passed ? rollout.currentPercent() : 0));
        controlPlanePublisher.publishRollout(saved);
        return repository.saveGuard(new ReleaseGuardRecord(idGenerator.nextId(), rollout.rolloutId(),
                rollout.serviceName(), ReleaseGuardStage.PRE_TRAFFIC, decision, sloPassed, alertsClear, capacityReady,
                dependenciesHealthy, null, null, null, null, null, null,
                preTrafficReason(sloPassed, alertsClear, capacityReady, dependenciesHealthy), Instant.now()));
    }

    @Transactional
    ReleaseGuardRecord evaluateCanaryGuard(long rolloutId, int observedPercent, BigDecimal errorRate, int latencyP95Ms,
            BigDecimal businessSuccessRate) {
        requirePercent(observedPercent);
        requireRatio(errorRate, "error rate");
        requireRatio(businessSuccessRate, "business success rate");
        if (latencyP95Ms < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "latency p95 must not be negative");
        }
        RolloutPlan rollout = repository.findRollout(rolloutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "rollout plan not found"));
        requirePreTrafficGuardPassed(rollout);
        ReleaseGuardDecision decision = canaryDecision(errorRate, latencyP95Ms, businessSuccessRate);
        RolloutStatus status = switch (decision) {
            case PASS -> RolloutStatus.RUNNING;
            case PAUSE -> RolloutStatus.PAUSED;
            case ROLLBACK -> RolloutStatus.ROLLED_BACK;
            case BLOCK -> RolloutStatus.PAUSED;
        };
        int nextPercent = decision == ReleaseGuardDecision.ROLLBACK ? 0 : observedPercent;
        RolloutPlan saved = repository.saveRollout(rollout.change(status, nextPercent));
        controlPlanePublisher.publishRollout(saved);
        return repository.saveGuard(new ReleaseGuardRecord(idGenerator.nextId(), rollout.rolloutId(),
                rollout.serviceName(), ReleaseGuardStage.CANARY, decision, null, null, null, null, errorRate,
                latencyP95Ms, businessSuccessRate, null, null, null,
                canaryReason(decision, errorRate, latencyP95Ms, businessSuccessRate), Instant.now()));
    }

    @Transactional
    ReleaseGuardRecord verifyRollbackRecovery(long rolloutId, boolean compensationTriggered,
            boolean messageReplayChecked, boolean downstreamRecovered) {
        RolloutPlan rollout = repository.findRollout(rolloutId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "rollout plan not found"));
        if (rollout.status() != RolloutStatus.ROLLED_BACK) {
            throw new BusinessException(ErrorCode.CONFLICT, "rollback recovery guard requires rolled back rollout");
        }
        boolean passed = compensationTriggered && messageReplayChecked && downstreamRecovered;
        ReleaseGuardDecision decision = passed ? ReleaseGuardDecision.PASS : ReleaseGuardDecision.BLOCK;
        return repository.saveGuard(new ReleaseGuardRecord(idGenerator.nextId(), rollout.rolloutId(),
                rollout.serviceName(), ReleaseGuardStage.ROLLBACK_RECOVERY, decision, null, null, null, null, null,
                null, null, compensationTriggered, messageReplayChecked, downstreamRecovered,
                rollbackRecoveryReason(compensationTriggered, messageReplayChecked, downstreamRecovered),
                Instant.now()));
    }

    List<ReleaseGuardRecord> findRolloutGuards(long rolloutId) {
        return repository.findGuards(rolloutId);
    }

    ReleaseGuardSummary guardSummary() {
        List<ReleaseGuardRecord> guards = repository.findGuards();
        int passed = (int) guards.stream().filter(guard -> guard.decision() == ReleaseGuardDecision.PASS).count();
        int blocked = (int) guards.stream().filter(guard -> guard.decision() == ReleaseGuardDecision.BLOCK).count();
        int paused = (int) guards.stream().filter(guard -> guard.decision() == ReleaseGuardDecision.PAUSE).count();
        int rollbacks =
                (int) guards.stream().filter(guard -> guard.decision() == ReleaseGuardDecision.ROLLBACK).count();
        return new ReleaseGuardSummary(passed, blocked, paused, rollbacks);
    }

    private void requirePercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "percent must be 0-100");
        }
    }

    private void requireRatio(BigDecimal ratio, String name) {
        if (ratio == null || ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, name + " must be 0-1");
        }
    }

    private void requirePreTrafficGuardPassed(RolloutPlan rollout) {
        boolean passed = repository.findGuards(rollout.rolloutId()).stream()
                .anyMatch(guard -> guard.stage() == ReleaseGuardStage.PRE_TRAFFIC
                        && guard.decision() == ReleaseGuardDecision.PASS);
        if (!passed) {
            throw new BusinessException(ErrorCode.CONFLICT, "rollout must pass pre-traffic guard");
        }
    }

    private ReleaseGuardDecision canaryDecision(BigDecimal errorRate, int latencyP95Ms,
            BigDecimal businessSuccessRate) {
        if (errorRate.compareTo(ROLLBACK_CANARY_ERROR_RATE) >= 0 || latencyP95Ms >= ROLLBACK_CANARY_LATENCY_P95_MS
                || businessSuccessRate.compareTo(ROLLBACK_CANARY_BUSINESS_SUCCESS_RATE) <= 0) {
            return ReleaseGuardDecision.ROLLBACK;
        }
        if (errorRate.compareTo(MAX_CANARY_ERROR_RATE) > 0 || latencyP95Ms > MAX_CANARY_LATENCY_P95_MS
                || businessSuccessRate.compareTo(MIN_CANARY_BUSINESS_SUCCESS_RATE) < 0) {
            return ReleaseGuardDecision.PAUSE;
        }
        return ReleaseGuardDecision.PASS;
    }

    private String preTrafficReason(boolean sloPassed, boolean alertsClear, boolean capacityReady,
            boolean dependenciesHealthy) {
        List<String> failed = new ArrayList<>();
        if (!sloPassed) {
            failed.add("slo");
        }
        if (!alertsClear) {
            failed.add("alerts");
        }
        if (!capacityReady) {
            failed.add("capacity");
        }
        if (!dependenciesHealthy) {
            failed.add("dependencies");
        }
        return failed.isEmpty() ? "pre-traffic guard passed" : "blocked by " + String.join(",", failed);
    }

    private String canaryReason(ReleaseGuardDecision decision, BigDecimal errorRate, int latencyP95Ms,
            BigDecimal businessSuccessRate) {
        if (decision == ReleaseGuardDecision.PASS) {
            return "canary metrics passed";
        }
        return "canary metrics failed: errorRate=" + errorRate + ", latencyP95Ms=" + latencyP95Ms
                + ", businessSuccessRate=" + businessSuccessRate;
    }

    private String rollbackRecoveryReason(boolean compensationTriggered, boolean messageReplayChecked,
            boolean downstreamRecovered) {
        List<String> failed = new ArrayList<>();
        if (!compensationTriggered) {
            failed.add("compensation");
        }
        if (!messageReplayChecked) {
            failed.add("message-replay");
        }
        if (!downstreamRecovered) {
            failed.add("downstream");
        }
        return failed.isEmpty()
                ? "rollback recovery guard passed"
                : "rollback recovery blocked by " + String.join(",", failed);
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "release value must not be blank");
        }
        return normalized;
    }
}
