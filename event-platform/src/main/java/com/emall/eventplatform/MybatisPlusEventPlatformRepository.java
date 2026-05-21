package com.emall.eventplatform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.emall.common.privacy.SensitiveDataType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusEventPlatformRepository implements EventPlatformRepository {
    private final EventPlatformMapper eventPlatformMapper;
    private final EventSchemaMapper schemaMapper;
    private final EventFieldClassificationMapper classificationMapper;
    private final TrackingEventMapper trackingEventMapper;
    private final MetricMaterializationMapper materializationMapper;

    MybatisPlusEventPlatformRepository(EventPlatformMapper eventPlatformMapper, EventSchemaMapper schemaMapper,
            EventFieldClassificationMapper classificationMapper, TrackingEventMapper trackingEventMapper,
            MetricMaterializationMapper materializationMapper) {
        this.eventPlatformMapper = eventPlatformMapper;
        this.schemaMapper = schemaMapper;
        this.classificationMapper = classificationMapper;
        this.trackingEventMapper = trackingEventMapper;
        this.materializationMapper = materializationMapper;
    }

    @Override
    public EventSchema saveSchema(EventSchema schema) {
        eventPlatformMapper.upsertSchema(toEntity(schema));
        return schema;
    }

    @Override
    public Optional<EventSchema> findSchema(String eventName, int version) {
        return Optional.ofNullable(schemaMapper.selectOne(new LambdaQueryWrapper<EventSchemaEntity>()
                .eq(EventSchemaEntity::getEventName, eventName).eq(EventSchemaEntity::getVersion, version)))
                .map(this::toDomain);
    }

    @Override
    public List<EventSchema> findSchemas() {
        return schemaMapper.selectList(null).stream().map(this::toDomain).toList();
    }

    @Override
    public EventFieldClassification saveFieldClassification(EventFieldClassification classification) {
        eventPlatformMapper.upsertFieldClassification(toEntity(classification));
        return classification;
    }

    @Override
    public List<EventFieldClassification> findFieldClassifications() {
        return classificationMapper.selectList(null).stream().map(this::toDomain).toList();
    }

    @Override
    public List<EventFieldClassification> findFieldClassifications(String eventName, int version) {
        return classificationMapper.selectList(new LambdaQueryWrapper<EventFieldClassificationEntity>()
                .eq(EventFieldClassificationEntity::getEventName, eventName)
                .eq(EventFieldClassificationEntity::getVersion, version)).stream().map(this::toDomain).toList();
    }

    @Override
    public TrackingEvent saveEvent(TrackingEvent event) {
        trackingEventMapper.insert(toEntity(event));
        return event;
    }

    @Override
    public boolean eventExists(String eventKey) {
        return trackingEventMapper.selectCount(new LambdaQueryWrapper<TrackingEventEntity>()
                .eq(TrackingEventEntity::getEventKey, eventKey)) > 0;
    }

    @Override
    public List<TrackingEvent> findEvents(String eventName) {
        return trackingEventMapper.selectList(new LambdaQueryWrapper<TrackingEventEntity>()
                .eq(TrackingEventEntity::getEventName, eventName)).stream().map(this::toDomain).toList();
    }

    @Override
    public List<TrackingEvent> findEvents() {
        return trackingEventMapper.selectList(null).stream().map(this::toDomain).toList();
    }

    @Override
    public PipelineOffset saveOffset(PipelineOffset offset) {
        eventPlatformMapper.upsertOffset(toEntity(offset));
        return offset;
    }

    @Override
    public MetricMaterialization saveMaterialization(MetricMaterialization materialization) {
        materializationMapper.insert(toEntity(materialization));
        return materialization;
    }

    @Override
    public List<MetricMaterialization> findMaterializations() {
        return materializationMapper.selectList(null).stream().map(this::toDomain).toList();
    }

    private EventSchemaEntity toEntity(EventSchema schema) {
        EventSchemaEntity entity = new EventSchemaEntity();
        entity.setSchemaId(schema.schemaId());
        entity.setEventName(schema.eventName());
        entity.setVersion(schema.version());
        entity.setOwner(schema.owner());
        entity.setJsonSchema(schema.jsonSchema());
        entity.setStatus(schema.status().name());
        entity.setCreatedAt(toDatabaseTime(schema.createdAt()));
        entity.setUpdatedAt(toDatabaseTime(schema.updatedAt()));
        return entity;
    }

    private EventSchema toDomain(EventSchemaEntity entity) {
        return new EventSchema(entity.getSchemaId(), entity.getEventName(), entity.getVersion(), entity.getOwner(),
                entity.getJsonSchema(), SchemaStatus.valueOf(entity.getStatus()), toDomainTime(entity.getCreatedAt()),
                toDomainTime(entity.getUpdatedAt()));
    }

    private EventFieldClassificationEntity toEntity(EventFieldClassification classification) {
        EventFieldClassificationEntity entity = new EventFieldClassificationEntity();
        entity.setClassificationId(classification.classificationId());
        entity.setEventName(classification.eventName());
        entity.setVersion(classification.version());
        entity.setFieldName(classification.fieldName());
        entity.setSensitivity(classification.sensitivity().name());
        entity.setRequired(classification.required());
        entity.setExportedToWarehouse(classification.exportedToWarehouse());
        entity.setCreatedAt(toDatabaseTime(classification.createdAt()));
        return entity;
    }

    private EventFieldClassification toDomain(EventFieldClassificationEntity entity) {
        return new EventFieldClassification(entity.getClassificationId(), entity.getEventName(), entity.getVersion(),
                entity.getFieldName(), SensitiveDataType.valueOf(entity.getSensitivity()), entity.getRequired(),
                entity.getExportedToWarehouse(), toDomainTime(entity.getCreatedAt()));
    }

    private TrackingEventEntity toEntity(TrackingEvent event) {
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

    private TrackingEvent toDomain(TrackingEventEntity entity) {
        return new TrackingEvent(entity.getEventId(), entity.getEventName(), entity.getVersion(), entity.getEventKey(),
                entity.getUserKey(), entity.getPayload(), entity.getLateEvent(), toDomainTime(entity.getOccurredAt()),
                toDomainTime(entity.getIngestedAt()));
    }

    private PipelineOffsetEntity toEntity(PipelineOffset offset) {
        PipelineOffsetEntity entity = new PipelineOffsetEntity();
        entity.setOffsetId(offset.offsetId());
        entity.setConsumerGroup(offset.consumerGroup());
        entity.setTopicName(offset.topicName());
        entity.setProcessedOffset(offset.processedOffset());
        entity.setUpdatedAt(toDatabaseTime(offset.updatedAt()));
        return entity;
    }

    private MetricMaterializationEntity toEntity(MetricMaterialization materialization) {
        MetricMaterializationEntity entity = new MetricMaterializationEntity();
        entity.setMaterializationId(materialization.materializationId());
        entity.setMetricName(materialization.metricName());
        entity.setWindowKey(materialization.windowKey());
        entity.setEventCount(materialization.eventCount());
        entity.setLateEventCount(materialization.lateEventCount());
        entity.setMaterializedAt(toDatabaseTime(materialization.materializedAt()));
        return entity;
    }

    private MetricMaterialization toDomain(MetricMaterializationEntity entity) {
        return new MetricMaterialization(entity.getMaterializationId(), entity.getMetricName(), entity.getWindowKey(),
                entity.getEventCount(), entity.getLateEventCount(), toDomainTime(entity.getMaterializedAt()));
    }

    private LocalDateTime toDatabaseTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toDomainTime(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC);
    }
}
