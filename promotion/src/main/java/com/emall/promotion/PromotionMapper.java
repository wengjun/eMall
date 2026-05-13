package com.emall.promotion;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface PromotionMapper {
    @Insert("""
            INSERT INTO promotion_campaign
                (campaign_id, name, promotion_type, threshold_amount, benefit_value, budget_amount, used_budget,
                priority, stackable, status, starts_at, ends_at, created_at, updated_at)
            VALUES (#{campaign.campaignId}, #{campaign.name}, #{campaign.type}, #{campaign.thresholdAmount},
                #{campaign.benefitValue}, #{campaign.budgetAmount}, #{campaign.usedBudget}, #{campaign.priority},
                #{campaign.stackable}, #{campaign.status}, #{campaign.startsAt}, #{campaign.endsAt},
                #{campaign.createdAt}, #{campaign.updatedAt})
            ON DUPLICATE KEY UPDATE used_budget = VALUES(used_budget), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveCampaign(@Param("campaign") PromotionCampaign campaign);

    @Select("""
            SELECT campaign_id, name, promotion_type, threshold_amount, benefit_value, budget_amount, used_budget,
                priority, stackable, status, starts_at, ends_at, created_at, updated_at
            FROM promotion_campaign
            WHERE campaign_id = #{campaignId}
            """)
    PromotionCampaign findCampaign(@Param("campaignId") long campaignId);

    @Select("""
            SELECT campaign_id, name, promotion_type, threshold_amount, benefit_value, budget_amount, used_budget,
                priority, stackable, status, starts_at, ends_at, created_at, updated_at
            FROM promotion_campaign
            WHERE status = 'ACTIVE'
            ORDER BY priority ASC
            """)
    List<PromotionCampaign> findActiveCampaigns();

    @Select("""
            SELECT campaign_id, name, promotion_type, threshold_amount, benefit_value, budget_amount, used_budget,
                priority, stackable, status, starts_at, ends_at, created_at, updated_at
            FROM promotion_campaign
            ORDER BY created_at DESC
            """)
    List<PromotionCampaign> findCampaigns();
}
