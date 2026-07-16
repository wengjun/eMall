package com.emall.common.messaging;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("consumed_aggregate_version")
public class AggregateVersionRecord {
    @TableId(value = "consumer_aggregate_id", type = IdType.INPUT)
    private String consumerAggregateId;
    private String consumerName;
    private String aggregateType;
    private String aggregateId;
    private Long aggregateVersion;
    private String eventId;
    private LocalDateTime updatedAt;
}
