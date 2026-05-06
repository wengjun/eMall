package com.emall.common.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.event.OutboxStatus;
import com.emall.common.outbox.InMemoryOutboxRepositorySupport;
import com.emall.common.outbox.OutboxRepository;
import com.emall.common.outbox.OutboxPublisherSupport;
import com.emall.common.task.InMemoryDistributedTaskLock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class KafkaOutboxPublisherIT {
    private static final String TOPIC = "emall.outbox.it";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @Test
    void shouldPublishOutboxEventToKafkaAndMarkItPublished() throws Exception {
        TestOutboxRepository outboxRepository = new TestOutboxRepository();
        OutboxEvent event = OutboxEvent.create("event-001", "Product", "30001",
                EventTypes.PRODUCT_CHANGED, Map.of("skuId", 30001L));
        outboxRepository.save(event);
        TestOutboxPublisher publisher = new TestOutboxPublisher(outboxRepository, kafkaTemplate());

        int published = publisher.publishBatch(10);
        JsonNode message = consumeOneMessage();

        assertThat(published).isEqualTo(1);
        assertThat(outboxRepository.event("event-001").status()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(message.path("eventId").asText()).isEqualTo("event-001");
        assertThat(message.path("eventType").asText()).isEqualTo(EventTypes.PRODUCT_CHANGED);
        assertThat(message.path("aggregateId").asText()).isEqualTo("30001");
    }

    private KafkaTemplate<String, String> kafkaTemplate() {
        Map<String, Object> properties = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(properties));
    }

    private JsonNode consumeOneMessage() throws Exception {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "emall-outbox-it");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.count()).isGreaterThanOrEqualTo(1);
            return OBJECT_MAPPER.readTree(records.iterator().next().value());
        }
    }

    private static final class TestOutboxPublisher extends OutboxPublisherSupport {
        private TestOutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
            super(outboxRepository, kafkaTemplate, OBJECT_MAPPER, "outbox-it", TOPIC,
                    new InMemoryDistributedTaskLock(Clock.systemUTC(), "outbox-it"));
        }
    }

    private static final class TestOutboxRepository extends InMemoryOutboxRepositorySupport {
        private final ConcurrentMap<String, OutboxEvent> events = new ConcurrentHashMap<>();

        @Override
        public OutboxEvent save(OutboxEvent event) {
            events.put(event.eventId(), event);
            return super.save(event);
        }

        private OutboxEvent event(String eventId) {
            return events.get(eventId);
        }
    }
}
