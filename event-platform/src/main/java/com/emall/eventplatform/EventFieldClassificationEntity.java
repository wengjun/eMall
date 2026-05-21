package com.emall.eventplatform;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("event_field_classification")
@Getter
@Setter
public class EventFieldClassificationEntity {
    @TableId(value = "classification_id", type = IdType.INPUT)
    private Long classificationId;

    @TableField("event_name")
    private String eventName;

    @TableField("version")
    private Integer version;

    @TableField("field_name")
    private String fieldName;

    @TableField("sensitivity")
    private String sensitivity;

    @TableField("required")
    private Boolean required;

    @TableField("exported_to_warehouse")
    private Boolean exportedToWarehouse;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
