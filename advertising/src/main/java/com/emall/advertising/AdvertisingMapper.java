package com.emall.advertising;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
interface AdvertisingMapper {
    @Insert("""
            INSERT INTO advertising_campaign
                (campaign_id, merchant_id, name, daily_budget, used_budget, bid_amount, status, starts_at,
                ends_at, created_at, updated_at)
            VALUES (#{campaign.campaignId}, #{campaign.merchantId}, #{campaign.name}, #{campaign.dailyBudget},
                #{campaign.usedBudget}, #{campaign.bidAmount}, #{campaign.status}, #{campaign.startsAt},
                #{campaign.endsAt}, #{campaign.createdAt}, #{campaign.updatedAt})
            ON DUPLICATE KEY UPDATE used_budget = VALUES(used_budget), status = VALUES(status),
                updated_at = VALUES(updated_at)
            """)
    int saveCampaign(@Param("campaign") AdCampaign campaign);

    @Insert("""
            INSERT INTO advertising_creative (creative_id, campaign_id, sku_id, title, target_url, active)
            VALUES (#{creative.creativeId}, #{creative.campaignId}, #{creative.skuId}, #{creative.title},
                #{creative.targetUrl}, #{creative.active})
            ON DUPLICATE KEY UPDATE title = VALUES(title), target_url = VALUES(target_url),
                active = VALUES(active)
            """)
    int saveCreative(@Param("creative") AdCreative creative);

    @Insert("""
            INSERT INTO advertising_keyword_target (target_id, campaign_id, keyword, bid_multiplier, active)
            VALUES (#{target.targetId}, #{target.campaignId}, #{target.keyword}, #{target.bidMultiplier},
                #{target.active})
            ON DUPLICATE KEY UPDATE bid_multiplier = VALUES(bid_multiplier), active = VALUES(active)
            """)
    int saveTarget(@Param("target") KeywordTarget target);
}
