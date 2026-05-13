package com.emall.flashsale.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.emall.flashsale.domain.CampaignStatus;
import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleRequestStatus;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "emall.storage", havingValue = "jdbc", matchIfMissing = true)
public class MybatisPlusFlashSaleRepository implements FlashSaleRepository {
    private final FlashSaleCampaignMapper campaignMapper;
    private final FlashSaleStockMapper stockMapper;
    private final FlashSaleTokenMapper tokenMapper;
    private final FlashSaleOrderRequestMapper orderRequestMapper;

    public MybatisPlusFlashSaleRepository(FlashSaleCampaignMapper campaignMapper, FlashSaleStockMapper stockMapper,
            FlashSaleTokenMapper tokenMapper, FlashSaleOrderRequestMapper orderRequestMapper) {
        this.campaignMapper = campaignMapper;
        this.stockMapper = stockMapper;
        this.tokenMapper = tokenMapper;
        this.orderRequestMapper = orderRequestMapper;
    }

    @Override
    public FlashSaleCampaign saveCampaign(FlashSaleCampaign campaign) {
        FlashSaleCampaignEntity entity = toEntity(campaign);
        try {
            campaignMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            campaignMapper.update(null, new UpdateWrapper<FlashSaleCampaignEntity>()
                    .set("name", entity.getName())
                    .set("starts_at", entity.getStartsAt())
                    .set("ends_at", entity.getEndsAt())
                    .set("per_user_limit", entity.getPerUserLimit())
                    .set("token_ttl_seconds", entity.getTokenTtlSeconds())
                    .set("queue_capacity", entity.getQueueCapacity())
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("campaign_id", entity.getCampaignId()));
        }
        return campaign;
    }

    @Override
    public Optional<FlashSaleCampaign> findCampaign(long campaignId) {
        return Optional.ofNullable(campaignMapper.selectById(campaignId)).map(this::toDomain);
    }

    @Override
    public FlashSaleStock saveStock(FlashSaleStock stock) {
        FlashSaleStockEntity entity = toEntity(stock);
        try {
            stockMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            stockMapper.update(null, new UpdateWrapper<FlashSaleStockEntity>()
                    .set("total_stock", entity.getTotalStock())
                    .set("available_stock", entity.getAvailableStock())
                    .set("token_reserved_stock", entity.getTokenReservedStock())
                    .set("queued_stock", entity.getQueuedStock())
                    .set("sold_stock", entity.getSoldStock())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("campaign_id", entity.getCampaignId()));
        }
        return stock;
    }

    @Override
    public Optional<FlashSaleStock> findStock(long campaignId) {
        return Optional.ofNullable(stockMapper.selectById(campaignId)).map(this::toDomain);
    }

    @Override
    public boolean reserveTokenStock(long campaignId, int quantity) {
        int updated = stockMapper.update(null, new UpdateWrapper<FlashSaleStockEntity>()
                .setSql("available_stock = available_stock - {0}", quantity)
                .setSql("token_reserved_stock = token_reserved_stock + {0}", quantity)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC))
                .eq("campaign_id", campaignId)
                .ge("available_stock", quantity));
        return updated == 1;
    }

    @Override
    public boolean moveTokenStockToQueue(long campaignId, int quantity) {
        int updated = stockMapper.update(null, new UpdateWrapper<FlashSaleStockEntity>()
                .setSql("token_reserved_stock = token_reserved_stock - {0}", quantity)
                .setSql("queued_stock = queued_stock + {0}", quantity)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC))
                .eq("campaign_id", campaignId)
                .ge("token_reserved_stock", quantity));
        return updated == 1;
    }

    @Override
    public FlashSaleToken saveToken(FlashSaleToken token) {
        FlashSaleTokenEntity entity = toEntity(token);
        try {
            tokenMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            tokenMapper.update(null, new UpdateWrapper<FlashSaleTokenEntity>()
                    .set("used", entity.getUsed())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("token_id", entity.getTokenId()));
        }
        return token;
    }

    @Override
    public Optional<FlashSaleToken> findToken(String token) {
        return Optional.ofNullable(tokenMapper.selectOne(
                new QueryWrapper<FlashSaleTokenEntity>().eq("token", token))).map(this::toDomain);
    }

    @Override
    public int countTokensByUser(long campaignId, long userId) {
        Long count = tokenMapper.selectCount(new QueryWrapper<FlashSaleTokenEntity>()
                .eq("campaign_id", campaignId)
                .eq("user_id", userId));
        return count.intValue();
    }

    @Override
    public FlashSaleOrderRequest saveOrderRequest(FlashSaleOrderRequest request) {
        FlashSaleOrderRequestEntity entity = toEntity(request);
        try {
            orderRequestMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            orderRequestMapper.update(null, new UpdateWrapper<FlashSaleOrderRequestEntity>()
                    .set("status", entity.getStatus())
                    .set("updated_at", entity.getUpdatedAt())
                    .eq("request_id", entity.getRequestId()));
        }
        return request;
    }

    @Override
    public Optional<FlashSaleOrderRequest> findOrderRequest(long requestId) {
        return Optional.ofNullable(orderRequestMapper.selectById(requestId)).map(this::toDomain);
    }

    @Override
    public List<FlashSaleOrderRequest> findQueuedRequests(long campaignId, int limit) {
        return orderRequestMapper.selectList(new QueryWrapper<FlashSaleOrderRequestEntity>()
                .eq("campaign_id", campaignId)
                .eq("status", FlashSaleRequestStatus.QUEUED.name())
                .orderByAsc("created_at", "request_id")
                .last("LIMIT " + limit)).stream().map(this::toDomain).toList();
    }

    @Override
    public int countQueuedRequests(long campaignId) {
        Long count = orderRequestMapper.selectCount(new QueryWrapper<FlashSaleOrderRequestEntity>()
                .eq("campaign_id", campaignId)
                .eq("status", FlashSaleRequestStatus.QUEUED.name()));
        return count.intValue();
    }

    private FlashSaleCampaignEntity toEntity(FlashSaleCampaign campaign) {
        FlashSaleCampaignEntity entity = new FlashSaleCampaignEntity();
        entity.setCampaignId(campaign.campaignId());
        entity.setSkuId(campaign.skuId());
        entity.setName(campaign.name());
        entity.setStartsAt(LocalDateTime.ofInstant(campaign.startsAt(), ZoneOffset.UTC));
        entity.setEndsAt(LocalDateTime.ofInstant(campaign.endsAt(), ZoneOffset.UTC));
        entity.setPerUserLimit(campaign.perUserLimit());
        entity.setTokenTtlSeconds(campaign.tokenTtlSeconds());
        entity.setQueueCapacity(campaign.queueCapacity());
        entity.setStatus(campaign.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(campaign.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(campaign.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private FlashSaleCampaign toDomain(FlashSaleCampaignEntity entity) {
        return new FlashSaleCampaign(entity.getCampaignId(), entity.getSkuId(), entity.getName(),
                entity.getStartsAt().toInstant(ZoneOffset.UTC), entity.getEndsAt().toInstant(ZoneOffset.UTC),
                entity.getPerUserLimit(), entity.getTokenTtlSeconds(), entity.getQueueCapacity(),
                CampaignStatus.valueOf(entity.getStatus()), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private FlashSaleStockEntity toEntity(FlashSaleStock stock) {
        FlashSaleStockEntity entity = new FlashSaleStockEntity();
        entity.setCampaignId(stock.campaignId());
        entity.setSkuId(stock.skuId());
        entity.setTotalStock(stock.totalStock());
        entity.setAvailableStock(stock.availableStock());
        entity.setTokenReservedStock(stock.tokenReservedStock());
        entity.setQueuedStock(stock.queuedStock());
        entity.setSoldStock(stock.soldStock());
        entity.setUpdatedAt(LocalDateTime.ofInstant(stock.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private FlashSaleStock toDomain(FlashSaleStockEntity entity) {
        return new FlashSaleStock(entity.getCampaignId(), entity.getSkuId(), entity.getTotalStock(),
                entity.getAvailableStock(), entity.getTokenReservedStock(), entity.getQueuedStock(),
                entity.getSoldStock(), entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private FlashSaleTokenEntity toEntity(FlashSaleToken token) {
        FlashSaleTokenEntity entity = new FlashSaleTokenEntity();
        entity.setTokenId(token.tokenId());
        entity.setCampaignId(token.campaignId());
        entity.setUserId(token.userId());
        entity.setSkuId(token.skuId());
        entity.setQuantity(token.quantity());
        entity.setToken(token.token());
        entity.setExpiresAt(LocalDateTime.ofInstant(token.expiresAt(), ZoneOffset.UTC));
        entity.setUsed(token.used());
        entity.setCreatedAt(LocalDateTime.ofInstant(token.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(token.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private FlashSaleToken toDomain(FlashSaleTokenEntity entity) {
        return new FlashSaleToken(entity.getTokenId(), entity.getCampaignId(), entity.getUserId(), entity.getSkuId(),
                entity.getQuantity(), entity.getToken(), entity.getExpiresAt().toInstant(ZoneOffset.UTC),
                entity.getUsed(), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    private FlashSaleOrderRequestEntity toEntity(FlashSaleOrderRequest request) {
        FlashSaleOrderRequestEntity entity = new FlashSaleOrderRequestEntity();
        entity.setRequestId(request.requestId());
        entity.setCampaignId(request.campaignId());
        entity.setUserId(request.userId());
        entity.setSkuId(request.skuId());
        entity.setQuantity(request.quantity());
        entity.setToken(request.token());
        entity.setStatus(request.status().name());
        entity.setCreatedAt(LocalDateTime.ofInstant(request.createdAt(), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(request.updatedAt(), ZoneOffset.UTC));
        return entity;
    }

    private FlashSaleOrderRequest toDomain(FlashSaleOrderRequestEntity entity) {
        return new FlashSaleOrderRequest(entity.getRequestId(), entity.getCampaignId(), entity.getUserId(),
                entity.getSkuId(), entity.getQuantity(), entity.getToken(),
                FlashSaleRequestStatus.valueOf(entity.getStatus()), entity.getCreatedAt().toInstant(ZoneOffset.UTC),
                entity.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
}
