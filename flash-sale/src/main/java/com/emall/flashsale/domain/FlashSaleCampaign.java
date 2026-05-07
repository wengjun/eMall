package com.emall.flashsale.domain;

import java.time.Instant;

public record FlashSaleCampaign(long campaignId, long skuId, String name, Instant startsAt, Instant endsAt,
        int perUserLimit, int tokenTtlSeconds, int queueCapacity, CampaignStatus status, Instant createdAt,
        Instant updatedAt) {
    public FlashSaleCampaign changeStatus(CampaignStatus newStatus) {
        return new FlashSaleCampaign(campaignId, skuId, name, startsAt, endsAt, perUserLimit, tokenTtlSeconds,
                queueCapacity, newStatus, createdAt, Instant.now());
    }

    public boolean isOpenAt(Instant now) {
        return status == CampaignStatus.ACTIVE && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }
}
