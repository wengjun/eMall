package com.emall.flashsale.service;

import com.emall.common.api.ErrorCode;
import com.emall.common.event.EventTypes;
import com.emall.common.event.OutboxEvent;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.idempotency.IdempotencyExecutor;
import com.emall.common.idempotency.IdempotencyKey;
import com.emall.common.idempotency.IdempotencyRecord;
import com.emall.common.idempotency.IdempotencyService;
import com.emall.common.idempotency.InMemoryIdempotencyRepository;
import com.emall.common.metrics.BusinessMetricNames;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.outbox.OutboxRepository;
import com.emall.common.trust.ClientTrustContext;
import com.emall.common.trust.IdentityAccessGuard;
import com.emall.common.trust.RiskEvaluationRequest;
import com.emall.common.trust.RiskGuard;
import com.emall.common.trust.RiskScene;
import com.emall.flashsale.domain.CampaignStatus;
import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleRequestStatus;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
import com.emall.flashsale.messaging.FlashSaleOrderQueuePublisher;
import com.emall.flashsale.messaging.NoopFlashSaleOrderQueuePublisher;
import com.emall.flashsale.repository.FlashSaleRepository;
import com.emall.flashsale.repository.InMemoryOutboxRepository;
import com.emall.flashsale.runtime.FlashSaleRuntimeStore;
import com.emall.flashsale.runtime.FlashSaleTokenSigner;
import com.emall.flashsale.runtime.InMemoryFlashSaleRuntimeStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlashSaleService {
    private final FlashSaleRepository repository;
    private final SnowflakeIdGenerator idGenerator;
    private final FlashSaleRuntimeStore runtimeStore;
    private final FlashSaleTokenSigner tokenSigner;
    private final FlashSaleOrderQueuePublisher queuePublisher;
    private final OutboxRepository outboxRepository;
    private final BusinessMetrics businessMetrics;
    private final IdentityAccessGuard identityAccessGuard;
    private final RiskGuard riskGuard;
    private final IdempotencyService idempotencyService;

    public FlashSaleService(FlashSaleRepository repository, SnowflakeIdGenerator idGenerator) {
        this(repository, idGenerator, new InMemoryFlashSaleRuntimeStore(),
                new FlashSaleTokenSigner("local-dev-flash-sale-token-secret"), new NoopFlashSaleOrderQueuePublisher(),
                new InMemoryOutboxRepository(), BusinessMetrics.noop(), IdentityAccessGuard.noop(), RiskGuard.noop(),
                localIdempotencyService());
    }

    @Autowired
    public FlashSaleService(FlashSaleRepository repository, SnowflakeIdGenerator idGenerator,
            FlashSaleRuntimeStore runtimeStore, FlashSaleTokenSigner tokenSigner,
            FlashSaleOrderQueuePublisher queuePublisher, OutboxRepository outboxRepository,
            BusinessMetrics businessMetrics, IdentityAccessGuard identityAccessGuard, RiskGuard riskGuard,
            IdempotencyService idempotencyService) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.runtimeStore = runtimeStore;
        this.tokenSigner = tokenSigner;
        this.queuePublisher = queuePublisher;
        this.outboxRepository = outboxRepository;
        this.businessMetrics = businessMetrics;
        this.identityAccessGuard = identityAccessGuard;
        this.riskGuard = riskGuard;
        this.idempotencyService = idempotencyService;
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
        runtimeStore.preloadStock(campaignId, totalStock);
        return repository.saveStock(stock);
    }

    public FlashSaleStock getStock(long campaignId) {
        return repository.findStock(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "flash sale stock not found"));
    }

    @Transactional
    public FlashSaleToken issueToken(long campaignId, long userId, int quantity) {
        return issueToken(null, campaignId, userId, quantity, null);
    }

    @Transactional
    public FlashSaleToken issueToken(long campaignId, long userId, int quantity, ClientTrustContext trustContext) {
        return issueToken(null, campaignId, userId, quantity, trustContext);
    }

    @Transactional
    public FlashSaleToken issueToken(String requestId, long campaignId, long userId, int quantity,
            ClientTrustContext trustContext) {
        if (requestId == null || requestId.isBlank()) {
            return issueTokenRaw(campaignId, userId, quantity, trustContext);
        }
        IdempotencyKey key = IdempotencyKey.of("flash-sale", String.valueOf(userId), requestId, "issue-token");
        String requestDigest =
                idempotencyService.digest("campaignId=" + campaignId + ",userId=" + userId + ",quantity=" + quantity);
        return IdempotencyExecutor.execute(idempotencyService, key, "FlashSaleToken", String.valueOf(campaignId),
                requestDigest, () -> issueTokenRaw(campaignId, userId, quantity, trustContext), this::replayIssuedToken,
                token -> Long.toString(token.tokenId()));
    }

    private FlashSaleToken issueTokenRaw(long campaignId, long userId, int quantity, ClientTrustContext trustContext) {
        FlashSaleCampaign campaign = getCampaign(campaignId);
        Instant now = Instant.now();
        validateOpenCampaign(campaign, now);
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "quantity must be positive");
        }
        ClientTrustContext safeTrustContext = normalizeTrustContext(trustContext, userId);
        identityAccessGuard.requireAccess(safeTrustContext, userId, "flash-sale:token", "user:" + userId);
        int issuedTokenCount = repository.countTokensByUser(campaignId, userId);
        riskGuard.check(new RiskEvaluationRequest(RiskScene.FLASH_SALE_TOKEN, safeTrustContext.subjectId(userId),
                safeTrustContext.deviceId(), safeTrustContext.sourceIp(), BigDecimal.ZERO, issuedTokenCount + 1));
        if (issuedTokenCount >= campaign.perUserLimit()) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale per-user limit exceeded");
        }
        if (!runtimeStore.reserveTokenStock(campaignId, userId, quantity, campaign.perUserLimit(),
                campaign.tokenTtlSeconds())) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale stock is sold out");
        }
        boolean databaseReserved = false;
        try {
            if (!repository.reserveTokenStock(campaignId, quantity)) {
                throw new BusinessException(ErrorCode.CONFLICT, "flash sale stock is sold out");
            }
            databaseReserved = true;
            long tokenId = idGenerator.nextId();
            Instant expiresAt = now.plusSeconds(campaign.tokenTtlSeconds());
            FlashSaleToken token = new FlashSaleToken(tokenId, campaignId, userId, campaign.skuId(), quantity,
                    tokenSigner.sign(tokenId, campaignId, userId, campaign.skuId(), quantity, expiresAt), expiresAt,
                    false, now, now);
            FlashSaleToken saved = repository.saveToken(token);
            businessMetrics.increment(BusinessMetricNames.FLASH_SALE_TOKEN_ISSUED, "campaign_id",
                    String.valueOf(campaignId));
            return saved;
        } catch (RuntimeException ex) {
            runtimeStore.releaseTokenStock(campaignId, userId, quantity);
            if (databaseReserved) {
                repository.releaseTokenStock(campaignId, quantity);
            }
            throw ex;
        }
    }

    @Transactional
    public FlashSaleOrderRequest enqueueOrder(String tokenValue) {
        return enqueueOrder(null, tokenValue, null);
    }

    @Transactional
    public FlashSaleOrderRequest enqueueOrder(String tokenValue, ClientTrustContext trustContext) {
        return enqueueOrder(null, tokenValue, trustContext);
    }

    @Transactional
    public FlashSaleOrderRequest enqueueOrder(String requestId, String tokenValue, ClientTrustContext trustContext) {
        String idempotencyRequestId =
                requestId == null || requestId.isBlank() ? idempotencyService.digest(tokenValue) : requestId;
        FlashSaleToken token = repository.findToken(tokenValue).orElse(null);
        String ownerId = token == null ? "unknown" : String.valueOf(token.userId());
        IdempotencyKey key = IdempotencyKey.of("flash-sale", ownerId, idempotencyRequestId, "enqueue-order");
        String requestDigest = idempotencyService.digest("token=" + tokenValue);
        return IdempotencyExecutor.execute(idempotencyService, key, "FlashSaleOrderRequest", idempotencyRequestId,
                requestDigest, () -> enqueueOrderRaw(tokenValue, trustContext),
                ignored -> repository.findOrderRequestByToken(tokenValue)
                        .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                                "idempotent flash sale order result is unavailable")),
                request -> idempotencyService
                        .digest("requestId=" + request.requestId() + ",status=" + request.status()));
    }

    private FlashSaleOrderRequest enqueueOrderRaw(String tokenValue, ClientTrustContext trustContext) {
        if (!tokenSigner.verify(tokenValue)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "flash sale token signature is invalid");
        }
        FlashSaleToken token = repository.findToken(tokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "flash sale token not found"));
        FlashSaleCampaign campaign = getCampaign(token.campaignId());
        Instant now = Instant.now();
        validateOpenCampaign(campaign, now);
        ClientTrustContext safeTrustContext = normalizeTrustContext(trustContext, token.userId());
        identityAccessGuard.requireAccess(safeTrustContext, token.userId(), "flash-sale:order",
                "user:" + token.userId());
        if (token.used()) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale token has already been used");
        }
        if (token.isExpiredAt(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale token has expired");
        }
        if (repository.countQueuedRequests(campaign.campaignId()) >= campaign.queueCapacity()) {
            businessMetrics.increment(BusinessMetricNames.FLASH_SALE_QUEUE_REJECTED, "reason", "queue_full");
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "flash sale queue is full");
        }
        if (!runtimeStore.enqueueToken(campaign.campaignId(), tokenValue, token.quantity(), campaign.queueCapacity(),
                campaign.tokenTtlSeconds())) {
            businessMetrics.increment(BusinessMetricNames.FLASH_SALE_QUEUE_REJECTED, "reason", "runtime_rejected");
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale token cannot enter queue");
        }
        FlashSaleOrderRequest saved = null;
        boolean tokenUsed = false;
        boolean stockMoved = false;
        try {
            if (!repository.markTokenUsed(tokenValue)) {
                throw new BusinessException(ErrorCode.CONFLICT, "flash sale token has already been used");
            }
            tokenUsed = true;
            if (!repository.moveTokenStockToQueue(campaign.campaignId(), token.quantity())) {
                throw new BusinessException(ErrorCode.CONFLICT, "flash sale reserved stock is unavailable");
            }
            stockMoved = true;
            FlashSaleOrderRequest request =
                    new FlashSaleOrderRequest(idGenerator.nextId(), campaign.campaignId(), token.userId(),
                            token.skuId(), token.quantity(), token.token(), FlashSaleRequestStatus.QUEUED, now, now);
            saved = repository.saveOrderRequest(request);
            appendOrderQueued(saved);
            businessMetrics.increment(BusinessMetricNames.FLASH_SALE_QUEUE_ENQUEUED, "campaign_id",
                    String.valueOf(campaign.campaignId()));
            return saved;
        } catch (RuntimeException ex) {
            runtimeStore.releaseQueuedToken(campaign.campaignId(), tokenValue, token.quantity());
            if (saved != null) {
                repository.deleteOrderRequest(saved.requestId());
            }
            if (stockMoved) {
                repository.releaseQueuedStock(campaign.campaignId(), token.quantity());
            }
            if (tokenUsed) {
                repository.unmarkTokenUsed(tokenValue);
            }
            throw ex;
        }
    }

    private FlashSaleToken replayIssuedToken(IdempotencyRecord record) {
        try {
            return repository.findTokenById(Long.parseLong(record.responseDigest()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT,
                            "idempotent flash sale token result is unavailable"));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "idempotent flash sale token result is invalid");
        }
    }

    public FlashSaleOrderRequest getOrderRequest(long requestId) {
        return repository.findOrderRequest(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "flash sale order request not found"));
    }

    public List<FlashSaleOrderRequest> findQueuedRequests(long campaignId, int limit) {
        getCampaign(campaignId);
        return repository.findQueuedRequests(campaignId, Math.max(1, Math.min(limit, 1000)));
    }

    private void appendOrderQueued(FlashSaleOrderRequest request) {
        outboxRepository.save(OutboxEvent.create("flash-sale-order-event-" + idGenerator.nextId(),
                "FlashSaleOrderRequest", String.valueOf(request.requestId()), EventTypes.FLASH_SALE_ORDER_QUEUED,
                Map.of("requestId", request.requestId(), "campaignId", request.campaignId(), "userId", request.userId(),
                        "skuId", request.skuId(), "quantity", request.quantity(), "token", request.token(), "status",
                        request.status().name())));
    }

    private void validateOpenCampaign(FlashSaleCampaign campaign, Instant now) {
        if (!campaign.isOpenAt(now)) {
            throw new BusinessException(ErrorCode.CONFLICT, "flash sale campaign is not open");
        }
    }

    private ClientTrustContext normalizeTrustContext(ClientTrustContext trustContext, long userId) {
        ClientTrustContext base = trustContext == null ? ClientTrustContext.anonymous() : trustContext;
        return base.withDefaults(userId, null, null);
    }

    private static IdempotencyService localIdempotencyService() {
        return new IdempotencyService(new InMemoryIdempotencyRepository(), Clock.systemUTC(), Duration.ofSeconds(30),
                Duration.ofDays(1));
    }
}
