package com.emall.promotion;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
        return Optional.ofNullable(promotionMapper.selectById(campaignId));
    }

    @Override
    public List<PromotionCampaign> findActiveCampaigns() {
        QueryWrapper<PromotionCampaign> query =
                new QueryWrapper<PromotionCampaign>().eq("status", CampaignStatus.ACTIVE.name()).orderByAsc("priority");
        return promotionMapper.selectList(query);
    }

    @Override
    public List<PromotionCampaign> findCampaigns() {
        return promotionMapper.selectList(new QueryWrapper<PromotionCampaign>().orderByDesc("created_at"));
    }
}
