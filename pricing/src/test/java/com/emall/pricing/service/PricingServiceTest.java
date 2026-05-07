package com.emall.pricing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.pricing.domain.PriceQuote;
import com.emall.pricing.repository.InMemoryPriceRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService(new InMemoryPriceRepository());

    @Test
    void shouldCreatePriceAndQuoteSubtotal() {
        pricingService.upsert(30001L, new BigDecimal("3999.00"), new BigDecimal("3799.00"), "CNY", true);

        PriceQuote quote = pricingService.quote(30001L, 2);

        assertThat(quote.unitPrice()).isEqualByComparingTo("3799.00");
        assertThat(quote.subtotal()).isEqualByComparingTo("7598.00");
        assertThat(quote.priceVersion()).isEqualTo(1L);
    }

    @Test
    void shouldRejectSalePriceGreaterThanListPrice() {
        assertThatThrownBy(
                () -> pricingService.upsert(30001L, new BigDecimal("99.00"), new BigDecimal("100.00"), "CNY", true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("sale price cannot exceed list price");
    }
}
