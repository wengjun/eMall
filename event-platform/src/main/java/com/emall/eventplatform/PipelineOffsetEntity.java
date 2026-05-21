package com.emall.eventplatform;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("pipeline_offset")
@Getter
@Setter
public class PipelineOffsetEntity {
    @TableId(value = "offset_id", type = IdType.INPUT)
    private Long offsetId;

    @TableField("consumer_group")
    private String consumerGroup;

    @TableField("topic_name")
    private String topicName;

    @TableField("processed_offset")
    private Long processedOffset;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
