package com.emall.release;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReleaseService {
    private final ReleaseRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    ReleaseService(ReleaseRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    FeatureToggle createToggle(String flagKey, String serviceName, ToggleStatus status, int rolloutPercent) {
        requirePercent(rolloutPercent);
        Instant now = Instant.now();
        return repository.saveToggle(new FeatureToggle(idGenerator.nextId(), normalize(flagKey), normalize(serviceName),
                status, rolloutPercent, now, now));
    }

    @Transactional
    FeatureToggle updateToggle(long toggleId, ToggleStatus status, int rolloutPercent) {
        requirePercent(rolloutPercent);
        FeatureToggle toggle = repository.findToggle(toggleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "feature toggle not found"));
        return repository.saveToggle(toggle.change(status, rolloutPercent));
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
        return repository.saveRollout(rollout.change(status, currentPercent));
    }

    @Transactional
    MessageTopicGovernance registerTopic(String topicName, String owner, String schemaVersion, long lagBudget) {
        if (lagBudget <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "lag budget must be positive");
        }
        Instant now = Instant.now();
        return repository.saveTopic(new MessageTopicGovernance(idGenerator.nextId(), normalize(topicName),
                normalize(owner), normalize(schemaVersion), lagBudget, TopicStatus.ACTIVE, now, now));
    }

    @Transactional
    MessageTopicGovernance changeTopicStatus(long topicId, TopicStatus status) {
        MessageTopicGovernance topic = repository.findTopic(topicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "message topic not found"));
        return repository.saveTopic(topic.changeStatus(status));
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
        return repository.saveReplay(replay.changeStatus(status));
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

    private void requirePercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "percent must be 0-100");
        }
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "release value must not be blank");
        }
        return normalized;
    }
}
