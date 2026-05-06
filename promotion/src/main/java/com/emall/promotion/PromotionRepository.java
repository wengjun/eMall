package com.emall.promotion;

import java.util.List;
import java.util.Optional;

interface PromotionRepository {
    PromotionCampaign saveCampaign(PromotionCampaign campaign);

    Optional<PromotionCampaign> findCampaign(long campaignId);

    List<PromotionCampaign> findActiveCampaigns();

    List<PromotionCampaign> findCampaigns();
}
