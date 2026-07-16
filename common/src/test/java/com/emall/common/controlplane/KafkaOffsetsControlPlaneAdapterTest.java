package com.emall.common.controlplane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.junit.jupiter.api.Test;

class KafkaOffsetsControlPlaneAdapterTest {
    @Test
    void resetsInactiveGroupAndReadsBackReplayProgress() {
        Admin admin = mock(Admin.class);
        String group = "search-replay";
        String topic = "product.changed";
        TopicPartition partition = new TopicPartition(topic, 0);
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> offsets =
                new AtomicReference<>(Map.of(partition, new OffsetAndMetadata(50)));
        stubInactiveGroup(admin, group);
        stubTopic(admin, topic);
        stubOffsets(admin, group, offsets);
        stubBounds(admin, partition);
        AtomicReference<Map<TopicPartition, OffsetAndMetadata>> altered = new AtomicReference<>();
        when(admin.alterConsumerGroupOffsets(eq(group), anyMap())).thenAnswer(invocation -> {
            Map<TopicPartition, OffsetAndMetadata> requested = invocation.getArgument(1);
            altered.set(requested);
            offsets.set(requested);
            AlterConsumerGroupOffsetsResult result = mock(AlterConsumerGroupOffsetsResult.class);
            when(result.all()).thenReturn(KafkaFuture.completedFuture(null));
            return result;
        });
        KafkaOffsetsControlPlaneAdapter adapter = new KafkaOffsetsControlPlaneAdapter(admin, Duration.ofSeconds(1));
        ControlPlaneOperation operation = operation(
                ControlPlaneCommands.kafkaConsumerOffsets("kafka-1", "release", "replay", "7", topic, group, 10, 100));
        Map<String, Object> rollback = adapter.captureRollbackState(operation);
        operation = withRollback(operation, rollback);

        adapter.apply(operation);
        offsets.set(Map.of(partition, new OffsetAndMetadata(100)));

        assertThat(altered.get().get(partition).offset()).isEqualTo(10);
        assertThat(adapter.observe(operation).converged()).isTrue();
        assertThat(ControlPlaneStateValues.map(rollback, "offsets")).containsEntry("0", 50L);
    }

    private void stubInactiveGroup(Admin admin, String group) {
        ConsumerGroupDescription description = mock(ConsumerGroupDescription.class);
        when(description.members()).thenReturn(List.of());
        DescribeConsumerGroupsResult result = mock(DescribeConsumerGroupsResult.class);
        when(result.describedGroups()).thenReturn(Map.of(group, KafkaFuture.completedFuture(description)));
        when(admin.describeConsumerGroups(List.of(group))).thenReturn(result);
    }

    private void stubTopic(Admin admin, String topic) {
        TopicPartitionInfo partition = mock(TopicPartitionInfo.class);
        when(partition.partition()).thenReturn(0);
        TopicDescription description = mock(TopicDescription.class);
        when(description.partitions()).thenReturn(List.of(partition));
        DescribeTopicsResult result = mock(DescribeTopicsResult.class);
        when(result.topicNameValues()).thenReturn(Map.of(topic, KafkaFuture.completedFuture(description)));
        when(admin.describeTopics(List.of(topic))).thenReturn(result);
    }

    private void stubOffsets(Admin admin, String group,
            AtomicReference<Map<TopicPartition, OffsetAndMetadata>> offsets) {
        ListConsumerGroupOffsetsResult result = mock(ListConsumerGroupOffsetsResult.class);
        when(result.partitionsToOffsetAndMetadata())
                .thenAnswer(invocation -> KafkaFuture.completedFuture(offsets.get()));
        when(admin.listConsumerGroupOffsets(group)).thenReturn(result);
    }

    private void stubBounds(Admin admin, TopicPartition partition) {
        ListOffsetsResultInfo earliest = mock(ListOffsetsResultInfo.class);
        ListOffsetsResultInfo latest = mock(ListOffsetsResultInfo.class);
        when(earliest.offset()).thenReturn(0L);
        when(latest.offset()).thenReturn(1000L);
        ListOffsetsResult first = mock(ListOffsetsResult.class);
        ListOffsetsResult second = mock(ListOffsetsResult.class);
        when(first.all()).thenReturn(KafkaFuture.completedFuture(Map.of(partition, earliest)));
        when(second.all()).thenReturn(KafkaFuture.completedFuture(Map.of(partition, latest)));
        when(admin.listOffsets(anyMap())).thenReturn(first, second);
    }

    private ControlPlaneOperation operation(ControlPlaneCommand command) {
        ControlPlaneCommandService service = new ControlPlaneCommandService(new InMemoryControlPlaneOperationStore(),
                new ControlPlaneProperties(), new ObjectMapper(), Clock.systemUTC());
        return service.submit(command);
    }

    private ControlPlaneOperation withRollback(ControlPlaneOperation operation, Map<String, Object> rollback) {
        return new ControlPlaneOperation(operation.operationId(), operation.idempotencyKey(), operation.module(),
                operation.target(), operation.action(), operation.resourceType(), operation.resourceId(),
                operation.desiredState(), operation.desiredDigest(), rollback, operation.observedState(),
                operation.status(), operation.attemptCount(), operation.maxAttempts(), operation.nextAttemptAt(),
                operation.leaseOwner(), operation.leaseUntil(), operation.lastError(), operation.createdAt(),
                operation.updatedAt());
    }
}
