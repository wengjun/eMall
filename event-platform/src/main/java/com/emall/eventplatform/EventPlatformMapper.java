package com.emall.eventplatform;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
    int upsertSchema(@Param("schema") EventSchemaEntity schema);

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
    int upsertFieldClassification(@Param("classification") EventFieldClassificationEntity classification);

    @Insert("""
            INSERT INTO pipeline_offset (offset_id, consumer_group, topic_name, processed_offset, updated_at)
            VALUES (#{offset.offsetId}, #{offset.consumerGroup}, #{offset.topicName},
                #{offset.processedOffset}, #{offset.updatedAt})
            ON DUPLICATE KEY UPDATE processed_offset = VALUES(processed_offset), updated_at = VALUES(updated_at)
            """)
    int upsertOffset(@Param("offset") PipelineOffsetEntity offset);
}
