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
        assertThatThrownBy(() -> cartService.update(70001L, 30001L, 1, true)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("cart item not found");
    }

    @Test
    void shouldRejectItemQuantityAboveLimit() {
        cartService.add(70001L, 30001L, 999);

        assertThatThrownBy(() -> cartService.add(70001L, 30001L, 1)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("item quantity limit exceeded");
        assertThat(cartService.list(70001L).items()).singleElement()
                .satisfies(item -> assertThat(item.quantity()).isEqualTo(999));
    }

    @Test
    void shouldRejectNewLineWhenCartLineLimitIsReached() {
        for (int index = 0; index < 500; index++) {
            cartService.add(70002L, 40000L + index, 1);
        }

        assertThatThrownBy(() -> cartService.add(70002L, 50001L, 1)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("cart line limit exceeded");
        assertThat(cartService.add(70002L, 40000L, 1).quantity()).isEqualTo(2);
    }
}
