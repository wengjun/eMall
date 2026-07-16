package com.emall.order.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MybatisPlusOrderSagaRepositoryTest {
    @Test
    void updateMustFailWhenTheExpectedDatabaseVersionWasAlreadyReplaced() {
        OrderSagaMapper mapper = mock(OrderSagaMapper.class);
        MybatisPlusOrderSagaRepository repository = new MybatisPlusOrderSagaRepository(mapper);
        OrderCreateSaga transition = OrderCreateSaga.start(1L, "request-1", 101L, 201L, 301L, Instant.now())
                .advance(OrderSagaStage.VALIDATED, null, "request-1");
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> repository.save(transition)).isInstanceOf(OrderSagaConcurrencyException.class)
                .hasMessageContaining("expectedVersion=0");
    }

    @Test
    void successfulCompareAndSetReturnsTheNewVersion() {
        OrderSagaMapper mapper = mock(OrderSagaMapper.class);
        MybatisPlusOrderSagaRepository repository = new MybatisPlusOrderSagaRepository(mapper);
        OrderCreateSaga transition = OrderCreateSaga.start(1L, "request-1", 101L, 201L, 301L, Instant.now())
                .advance(OrderSagaStage.VALIDATED, null, "request-1");
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        assertThat(repository.save(transition).version()).isEqualTo(1L);
    }
}
