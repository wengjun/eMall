package com.emall.common.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("outbox_event")
@Getter
@Setter
public class OutboxEventRecord {
    @TableId(value = "event_id", type = IdType.INPUT)
    private String eventId;

    @TableField("aggregate_type")
    private String aggregateType;

    @TableField("aggregate_id")
    private String aggregateId;

    @TableField("event_type")
    private String eventType;

    @TableField("shard_id")
    private Integer shardId;

    @TableField("payload")
    private String payload;

    @TableField("status")
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("next_retry_at")
    private LocalDateTime nextRetryAt;

    @TableField("claimed_by")
    private String claimedBy;

    @TableField("claimed_until")
    private LocalDateTime claimedUntil;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("error_code")
    private String errorCode;

    @TableField("last_error")
    private String lastError;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
