package com.emall.promotion;

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
class JdbcPromotionRepository implements PromotionRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcPromotionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PromotionCampaign saveCampaign(PromotionCampaign campaign) {
        jdbcTemplate.update("""
                INSERT INTO promotion_campaign
                    (campaign_id, name, promotion_type, threshold_amount, benefit_value, budget_amount, used_budget,
                    priority, stackable, status, starts_at, ends_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE used_budget = VALUES(used_budget), status = VALUES(status),
                    updated_at = VALUES(updated_at)
                """, campaign.campaignId(), campaign.name(), campaign.type().name(), campaign.thresholdAmount(),
                campaign.benefitValue(), campaign.budgetAmount(), campaign.usedBudget(), campaign.priority(),
                campaign.stackable(), campaign.status().name(), Timestamp.from(campaign.startsAt()),
                Timestamp.from(campaign.endsAt()), Timestamp.from(campaign.createdAt()),
                Timestamp.from(campaign.updatedAt()));
        return campaign;
    }

    @Override
    public Optional<PromotionCampaign> findCampaign(long campaignId) {
        return jdbcTemplate
                .query("SELECT * FROM promotion_campaign WHERE campaign_id = ?", this::mapCampaign, campaignId).stream()
                .findFirst();
    }

    @Override
    public List<PromotionCampaign> findActiveCampaigns() {
        return jdbcTemplate.query("""
                SELECT * FROM promotion_campaign
                WHERE status = 'ACTIVE'
                ORDER BY priority ASC
                """, this::mapCampaign);
    }

    @Override
    public List<PromotionCampaign> findCampaigns() {
        return jdbcTemplate.query("SELECT * FROM promotion_campaign ORDER BY created_at DESC", this::mapCampaign);
    }

    private PromotionCampaign mapCampaign(ResultSet rs, int rowNum) throws SQLException {
        return new PromotionCampaign(rs.getLong("campaign_id"), rs.getString("name"),
                PromotionType.valueOf(rs.getString("promotion_type")), rs.getBigDecimal("threshold_amount"),
                rs.getBigDecimal("benefit_value"), rs.getBigDecimal("budget_amount"), rs.getBigDecimal("used_budget"),
                rs.getInt("priority"), rs.getBoolean("stackable"), CampaignStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
