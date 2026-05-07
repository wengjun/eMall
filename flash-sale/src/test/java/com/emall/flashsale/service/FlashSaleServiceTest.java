package com.emall.flashsale.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.flashsale.domain.CampaignStatus;
import com.emall.flashsale.domain.FlashSaleCampaign;
import com.emall.flashsale.domain.FlashSaleOrderRequest;
import com.emall.flashsale.domain.FlashSaleRequestStatus;
import com.emall.flashsale.domain.FlashSaleStock;
import com.emall.flashsale.domain.FlashSaleToken;
import com.emall.flashsale.repository.InMemoryFlashSaleRepository;
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

    private FlashSaleCampaign createActiveCampaign(int perUserLimit, int queueCapacity) {
        Instant now = Instant.now();
        FlashSaleCampaign campaign = service.createCampaign(3001L, "launch sale", now.minus(1, ChronoUnit.MINUTES),
                now.plus(1, ChronoUnit.HOURS), perUserLimit, 60, queueCapacity);
        return service.changeCampaignStatus(campaign.campaignId(), CampaignStatus.ACTIVE);
    }
}
