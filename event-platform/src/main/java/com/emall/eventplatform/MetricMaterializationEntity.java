package com.emall.eventplatform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("metric_materialization")
@Getter
@Setter
public class MetricMaterializationEntity {
    @TableId(value = "materialization_id", type = IdType.INPUT)
    private Long materializationId;

    @TableField("metric_name")
    private String metricName;

    @TableField("window_key")
    private String windowKey;

    @TableField("event_count")
    private Long eventCount;

    @TableField("late_event_count")
    private Long lateEventCount;

    @TableField("materialized_at")
    private LocalDateTime materializedAt;
}
