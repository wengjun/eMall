package com.emall.eventplatform;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface EventPlatformMapper {
    @Insert("""
            INSERT INTO event_schema
                (schema_id, event_name, version, owner, json_schema, status, created_at, updated_at)
            VALUES (#{schema.schemaId}, #{schema.eventName}, #{schema.version}, #{schema.owner},
                #{schema.jsonSchema}, #{schema.status}, #{schema.createdAt}, #{schema.updatedAt})
            ON DUPLICATE KEY UPDATE json_schema = VALUES(json_schema), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveSchema(@Param("schema") EventSchema schema);

    @Select("""
            SELECT schema_id, event_name, version, owner, json_schema, status, created_at, updated_at
            FROM event_schema
            WHERE event_name = #{eventName} AND version = #{version}
            """)
    EventSchema findSchema(@Param("eventName") String eventName, @Param("version") int version);

    @Select("""
            SELECT schema_id, event_name, version, owner, json_schema, status, created_at, updated_at
            FROM event_schema
            """)
    List<EventSchema> findSchemas();

    @Insert("""
            INSERT INTO event_field_classification
                (classification_id, event_name, version, field_name, sensitivity, required, exported_to_warehouse,
                 created_at)
            VALUES (#{classification.classificationId}, #{classification.eventName}, #{classification.version},
                #{classification.fieldName}, #{classification.sensitivity}, #{classification.required},
                #{classification.exportedToWarehouse}, #{classification.createdAt})
            ON DUPLICATE KEY UPDATE sensitivity = VALUES(sensitivity), required = VALUES(required),
                exported_to_warehouse = VALUES(exported_to_warehouse)
            """)
    int saveFieldClassification(@Param("classification") EventFieldClassification classification);

    @Select("""
            SELECT classification_id, event_name, version, field_name, sensitivity, required, exported_to_warehouse,
                created_at
            FROM event_field_classification
            """)
    List<EventFieldClassification> findFieldClassifications();

    @Select("""
            SELECT classification_id, event_name, version, field_name, sensitivity, required, exported_to_warehouse,
                created_at
            FROM event_field_classification
            WHERE event_name = #{eventName} AND version = #{version}
            """)
    List<EventFieldClassification> findFieldClassificationsBySchema(@Param("eventName") String eventName,
            @Param("version") int version);

    @Insert("""
            INSERT INTO tracking_event
                (event_id, event_name, version, event_key, user_key, payload, late_event, occurred_at,
                ingested_at)
            VALUES (#{event.eventId}, #{event.eventName}, #{event.version}, #{event.eventKey}, #{event.userKey},
                #{event.payload}, #{event.lateEvent}, #{event.occurredAt}, #{event.ingestedAt})
            """)
    int saveEvent(@Param("event") TrackingEvent event);

    @Select("SELECT COUNT(*) FROM tracking_event WHERE event_key = #{eventKey}")
    long countEventsByKey(@Param("eventKey") String eventKey);

    @Select("""
            SELECT event_id, event_name, version, event_key, user_key, payload, late_event, occurred_at, ingested_at
            FROM tracking_event
            WHERE event_name = #{eventName}
            """)
    List<TrackingEvent> findEventsByName(@Param("eventName") String eventName);

    @Select("""
            SELECT event_id, event_name, version, event_key, user_key, payload, late_event, occurred_at, ingested_at
            FROM tracking_event
            """)
    List<TrackingEvent> findEvents();

    @Insert("""
            INSERT INTO pipeline_offset (offset_id, consumer_group, topic_name, processed_offset, updated_at)
            VALUES (#{offset.offsetId}, #{offset.consumerGroup}, #{offset.topicName},
                #{offset.processedOffset}, #{offset.updatedAt})
            ON DUPLICATE KEY UPDATE processed_offset = VALUES(processed_offset), updated_at = VALUES(updated_at)
            """)
    int saveOffset(@Param("offset") PipelineOffset offset);

    @Insert("""
            INSERT INTO metric_materialization
                (materialization_id, metric_name, window_key, event_count, late_event_count, materialized_at)
            VALUES (#{materialization.materializationId}, #{materialization.metricName},
                #{materialization.windowKey}, #{materialization.eventCount}, #{materialization.lateEventCount},
                #{materialization.materializedAt})
            """)
    int saveMaterialization(@Param("materialization") MetricMaterialization materialization);

    @Select("""
            SELECT materialization_id, metric_name, window_key, event_count, late_event_count, materialized_at
            FROM metric_materialization
            """)
    List<MetricMaterialization> findMaterializations();
}
