package com.emall.flashsale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.event.OutboxEvent;
import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.common.idempotency.IdempotencyService;
import com.emall.common.idempotency.InMemoryIdempotencyRepository;
import com.emall.common.metrics.BusinessMetrics;
import com.emall.common.trust.IdentityAccessGuard;
import com.emall.common.trust.RiskGuard;
import com.emall.flashsale.domain.CampaignStatus;
import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleRequestStatus;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
import com.emall.flashsale.messaging.NoopFlashSaleOrderQueuePublisher;
import com.emall.flashsale.repository.InMemoryFlashSaleRepository;
import com.emall.flashsale.repository.InMemoryOutboxRepository;
import com.emall.flashsale.runtime.FlashSaleTokenSigner;
import com.emall.flashsale.runtime.InMemoryFlashSaleRuntimeStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlashSaleServiceTest {
    private final InMemoryFlashSaleRepository repository = new InMemoryFlashSaleRepository();
    private final FlashSaleService service = new FlashSaleService(repository, new SnowflakeIdGenerator(12L));

    @Test
    void issuesTokenAndQueuesOrderAgainstPreallocatedStock() {
        FlashSaleCampaign campaign = createActiveCampaign(2, 10);
        service.preallocateStock(campaign.campaignId(), 5);

        FlashSaleToken token = service.issueToken(campaign.campaignId(), 1001L, 2);
        FlashSaleOrderRequest request = service.enqueueOrder(token.token());
        FlashSaleStock stock = service.getStock(campaign.campaignId());
        List<FlashSaleOrderRequest> queued = service.findQueuedRequests(campaign.campaignId(), 10);

        assertThat(token.used()).isFalse();
        assertThat(request.status()).isEqualTo(FlashSaleRequestStatus.QUEUED);
        assertThat(stock.availableStock()).isEqualTo(3);
        assertThat(stock.tokenReservedStock()).isZero();
        assertThat(stock.queuedStock()).isEqualTo(2);
        assertThat(queued).containsExactly(request);
    }

    @Test
    void rejectsUsersBeyondCampaignLimit() {
        FlashSaleCampaign campaign = createActiveCampaign(1, 10);
        service.preallocateStock(campaign.campaignId(), 5);
        service.issueToken(campaign.campaignId(), 1001L, 1);

        assertThatThrownBy(() -> service.issueToken(campaign.campaignId(), 1001L, 1))
                .isInstanceOf(BusinessException.class).hasMessageContaining("per-user limit");
    }

    @Test
    void rejectsWhenQueueIsFull() {
        FlashSaleCampaign campaign = createActiveCampaign(2, 1);
        service.preallocateStock(campaign.campaignId(), 5);
        service.enqueueOrder(service.issueToken(campaign.campaignId(), 1001L, 1).token());
        FlashSaleToken secondToken = service.issueToken(campaign.campaignId(), 1002L, 1);

        assertThatThrownBy(() -> service.enqueueOrder(secondToken.token())).isInstanceOf(BusinessException.class)
                .hasMessageContaining("queue is full");
    }

    @Test
    void rejectsTamperedTokenBeforeQueueing() {
        FlashSaleCampaign campaign = createActiveCampaign(2, 10);
        service.preallocateStock(campaign.campaignId(), 5);
        FlashSaleToken token = service.issueToken(campaign.campaignId(), 1001L, 1);

        assertThatThrownBy(() -> service.enqueueOrder(token.token() + "x")).isInstanceOf(BusinessException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void compensatesRuntimeAndDatabaseStateWhenOrderOutboxAppendFails() {
        InMemoryFlashSaleRepository failingRepository = new InMemoryFlashSaleRepository();
        FailingOnceOutboxRepository outboxRepository = new FailingOnceOutboxRepository();
        FlashSaleService failingService = new FlashSaleService(failingRepository, new SnowflakeIdGenerator(13L),
                new InMemoryFlashSaleRuntimeStore(), new FlashSaleTokenSigner("local-dev-flash-sale-token-secret"),
                new NoopFlashSaleOrderQueuePublisher(), outboxRepository, BusinessMetrics.noop(),
                IdentityAccessGuard.noop(), RiskGuard.noop(),
                new IdempotencyService(new InMemoryIdempotencyRepository(), Clock.systemUTC(), Duration.ofSeconds(30),
                        Duration.ofDays(1)));
        Instant now = Instant.now();
        FlashSaleCampaign campaign = failingService.createCampaign(3001L, "launch sale",
                now.minus(1, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS), 2, 60, 10);
        campaign = failingService.changeCampaignStatus(campaign.campaignId(), CampaignStatus.ACTIVE);
        failingService.preallocateStock(campaign.campaignId(), 2);
        FlashSaleToken token = failingService.issueToken(campaign.campaignId(), 1001L, 1);

        assertThatThrownBy(() -> failingService.enqueueOrder(token.token())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("outbox unavailable");
        FlashSaleOrderRequest retry = failingService.enqueueOrder(token.token());

        FlashSaleStock stock = failingService.getStock(campaign.campaignId());
        assertThat(retry.status()).isEqualTo(FlashSaleRequestStatus.QUEUED);
        assertThat(stock.availableStock()).isEqualTo(1);
        assertThat(stock.tokenReservedStock()).isZero();
        assertThat(stock.queuedStock()).isEqualTo(1);
    }

    private FlashSaleCampaign createActiveCampaign(int perUserLimit, int queueCapacity) {
        Instant now = Instant.now();
        FlashSaleCampaign campaign = service.createCampaign(3001L, "launch sale", now.minus(1, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.HOURS), perUserLimit, 60, queueCapacity);
        return service.changeCampaignStatus(campaign.campaignId(), CampaignStatus.ACTIVE);
    }

    private static final class FailingOnceOutboxRepository extends InMemoryOutboxRepository {
        private boolean failed;

        @Override
        public OutboxEvent save(OutboxEvent event) {
            if (!failed) {
                failed = true;
                throw new IllegalStateException("outbox unavailable");
            }
            return super.save(event);
        }
    }
}
