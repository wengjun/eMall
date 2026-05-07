package com.emall.advertising;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AdvertisingServiceTest {
    private final InMemoryAdvertisingRepository repository = new InMemoryAdvertisingRepository();
    private final AdvertisingService service = new AdvertisingService(repository, new SnowflakeIdGenerator(34L));

    @Test
    void ranksSponsoredItemsAndChargesClickCost() {
        AdCampaign campaign = service.createCampaign(1001L, "phone ads", new BigDecimal("100"), new BigDecimal("2.50"),
                Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        service.addCreative(campaign.campaignId(), 2001L, "Flagship Phone", "https://example.com/p/2001");
        service.addTarget(campaign.campaignId(), "phone", new BigDecimal("1.20"));
        service.changeStatus(campaign.campaignId(), AdStatus.ACTIVE);

        SponsoredResult result = service.rank("phone", 5);
        AdEvent event = service.recordEvent(campaign.campaignId(), result.items().get(0).creativeId(), "click");

        assertThat(result.items()).hasSize(1);
        assertThat(event.cost()).isEqualByComparingTo("2.50");
    }
}
