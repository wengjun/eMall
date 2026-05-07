package com.emall.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PromotionServiceTest {
    private final InMemoryPromotionRepository repository = new InMemoryPromotionRepository();
    private final PromotionService service = new PromotionService(repository, new SnowflakeIdGenerator(32L));

    @Test
    void stacksCampaignsAndConsumesBudget() {
        Instant start = Instant.now().minusSeconds(60);
        Instant end = Instant.now().plusSeconds(3600);
        PromotionCampaign amountOff = service.createCampaign("amount off", PromotionType.AMOUNT_OFF,
                new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("100"), 1, true, start, end);
        PromotionCampaign percentOff = service.createCampaign("percent off", PromotionType.PERCENT_OFF, BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("100"), 2, true, start, end);
        service.changeStatus(amountOff.campaignId(), CampaignStatus.ACTIVE);
        service.changeStatus(percentOff.campaignId(), CampaignStatus.ACTIVE);

        PromotionQuote quote = service.quote(1001L, new BigDecimal("200"));

        assertThat(quote.discountAmount()).isEqualByComparingTo("40.00");
        assertThat(quote.campaignIds()).hasSize(2);
        assertThat(service.calendar("2026-04").activeCampaigns()).isEqualTo(2);
    }
}
