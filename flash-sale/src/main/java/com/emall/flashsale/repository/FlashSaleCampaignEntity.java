package com.emall.flashsale.repository;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@TableName("flash_sale_campaign")
@Getter
@Setter
public class FlashSaleCampaignEntity {
    @TableId(value = "campaign_id", type = IdType.INPUT)
    private Long campaignId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("name")
    private String name;

    @TableField("starts_at")
    private LocalDateTime startsAt;

    @TableField("ends_at")
    private LocalDateTime endsAt;

    @TableField("per_user_limit")
    private Integer perUserLimit;

    @TableField("token_ttl_seconds")
    private Integer tokenTtlSeconds;

    @TableField("queue_capacity")
    private Integer queueCapacity;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
