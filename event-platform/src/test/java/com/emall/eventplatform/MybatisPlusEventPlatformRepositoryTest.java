package com.emall.eventplatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.emall.common.privacy.SensitiveDataType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisPlusEventPlatformRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final EventPlatformMapper eventPlatformMapper = mock(EventPlatformMapper.class);
    private final EventSchemaMapper schemaMapper = mock(EventSchemaMapper.class);
    private final EventFieldClassificationMapper classificationMapper = mock(EventFieldClassificationMapper.class);
    private final TrackingEventMapper trackingEventMapper = mock(TrackingEventMapper.class);
    private final MetricMaterializationMapper materializationMapper = mock(MetricMaterializationMapper.class);
    private final MybatisPlusEventPlatformRepository repository = new MybatisPlusEventPlatformRepository(
            eventPlatformMapper, schemaMapper, classificationMapper, trackingEventMapper, materializationMapper);

    @Test
    void shouldKeepUniqueKeyUpsertsOnCustomMapper() {
        EventSchema schema = new EventSchema(1001L, "product_view", 1, "growth", "{\"type\":\"object\"}",
                SchemaStatus.ACTIVE, NOW, NOW);
        EventFieldClassification classification = new EventFieldClassification(2001L, "product_view", 1, "mobile",
                SensitiveDataType.MOBILE, true, false, NOW);
        PipelineOffset offset = new PipelineOffset(3001L, "warehouse-consumer", "tracking-events", 9001L, NOW);

        repository.saveSchema(schema);
        repository.saveFieldClassification(classification);
        repository.saveOffset(offset);

        ArgumentCaptor<EventSchemaEntity> schemaCaptor = ArgumentCaptor.forClass(EventSchemaEntity.class);
        ArgumentCaptor<EventFieldClassificationEntity> classificationCaptor =
                ArgumentCaptor.forClass(EventFieldClassificationEntity.class);
        ArgumentCaptor<PipelineOffsetEntity> offsetCaptor = ArgumentCaptor.forClass(PipelineOffsetEntity.class);
        verify(eventPlatformMapper).upsertSchema(schemaCaptor.capture());
        verify(eventPlatformMapper).upsertFieldClassification(classificationCaptor.capture());
        verify(eventPlatformMapper).upsertOffset(offsetCaptor.capture());
        assertThat(schemaCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(classificationCaptor.getValue().getSensitivity()).isEqualTo("MOBILE");
        assertThat(offsetCaptor.getValue().getProcessedOffset()).isEqualTo(9001L);
    }

    @Test
    void shouldPersistAndQueryTrackingEventsThroughMybatisPlusMapper() {
        TrackingEvent event = new TrackingEvent(4001L, "product_view", 1, "event-1", "user-1",
                "{\"skuId\":1001}", false, NOW, NOW);
        TrackingEventEntity stored = trackingEventEntity(event);
        when(trackingEventMapper.selectCount(anyTrackingEventWrapper())).thenReturn(1L);
        when(trackingEventMapper.selectList(anyTrackingEventWrapper())).thenReturn(List.of(stored));

        repository.saveEvent(event);
        boolean exists = repository.eventExists("event-1");
        List<TrackingEvent> events = repository.findEvents("product_view");

        ArgumentCaptor<TrackingEventEntity> eventCaptor = ArgumentCaptor.forClass(TrackingEventEntity.class);
        verify(trackingEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventKey()).isEqualTo("event-1");
        assertThat(exists).isTrue();
        assertThat(events).containsExactly(event);
    }

    @Test
    void shouldReadReferenceDataThroughMybatisPlusMappers() {
        EventSchemaEntity schema = eventSchemaEntity();
        EventFieldClassificationEntity classification = classificationEntity();
        MetricMaterializationEntity materialization = materializationEntity();
        when(schemaMapper.selectOne(anySchemaWrapper())).thenReturn(schema);
        when(schemaMapper.selectList(isNull())).thenReturn(List.of(schema));
        when(classificationMapper.selectList(anyClassificationWrapper())).thenReturn(List.of(classification));
        when(materializationMapper.selectList(isNull())).thenReturn(List.of(materialization));

        assertThat(repository.findSchema("product_view", 1)).contains(new EventSchema(1001L, "product_view", 1,
                "growth", "{\"type\":\"object\"}", SchemaStatus.ACTIVE, NOW, NOW));
        assertThat(repository.findSchemas()).hasSize(1);
        assertThat(repository.findFieldClassifications("product_view", 1)).containsExactly(new EventFieldClassification(
                2001L, "product_view", 1, "mobile", SensitiveDataType.MOBILE, true, false, NOW));
        assertThat(repository.findMaterializations()).containsExactly(
                new MetricMaterialization(5001L, "product_view_count", "2026-01-01T00", 10L, 1L, NOW));
    }

    private EventSchemaEntity eventSchemaEntity() {
        EventSchemaEntity entity = new EventSchemaEntity();
        entity.setSchemaId(1001L);
        entity.setEventName("product_view");
        entity.setVersion(1);
        entity.setOwner("growth");
        entity.setJsonSchema("{\"type\":\"object\"}");
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(toDatabaseTime(NOW));
        entity.setUpdatedAt(toDatabaseTime(NOW));
        return entity;
    }

    private EventFieldClassificationEntity classificationEntity() {
        EventFieldClassificationEntity entity = new EventFieldClassificationEntity();
        entity.setClassificationId(2001L);
        entity.setEventName("product_view");
        entity.setVersion(1);
        entity.setFieldName("mobile");
        entity.setSensitivity("MOBILE");
        entity.setRequired(true);
        entity.setExportedToWarehouse(false);
        entity.setCreatedAt(toDatabaseTime(NOW));
        return entity;
    }

    private TrackingEventEntity trackingEventEntity(TrackingEvent event) {
        TrackingEventEntity entity = new TrackingEventEntity();
        entity.setEventId(event.eventId());
        entity.setEventName(event.eventName());
        entity.setVersion(event.version());
        entity.setEventKey(event.eventKey());
        entity.setUserKey(event.userKey());
        entity.setPayload(event.payload());
        entity.setLateEvent(event.lateEvent());
        entity.setOccurredAt(toDatabaseTime(event.occurredAt()));
        entity.setIngestedAt(toDatabaseTime(event.ingestedAt()));
        return entity;
    }

    private MetricMaterializationEntity materializationEntity() {
        MetricMaterializationEntity entity = new MetricMaterializationEntity();
        entity.setMaterializationId(5001L);
        entity.setMetricName("product_view_count");
        entity.setWindowKey("2026-01-01T00");
        entity.setEventCount(10L);
        entity.setLateEventCount(1L);
        entity.setMaterializedAt(toDatabaseTime(NOW));
        return entity;
    }

    private LocalDateTime toDatabaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<EventSchemaEntity> anySchemaWrapper() {
        return any(Wrapper.class);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<EventFieldClassificationEntity> anyClassificationWrapper() {
        return any(Wrapper.class);
    }

    @SuppressWarnings("unchecked")
    private Wrapper<TrackingEventEntity> anyTrackingEventWrapper() {
        return any(Wrapper.class);
    }
}
