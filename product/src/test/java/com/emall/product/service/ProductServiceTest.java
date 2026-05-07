package com.emall.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.common.exception.BusinessException;
import com.emall.common.id.SnowflakeIdGenerator;
import com.emall.product.domain.Product;
import com.emall.product.domain.ProductStatus;
import com.emall.product.repository.InMemoryOutboxRepository;
import com.emall.product.repository.InMemoryProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProductServiceTest {
    private final InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
    private final ProductService productService =
            new ProductService(new InMemoryProductRepository(), outboxRepository, new SnowflakeIdGenerator(1));

    @Test
    void shouldCreatePublishAndSearchProduct() {
        Product created = productService.create(1001L, "flagship phone", "digital", new BigDecimal("3999.00"));

        Product onSale = productService.changeStatus(created.skuId(), ProductStatus.ON_SALE);
        Product repriced = productService.changePrice(created.skuId(), new BigDecimal("3799.00"));

        assertThat(onSale.status()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(repriced.price()).isEqualByComparingTo("3799.00");
        assertThat(productService.search("phone", 10)).extracting(Product::skuId).contains(created.skuId());
        assertThat(outboxRepository.findPublishable(Instant.now(), 10)).hasSize(3);
    }

    @Test
    void shouldRejectNonPositivePrice() {
        Product created = productService.create(1001L, "flagship phone", "digital", new BigDecimal("3999.00"));

        assertThatThrownBy(() -> productService.changePrice(created.skuId(), BigDecimal.ZERO))
                .isInstanceOf(BusinessException.class).hasMessageContaining("price must be positive");
    }
}
