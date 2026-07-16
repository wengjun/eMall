package com.emall.common.controlplane;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;

public class KafkaOffsetsControlPlaneAdapter implements ControlPlaneAdapter {
    private final Admin admin;
    private final Duration timeout;

    public KafkaOffsetsControlPlaneAdapter(Admin admin, Duration timeout) {
        this.admin = admin;
        this.timeout = timeout;
    }

    @Override
    public ControlPlaneTarget target() {
        return ControlPlaneTarget.KAFKA_CONSUMER_OFFSETS;
    }

    @Override
    public Map<String, Object> captureRollbackState(ControlPlaneOperation operation) {
        Map<String, Object> desired = operation.desiredState();
        String group = ControlPlaneStateValues.text(desired, "consumerGroup");
        String topic = ControlPlaneStateValues.text(desired, "topic");
        long fromOffset = ControlPlaneStateValues.number(desired, "fromOffset");
        long toOffset = ControlPlaneStateValues.number(desired, "toOffset");
        requireInactive(group);

        Set<TopicPartition> partitions = topicPartitions(topic);
        Map<TopicPartition, OffsetAndMetadata> current = offsets(group);
        Map<TopicPartition, ListOffsetsResultInfo> earliest = listOffsets(partitions, OffsetSpec.earliest());
        Map<TopicPartition, ListOffsetsResultInfo> latest = listOffsets(partitions, OffsetSpec.latest());
        Map<String, Object> previousOffsets = new LinkedHashMap<>();
        Map<String, Object> replayEndOffsets = new LinkedHashMap<>();
        List<Integer> missingPartitions = new ArrayList<>();
        for (TopicPartition partition : partitions) {
            long earliestOffset = earliest.get(partition).offset();
            long latestOffset = latest.get(partition).offset();
            long replayEnd = Math.min(toOffset, latestOffset);
            if (fromOffset < earliestOffset || fromOffset > replayEnd) {
                throw new IllegalArgumentException("replay offsets are outside the retained range for " + partition);
            }
            OffsetAndMetadata previous = current.get(partition);
            if (previous == null) {
                missingPartitions.add(partition.partition());
            } else {
                previousOffsets.put(Integer.toString(partition.partition()), previous.offset());
            }
            replayEndOffsets.put(Integer.toString(partition.partition()), replayEnd);
        }
        Map<String, Object> rollback = new LinkedHashMap<>();
        rollback.put("topic", topic);
        rollback.put("consumerGroup", group);
        rollback.put("offsets", previousOffsets);
        rollback.put("missingPartitions", missingPartitions);
        rollback.put("replayEndOffsets", replayEndOffsets);
        return rollback;
    }

    @Override
    public void apply(ControlPlaneOperation operation) {
        String group = ControlPlaneStateValues.text(operation.desiredState(), "consumerGroup");
        String topic = ControlPlaneStateValues.text(operation.desiredState(), "topic");
        long fromOffset = ControlPlaneStateValues.number(operation.desiredState(), "fromOffset");
        requireInactive(group);
        Map<TopicPartition, OffsetAndMetadata> target = new LinkedHashMap<>();
        replayEndOffsets(operation).keySet().forEach(
                partition -> target.put(new TopicPartition(topic, partition), new OffsetAndMetadata(fromOffset)));
        await(admin.alterConsumerGroupOffsets(group, target).all());
    }

    @Override
    public ControlPlaneObservation observe(ControlPlaneOperation operation) {
        String group = ControlPlaneStateValues.text(operation.desiredState(), "consumerGroup");
        String topic = ControlPlaneStateValues.text(operation.desiredState(), "topic");
        Map<TopicPartition, OffsetAndMetadata> current = offsets(group);
        Map<Integer, Long> replayEnds = replayEndOffsets(operation);
        boolean converged = replayEnds.entrySet().stream().allMatch(entry -> {
            OffsetAndMetadata offset = current.get(new TopicPartition(topic, entry.getKey()));
            return offset != null && offset.offset() >= entry.getValue();
        });
        return new ControlPlaneObservation(converged, offsetState(current, topic),
                converged ? "replay range consumed" : "replay consumer has not reached the requested end offsets");
    }

    @Override
    public void rollback(ControlPlaneOperation operation) {
        Map<String, Object> rollback = operation.rollbackState();
        String group = ControlPlaneStateValues.text(rollback, "consumerGroup");
        String topic = ControlPlaneStateValues.text(rollback, "topic");
        requireInactive(group);
        Map<TopicPartition, OffsetAndMetadata> previous = new LinkedHashMap<>();
        offsetMap(ControlPlaneStateValues.map(rollback, "offsets")).forEach((partition, offset) -> previous
                .put(new TopicPartition(topic, partition), new OffsetAndMetadata(offset)));
        if (!previous.isEmpty()) {
            await(admin.alterConsumerGroupOffsets(group, previous).all());
        }
        Set<TopicPartition> missing = new LinkedHashSet<>();
        for (Object value : list(rollback.get("missingPartitions"))) {
            missing.add(new TopicPartition(topic, ((Number) value).intValue()));
        }
        if (!missing.isEmpty()) {
            await(admin.deleteConsumerGroupOffsets(group, missing).all());
        }
    }

    @Override
    public ControlPlaneObservation observeRollback(ControlPlaneOperation operation) {
        Map<String, Object> rollback = operation.rollbackState();
        String group = ControlPlaneStateValues.text(rollback, "consumerGroup");
        String topic = ControlPlaneStateValues.text(rollback, "topic");
        Map<TopicPartition, OffsetAndMetadata> current = offsets(group);
        Map<Integer, Long> previous = offsetMap(ControlPlaneStateValues.map(rollback, "offsets"));
        boolean converged = previous.entrySet().stream().allMatch(entry -> {
            OffsetAndMetadata offset = current.get(new TopicPartition(topic, entry.getKey()));
            return offset != null && offset.offset() == entry.getValue();
        });
        for (Object value : list(rollback.get("missingPartitions"))) {
            converged &= !current.containsKey(new TopicPartition(topic, ((Number) value).intValue()));
        }
        return new ControlPlaneObservation(converged, offsetState(current, topic),
                converged ? "rollback converged" : "Kafka rollback offsets differ");
    }

    private void requireInactive(String group) {
        ConsumerGroupDescription description =
                await(admin.describeConsumerGroups(List.of(group)).describedGroups().get(group));
        if (!description.members().isEmpty()) {
            throw new IllegalStateException("consumer group must be inactive before changing offsets: " + group);
        }
    }

    private Set<TopicPartition> topicPartitions(String topic) {
        return await(admin.describeTopics(List.of(topic)).topicNameValues().get(topic)).partitions().stream()
                .map(partition -> new TopicPartition(topic, partition.partition()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<TopicPartition, OffsetAndMetadata> offsets(String group) {
        return await(admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata());
    }

    private Map<TopicPartition, ListOffsetsResultInfo> listOffsets(Collection<TopicPartition> partitions,
            OffsetSpec specification) {
        Map<TopicPartition, OffsetSpec> request = new LinkedHashMap<>();
        partitions.forEach(partition -> request.put(partition, specification));
        return await(admin.listOffsets(request).all());
    }

    private Map<Integer, Long> replayEndOffsets(ControlPlaneOperation operation) {
        return offsetMap(ControlPlaneStateValues.map(operation.rollbackState(), "replayEndOffsets"));
    }

    private Map<Integer, Long> offsetMap(Map<String, Object> source) {
        Map<Integer, Long> offsets = new LinkedHashMap<>();
        source.forEach((partition, offset) -> offsets.put(Integer.parseInt(partition), ((Number) offset).longValue()));
        return offsets;
    }

    private Map<String, Object> offsetState(Map<TopicPartition, OffsetAndMetadata> offsets, String topic) {
        Map<String, Object> state = new LinkedHashMap<>();
        offsets.entrySet().stream().filter(entry -> entry.getKey().topic().equals(topic))
                .forEach(entry -> state.put(Integer.toString(entry.getKey().partition()), entry.getValue().offset()));
        return state;
    }

    private List<?> list(Object value) {
        if (value instanceof List<?> values) {
            return values;
        }
        throw new IllegalArgumentException("stored Kafka partition list is invalid");
    }

    private <T> T await(KafkaFuture<T> future) {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka control-plane call was interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Kafka control-plane call failed", exception);
        }
    }
}
