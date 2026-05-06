package com.emall.promotion;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "memory")
class InMemoryPromotionRepository implements PromotionRepository {
    private final ConcurrentMap<Long, PromotionCampaign> campaigns = new ConcurrentHashMap<>();

    @Override
    public PromotionCampaign saveCampaign(PromotionCampaign campaign) {
        campaigns.put(campaign.campaignId(), campaign);
        return campaign;
    }

    @Override
    public Optional<PromotionCampaign> findCampaign(long campaignId) {
        return Optional.ofNullable(campaigns.get(campaignId));
    }

    @Override
    public List<PromotionCampaign> findActiveCampaigns() {
        return campaigns.values().stream()
                .filter(campaign -> campaign.status() == CampaignStatus.ACTIVE)
                .sorted(Comparator.comparingInt(PromotionCampaign::priority))
                .toList();
    }

    @Override
    public List<PromotionCampaign> findCampaigns() {
        return campaigns.values().stream()
                .sorted(Comparator.comparing(PromotionCampaign::createdAt).reversed())
                .toList();
    }
}
