package com.emall.promotion;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class MybatisPlusPromotionRepository implements PromotionRepository {
    private final PromotionMapper promotionMapper;

    MybatisPlusPromotionRepository(PromotionMapper promotionMapper) {
        this.promotionMapper = promotionMapper;
    }

    @Override
    public PromotionCampaign saveCampaign(PromotionCampaign campaign) {
        promotionMapper.saveCampaign(campaign);
        return campaign;
    }

    @Override
    public Optional<PromotionCampaign> findCampaign(long campaignId) {
        return Optional.ofNullable(promotionMapper.findCampaign(campaignId));
    }

    @Override
    public List<PromotionCampaign> findActiveCampaigns() {
        return promotionMapper.findActiveCampaigns();
    }

    @Override
    public List<PromotionCampaign> findCampaigns() {
        return promotionMapper.findCampaigns();
    }
}
