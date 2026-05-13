package com.emall.promotion;

import static com.emall.common.persistence.RowMaps.booleanValue;
import static com.emall.common.persistence.RowMaps.decimalValue;
import static com.emall.common.persistence.RowMaps.instantValue;
import static com.emall.common.persistence.RowMaps.intValue;
import static com.emall.common.persistence.RowMaps.longValue;
import static com.emall.common.persistence.RowMaps.stringValue;

import java.util.List;
import java.util.Map;
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
        return Optional.ofNullable(promotionMapper.findCampaign(campaignId)).map(this::mapCampaign);
    }

    @Override
    public List<PromotionCampaign> findActiveCampaigns() {
        return promotionMapper.findActiveCampaigns().stream().map(this::mapCampaign).toList();
    }

    @Override
    public List<PromotionCampaign> findCampaigns() {
        return promotionMapper.findCampaigns().stream().map(this::mapCampaign).toList();
    }

    private PromotionCampaign mapCampaign(Map<String, Object> row) {
        return new PromotionCampaign(longValue(row, "campaign_id"), stringValue(row, "name"),
                PromotionType.valueOf(stringValue(row, "promotion_type")), decimalValue(row, "threshold_amount"),
                decimalValue(row, "benefit_value"), decimalValue(row, "budget_amount"),
                decimalValue(row, "used_budget"), intValue(row, "priority"), booleanValue(row, "stackable"),
                CampaignStatus.valueOf(stringValue(row, "status")), instantValue(row, "starts_at"),
                instantValue(row, "ends_at"), instantValue(row, "created_at"), instantValue(row, "updated_at"));
    }
}
