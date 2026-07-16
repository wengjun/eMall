package com.emall.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.emall.common.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ShardRouteServiceTest {
    private static final String HASH = "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    @Test
    void createsAVersionedAuthoritativeRoute() {
        ShardRouteMapper mapper = mock(ShardRouteMapper.class);
        when(mapper.selectById("order-id:" + HASH)).thenReturn(null);
        ShardRouteService service = service(mapper);

        var route = service.bind("order-id", HASH, 2001L, null, true);

        assertThat(route.shardKey()).isEqualTo(2001L);
        assertThat(route.version()).isEqualTo(1L);
        verify(mapper).insert(any(ShardRouteEntity.class));
    }

    @Test
    void rejectsUniqueRouteOwnershipChanges() {
        ShardRouteMapper mapper = mock(ShardRouteMapper.class);
        when(mapper.selectById("order-id:" + HASH)).thenReturn(entity(2001L, 4L));
        ShardRouteService service = service(mapper);

        assertThatThrownBy(() -> service.bind("order-id", HASH, 3001L, null, true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("already belongs");
    }

    @Test
    void advancesVersionWithDatabaseCompareAndSet() {
        ShardRouteMapper mapper = mock(ShardRouteMapper.class);
        when(mapper.selectById("order-id:" + HASH)).thenReturn(entity(2001L, 4L));
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        ShardRouteService service = service(mapper);

        var route = service.bind("order-id", HASH, 2001L, null, true);

        assertThat(route.version()).isEqualTo(5L);
    }

    private ShardRouteService service(ShardRouteMapper mapper) {
        return new ShardRouteService(mapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ShardRouteEntity entity(long shardKey, long version) {
        ShardRouteEntity entity = new ShardRouteEntity();
        entity.setRouteId("order-id:" + HASH);
        entity.setNamespace("order-id");
        entity.setLookupHash(HASH);
        entity.setShardKey(shardKey);
        entity.setRouteVersion(version);
        entity.setCreatedAt(LocalDateTime.ofInstant(NOW.minusSeconds(60), ZoneOffset.UTC));
        entity.setUpdatedAt(LocalDateTime.ofInstant(NOW.minusSeconds(30), ZoneOffset.UTC));
        return entity;
    }
}
