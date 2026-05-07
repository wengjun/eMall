package com.emall.advertising;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
class JdbcAdvertisingRepository implements AdvertisingRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcAdvertisingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AdCampaign saveCampaign(AdCampaign campaign) {
        jdbcTemplate.update("""
                INSERT INTO advertising_campaign
                    (campaign_id, merchant_id, name, daily_budget, used_budget, bid_amount, status, starts_at,
                    ends_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE used_budget = VALUES(used_budget), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, campaign.campaignId(), campaign.merchantId(), campaign.name(), campaign.dailyBudget(),
                campaign.usedBudget(), campaign.bidAmount(), campaign.status().name(),
                Timestamp.from(campaign.startsAt()), Timestamp.from(campaign.endsAt()),
                Timestamp.from(campaign.createdAt()), Timestamp.from(campaign.updatedAt()));
        return campaign;
    }

    @Override
    public Optional<AdCampaign> findCampaign(long campaignId) {
        return jdbcTemplate
                .query("SELECT * FROM advertising_campaign WHERE campaign_id = ?", this::mapCampaign, campaignId)
                .stream().findFirst();
    }

    @Override
    public AdCreative saveCreative(AdCreative creative) {
        jdbcTemplate.update("""
                INSERT INTO advertising_creative (creative_id, campaign_id, sku_id, title, target_url, active)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE title = VALUES(title), target_url = VALUES(target_url),
                    active = VALUES(active)
                """, creative.creativeId(), creative.campaignId(), creative.skuId(), creative.title(),
                creative.targetUrl(), creative.active());
        return creative;
    }

    @Override
    public List<AdCreative> findCreatives(long campaignId) {
        return jdbcTemplate.query("""
                SELECT * FROM advertising_creative
                WHERE campaign_id = ? AND active = TRUE
                """, this::mapCreative, campaignId);
    }

    @Override
    public KeywordTarget saveTarget(KeywordTarget target) {
        jdbcTemplate.update("""
                INSERT INTO advertising_keyword_target (target_id, campaign_id, keyword, bid_multiplier, active)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE bid_multiplier = VALUES(bid_multiplier), active = VALUES(active)
                """, target.targetId(), target.campaignId(), target.keyword(), target.bidMultiplier(), target.active());
        return target;
    }

    @Override
    public List<KeywordTarget> findTargets(String keyword) {
        return jdbcTemplate.query("""
                SELECT * FROM advertising_keyword_target
                WHERE keyword = ? AND active = TRUE
                """, this::mapTarget, keyword);
    }

    @Override
    public AdEvent saveEvent(AdEvent event) {
        jdbcTemplate.update("""
                INSERT INTO advertising_event (event_id, campaign_id, creative_id, event_type, cost, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, event.eventId(), event.campaignId(), event.creativeId(), event.eventType(), event.cost(),
                Timestamp.from(event.occurredAt()));
        return event;
    }

    private AdCampaign mapCampaign(ResultSet rs, int rowNum) throws SQLException {
        return new AdCampaign(rs.getLong("campaign_id"), rs.getLong("merchant_id"), rs.getString("name"),
                rs.getBigDecimal("daily_budget"), rs.getBigDecimal("used_budget"), rs.getBigDecimal("bid_amount"),
                AdStatus.valueOf(rs.getString("status")), rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at").toInstant(), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private AdCreative mapCreative(ResultSet rs, int rowNum) throws SQLException {
        return new AdCreative(rs.getLong("creative_id"), rs.getLong("campaign_id"), rs.getLong("sku_id"),
                rs.getString("title"), rs.getString("target_url"), rs.getBoolean("active"));
    }

    private KeywordTarget mapTarget(ResultSet rs, int rowNum) throws SQLException {
        return new KeywordTarget(rs.getLong("target_id"), rs.getLong("campaign_id"), rs.getString("keyword"),
                rs.getBigDecimal("bid_multiplier"), rs.getBoolean("active"));
    }
}
