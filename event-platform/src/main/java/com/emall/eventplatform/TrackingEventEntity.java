package com.emall.eventplatform;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("tracking_event")
@Getter
@Setter
public class TrackingEventEntity {
    @TableId(value = "event_id", type = IdType.INPUT)
    private Long eventId;

    @TableField("event_name")
    private String eventName;

    @TableField("version")
    private Integer version;

    @TableField("event_key")
    private String eventKey;

    @TableField("user_key")
    private String userKey;

    @TableField("payload")
    private String payload;

    @TableField("late_event")
    private Boolean lateEvent;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("ingested_at")
    private LocalDateTime ingestedAt;
}
