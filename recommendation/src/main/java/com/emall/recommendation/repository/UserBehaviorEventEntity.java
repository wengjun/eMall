package com.emall.recommendation.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("recommendation_behavior_event")
@Getter
@Setter
public class UserBehaviorEventEntity {
    @TableId(value = "event_id", type = IdType.INPUT)
    private Long eventId;

    @TableField("user_id")
    private Long userId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("behavior_type")
    private String behaviorType;

    @TableField("weight")
    private Integer weight;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;
}
