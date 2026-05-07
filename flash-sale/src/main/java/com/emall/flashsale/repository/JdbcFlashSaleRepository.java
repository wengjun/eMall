package com.emall.flashsale.repository;

import com.emall.flashsale.domain.CampaignStatus;
import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleRequestStatus;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
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
public class JdbcFlashSaleRepository implements FlashSaleRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcFlashSaleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FlashSaleCampaign saveCampaign(FlashSaleCampaign campaign) {
        jdbcTemplate.update("""
                INSERT INTO flash_sale_campaign
                    (campaign_id, sku_id, name, starts_at, ends_at, per_user_limit, token_ttl_seconds,
                    queue_capacity, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), starts_at = VALUES(starts_at),
                    ends_at = VALUES(ends_at), per_user_limit = VALUES(per_user_limit),
                    token_ttl_seconds = VALUES(token_ttl_seconds), queue_capacity = VALUES(queue_capacity),
                    status = VALUES(status), updated_at = VALUES(updated_at)
                """, campaign.campaignId(), campaign.skuId(), campaign.name(), Timestamp.from(campaign.startsAt()),
                Timestamp.from(campaign.endsAt()), campaign.perUserLimit(), campaign.tokenTtlSeconds(),
                campaign.queueCapacity(), campaign.status().name(), Timestamp.from(campaign.createdAt()),
                Timestamp.from(campaign.updatedAt()));
        return campaign;
    }

    @Override
    public Optional<FlashSaleCampaign> findCampaign(long campaignId) {
        return jdbcTemplate
                .query("SELECT * FROM flash_sale_campaign WHERE campaign_id = ?", this::mapCampaign, campaignId)
                .stream().findFirst();
    }

    @Override
    public FlashSaleStock saveStock(FlashSaleStock stock) {
        jdbcTemplate.update("""
                INSERT INTO flash_sale_stock
                    (campaign_id, sku_id, total_stock, available_stock, token_reserved_stock, queued_stock,
                    sold_stock, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE total_stock = VALUES(total_stock),
                    available_stock = VALUES(available_stock), token_reserved_stock = VALUES(token_reserved_stock),
                    queued_stock = VALUES(queued_stock), sold_stock = VALUES(sold_stock),
                    updated_at = VALUES(updated_at)
                """, stock.campaignId(), stock.skuId(), stock.totalStock(), stock.availableStock(),
                stock.tokenReservedStock(), stock.queuedStock(), stock.soldStock(), Timestamp.from(stock.updatedAt()));
        return stock;
    }

    @Override
    public Optional<FlashSaleStock> findStock(long campaignId) {
        return jdbcTemplate.query("SELECT * FROM flash_sale_stock WHERE campaign_id = ?", this::mapStock, campaignId)
                .stream().findFirst();
    }

    @Override
    public boolean reserveTokenStock(long campaignId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE flash_sale_stock
                SET available_stock = available_stock - ?,
                    token_reserved_stock = token_reserved_stock + ?,
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE campaign_id = ? AND available_stock >= ?
                """, quantity, quantity, campaignId, quantity);
        return updated == 1;
    }

    @Override
    public boolean moveTokenStockToQueue(long campaignId, int quantity) {
        int updated = jdbcTemplate.update("""
                UPDATE flash_sale_stock
                SET token_reserved_stock = token_reserved_stock - ?,
                    queued_stock = queued_stock + ?,
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE campaign_id = ? AND token_reserved_stock >= ?
                """, quantity, quantity, campaignId, quantity);
        return updated == 1;
    }

    @Override
    public FlashSaleToken saveToken(FlashSaleToken token) {
        jdbcTemplate.update("""
                INSERT INTO flash_sale_token
                    (token_id, campaign_id, user_id, sku_id, quantity, token, expires_at, used, created_at,
                    updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE used = VALUES(used), updated_at = VALUES(updated_at)
                """, token.tokenId(), token.campaignId(), token.userId(), token.skuId(), token.quantity(),
                token.token(), Timestamp.from(token.expiresAt()), token.used(), Timestamp.from(token.createdAt()),
                Timestamp.from(token.updatedAt()));
        return token;
    }

    @Override
    public Optional<FlashSaleToken> findToken(String token) {
        return jdbcTemplate.query("SELECT * FROM flash_sale_token WHERE token = ?", this::mapToken, token).stream()
                .findFirst();
    }

    @Override
    public int countTokensByUser(long campaignId, long userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flash_sale_token
                WHERE campaign_id = ? AND user_id = ?
                """, Integer.class, campaignId, userId);
        return count == null ? 0 : count;
    }

    @Override
    public FlashSaleOrderRequest saveOrderRequest(FlashSaleOrderRequest request) {
        jdbcTemplate.update("""
                INSERT INTO flash_sale_order_request
                    (request_id, campaign_id, user_id, sku_id, quantity, token, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, request.requestId(), request.campaignId(), request.userId(), request.skuId(), request.quantity(),
                request.token(), request.status().name(), Timestamp.from(request.createdAt()),
                Timestamp.from(request.updatedAt()));
        return request;
    }

    @Override
    public Optional<FlashSaleOrderRequest> findOrderRequest(long requestId) {
        return jdbcTemplate
                .query("SELECT * FROM flash_sale_order_request WHERE request_id = ?", this::mapOrderRequest, requestId)
                .stream().findFirst();
    }

    @Override
    public List<FlashSaleOrderRequest> findQueuedRequests(long campaignId, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM flash_sale_order_request
                WHERE campaign_id = ? AND status = 'QUEUED'
                ORDER BY created_at ASC, request_id ASC
                LIMIT ?
                """, this::mapOrderRequest, campaignId, limit);
    }

    @Override
    public int countQueuedRequests(long campaignId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flash_sale_order_request
                WHERE campaign_id = ? AND status = 'QUEUED'
                """, Integer.class, campaignId);
        return count == null ? 0 : count;
    }

    private FlashSaleCampaign mapCampaign(ResultSet rs, int rowNum) throws SQLException {
        return new FlashSaleCampaign(rs.getLong("campaign_id"), rs.getLong("sku_id"), rs.getString("name"),
                rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at").toInstant(),
                rs.getInt("per_user_limit"), rs.getInt("token_ttl_seconds"), rs.getInt("queue_capacity"),
                CampaignStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private FlashSaleStock mapStock(ResultSet rs, int rowNum) throws SQLException {
        return new FlashSaleStock(rs.getLong("campaign_id"), rs.getLong("sku_id"), rs.getInt("total_stock"),
                rs.getInt("available_stock"), rs.getInt("token_reserved_stock"), rs.getInt("queued_stock"),
                rs.getInt("sold_stock"), rs.getTimestamp("updated_at").toInstant());
    }

    private FlashSaleToken mapToken(ResultSet rs, int rowNum) throws SQLException {
        return new FlashSaleToken(rs.getLong("token_id"), rs.getLong("campaign_id"), rs.getLong("user_id"),
                rs.getLong("sku_id"), rs.getInt("quantity"), rs.getString("token"),
                rs.getTimestamp("expires_at").toInstant(), rs.getBoolean("used"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private FlashSaleOrderRequest mapOrderRequest(ResultSet rs, int rowNum) throws SQLException {
        return new FlashSaleOrderRequest(rs.getLong("request_id"), rs.getLong("campaign_id"), rs.getLong("user_id"),
                rs.getLong("sku_id"), rs.getInt("quantity"), rs.getString("token"),
                FlashSaleRequestStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
