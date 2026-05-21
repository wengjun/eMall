package com.emall.promotion;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

enum CampaignStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    FINISHED
}

enum PromotionType {
    AMOUNT_OFF,
    PERCENT_OFF,
    GIFT,
    BUNDLE,
    COUPON_PACKAGE
}

@TableName("promotion_campaign")
record PromotionCampaign(@TableId(value = "campaign_id", type = IdType.INPUT) long campaignId, String name,
        @TableField("promotion_type") PromotionType type, BigDecimal thresholdAmount, BigDecimal benefitValue,
        BigDecimal budgetAmount, BigDecimal usedBudget, int priority, boolean stackable, CampaignStatus status,
        Instant startsAt, Instant endsAt, Instant createdAt, Instant updatedAt) {
    PromotionCampaign changeStatus(CampaignStatus nextStatus) {
        return new PromotionCampaign(campaignId, name, type, thresholdAmount, benefitValue, budgetAmount, usedBudget,
                priority, stackable, nextStatus, startsAt, endsAt, createdAt, Instant.now());
    }

    PromotionCampaign consume(BigDecimal amount) {
        return new PromotionCampaign(campaignId, name, type, thresholdAmount, benefitValue, budgetAmount,
                usedBudget.add(amount), priority, stackable, status, startsAt, endsAt, createdAt, Instant.now());
    }
}

record PromotionQuote(long userId, BigDecimal orderAmount, BigDecimal discountAmount, BigDecimal payableAmount,
        List<Long> campaignIds) {
}

record CampaignCalendar(String month, int activeCampaigns, BigDecimal totalBudget, BigDecimal usedBudget) {
}
