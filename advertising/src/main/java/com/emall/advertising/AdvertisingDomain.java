package com.emall.advertising;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

enum AdStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    FINISHED
}

@TableName("advertising_campaign")
record AdCampaign(@TableId(value = "campaign_id", type = IdType.INPUT) long campaignId, long merchantId, String name,
        BigDecimal dailyBudget, BigDecimal usedBudget, BigDecimal bidAmount, AdStatus status, Instant startsAt,
        Instant endsAt, Instant createdAt, Instant updatedAt) {
    AdCampaign changeStatus(AdStatus nextStatus) {
        return new AdCampaign(campaignId, merchantId, name, dailyBudget, usedBudget, bidAmount, nextStatus, startsAt,
                endsAt, createdAt, Instant.now());
    }

    AdCampaign consume(BigDecimal cost) {
        return new AdCampaign(campaignId, merchantId, name, dailyBudget, usedBudget.add(cost), bidAmount, status,
                startsAt, endsAt, createdAt, Instant.now());
    }
}

@TableName("advertising_creative")
record AdCreative(@TableId(value = "creative_id", type = IdType.INPUT) long creativeId, long campaignId, long skuId,
        String title, String targetUrl, boolean active) {
}

@TableName("advertising_keyword_target")
record KeywordTarget(@TableId(value = "target_id", type = IdType.INPUT) long targetId, long campaignId, String keyword,
        BigDecimal bidMultiplier, boolean active) {
}

@TableName("advertising_event")
record AdEvent(@TableId(value = "event_id", type = IdType.INPUT) long eventId, long campaignId, long creativeId,
        String eventType, BigDecimal cost, Instant occurredAt) {
}

record SponsoredItem(long campaignId, long creativeId, long skuId, BigDecimal score, String title, String targetUrl) {
}

record SponsoredResult(String keyword, List<SponsoredItem> items) {
}
