package com.emall.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.emall.common.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CatalogServiceTest {
    private final InMemoryCatalogRepository repository = new InMemoryCatalogRepository();
    private final CatalogService service = new CatalogService(repository, new SnowflakeIdGenerator(31L));

    @Test
    void createsReviewedAndPublishedListing() {
        CategoryNode category = service.createCategory(0L, "phone", "Phone", true);
        service.upsertTemplate(category.categoryId(), "color,memory", "network");
        service.authorizeBrand(1001L, "brand-a");
        Spu spu = service.createSpu(1001L, "Flagship Phone", category.categoryId(), "brand-a");
        service.createSku(spu.spuId(), "phone-red-256", new BigDecimal("4999.00"), "color=red;memory=256");

        ListingReview review = service.reviewListing(spu.spuId(), true, 95, "approved");
        Spu published = service.publish(spu.spuId());

        assertThat(review.status()).isEqualTo(ListingStatus.APPROVED);
        assertThat(published.status()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(service.findSkus(spu.spuId())).hasSize(1);
    }

    @Test
    void rejectedListingRecordsViolation() {
        CategoryNode category = service.createCategory(0L, "book", "Book", true);
        service.authorizeBrand(1001L, "brand-b");
        Spu spu = service.createSpu(1001L, "Bad Listing", category.categoryId(), "brand-b");

        service.reviewListing(spu.spuId(), false, 20, "prohibited words");

        assertThat(service.findViolations(spu.spuId())).hasSize(1);
    }
}
