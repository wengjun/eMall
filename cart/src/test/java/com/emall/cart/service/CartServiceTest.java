package com.emall.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emall.cart.domain.CartSnapshot;
import com.emall.cart.repository.InMemoryCartRepository;
import com.emall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class CartServiceTest {
    private final CartService cartService = new CartService(new InMemoryCartRepository());

    @Test
    void shouldMergeItemsAndClearSelectedLines() {
        cartService.add(70001L, 30001L, 1);
        cartService.add(70001L, 30001L, 2);
        cartService.add(70001L, 30002L, 4);
        cartService.update(70001L, 30002L, 4, false);

        CartSnapshot beforeClear = cartService.list(70001L);
        cartService.clearSelected(70001L);
        CartSnapshot afterClear = cartService.list(70001L);

        assertThat(beforeClear.selectedQuantity()).isEqualTo(3);
        assertThat(afterClear.items()).singleElement().satisfies(item -> {
            assertThat(item.skuId()).isEqualTo(30002L);
            assertThat(item.selected()).isFalse();
        });
    }

    @Test
    void shouldRejectMissingCartItemUpdate() {
        assertThatThrownBy(() -> cartService.update(70001L, 30001L, 1, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cart item not found");
    }
}
