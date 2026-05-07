package com.emall.flashsale.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.flashsale.domain.CampaignStatus;
import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleRequestStatus;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
import com.emall.flashsale.repository.FlashSaleRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlashSaleService {
    private final FlashSaleRepository repository;
    private final SnowflakeIdGenerator idGenerator;

    public FlashSaleService(FlashSaleRepository repository, SnowflakeIdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public FlashSaleCampaign createCampaign(long skuId, String name, Instant startsAt, Instant endsAt, int perUserLimit,
            int tokenTtlSeconds, int queueCapacity) {
        if (!startsAt.isBefore(endsAt)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "campaign start time must be before end time");
        }
        Instant now = Instant.now();
        return repository.saveCampaign(new FlashSaleCampaign(idGenerator.nextId(), skuId, name, startsAt, endsAt,
                perUserLimit, tokenTtlSeconds, queueCapacity, CampaignStatus.DRAFT, now, now));
    }

    public FlashSaleCampaign getCampaign(long campaignId) {
        return repository.findCampaign(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "flash sale campaign not found"));
    }

    @Transactional
    public FlashSaleCampaign changeCampaignStatus(long campaignId, CampaignStatus status) {
        return repository.saveCampaign(getCampaign(campaignId).changeStatus(status));
    }

    @Transactional
    public FlashSaleStock preallocateStock(long campaignId, int totalStock) {
        if (totalStock <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "flash sale stock must be positive");
        }
        FlashSaleCampaign campaign = getCampaign(campaignId);
        FlashSaleStock stock =
                new FlashSaleStock(campaignId, campaign.skuId(), totalStock, totalStock, 0, 0, 0, Instant.now());
        return repository.saveStock(stock);
    }

    public FlashSaleStock getStock(long campaignId) {
        return repository.findStock(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "flash sale stock not found"));
    }

    @Transactional
    public synchronized FlashSaleToken issueToken(long campaignId, long userId, int quantity) {
        FlashSaleCampaign campaign = getCampaign(campaignId);
        Instant now = Instant.now();
        validateOpenCampaign(campaign, now);
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "quantity must be positive");
        }
        if (repository.countTokensByUser(campaignId, userId) >= campaign.perUserLimit()) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale per-user limit exceeded");
        }
        if (!repository.reserveTokenStock(campaignId, quantity)) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale stock is sold out");
        }
        long tokenId = idGenerator.nextId();
        FlashSaleToken token = new FlashSaleToken(tokenId, campaignId, userId, campaign.skuId(), quantity,
                generateToken(tokenId), now.plusSeconds(campaign.tokenTtlSeconds()), false, now, now);
        return repository.saveToken(token);
    }

    @Transactional
    public synchronized FlashSaleOrderRequest enqueueOrder(String tokenValue) {
        FlashSaleToken token = repository.findToken(tokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "flash sale token not found"));
        FlashSaleCampaign campaign = getCampaign(token.campaignId());
        Instant now = Instant.now();
        validateOpenCampaign(campaign, now);
        if (token.used()) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale token has already been used");
        }
        if (token.isExpiredAt(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale token has expired");
        }
        if (repository.countQueuedRequests(campaign.campaignId()) >= campaign.queueCapacity()) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "flash sale queue is full");
        }
        if (!repository.moveTokenStockToQueue(campaign.campaignId(), token.quantity())) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale reserved stock is unavailable");
        }
        FlashSaleToken usedToken = repository.saveToken(token.use());
        FlashSaleOrderRequest request = new FlashSaleOrderRequest(idGenerator.nextId(), campaign.campaignId(),
                usedToken.userId(), usedToken.skuId(), usedToken.quantity(), usedToken.token(),
                FlashSaleRequestStatus.QUEUED, now, now);
        return repository.saveOrderRequest(request);
    }

    public FlashSaleOrderRequest getOrderRequest(long requestId) {
        return repository.findOrderRequest(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "flash sale order request not found"));
    }

    public List<FlashSaleOrderRequest> findQueuedRequests(long campaignId, int limit) {
        getCampaign(campaignId);
        return repository.findQueuedRequests(campaignId, Math.max(1, Math.min(limit, 1000)));
    }

    private void validateOpenCampaign(FlashSaleCampaign campaign, Instant now) {
        if (!campaign.isOpenAt(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale campaign is not open");
        }
    }

    private String generateToken(long tokenId) {
        return tokenId + "." + UUID.randomUUID().toString().replace("-", "");
    }
}
