package com.emall.eventplatform;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("event_schema")
@Getter
@Setter
public class EventSchemaEntity {
    @TableId(value = "schema_id", type = IdType.INPUT)
    private Long schemaId;

    @TableField("event_name")
    private String eventName;

    @TableField("version")
    private Integer version;

    @TableField("owner")
    private String owner;

    @TableField("json_schema")
    private String jsonSchema;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
