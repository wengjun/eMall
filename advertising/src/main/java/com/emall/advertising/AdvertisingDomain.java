package com.emall.advertising;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

enum AdStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    FINISHED
}

record AdCampaign(long campaignId, long merchantId, String name, BigDecimal dailyBudget, BigDecimal usedBudget,
                  BigDecimal bidAmount, AdStatus status, Instant startsAt, Instant endsAt,
                  Instant createdAt, Instant updatedAt) {
    AdCampaign changeStatus(AdStatus nextStatus) {
        return new AdCampaign(campaignId, merchantId, name, dailyBudget, usedBudget, bidAmount, nextStatus,
                startsAt, endsAt, createdAt, Instant.now());
    }

    AdCampaign consume(BigDecimal cost) {
        return new AdCampaign(campaignId, merchantId, name, dailyBudget, usedBudget.add(cost), bidAmount, status,
                startsAt, endsAt, createdAt, Instant.now());
    }
}

record AdCreative(long creativeId, long campaignId, long skuId, String title, String targetUrl, boolean active) {
}

record KeywordTarget(long targetId, long campaignId, String keyword, BigDecimal bidMultiplier, boolean active) {
}

record AdEvent(long eventId, long campaignId, long creativeId, String eventType, BigDecimal cost, Instant occurredAt) {
}

record SponsoredItem(long campaignId, long creativeId, long skuId, BigDecimal score, String title, String targetUrl) {
}

record SponsoredResult(String keyword, List<SponsoredItem> items) {
}
