package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HashModShardRouterTest {
    @Test
    void shouldRouteSameShardKeyToSamePhysicalShard() {
        HashModShardRouter router = new HashModShardRouter("order_db", 4, "orders", 16);

        ShardRoute first = router.route(123456789L);
        ShardRoute second = router.route(123456789L);

        assertThat(first).isEqualTo(second);
        assertThat(first.databaseName()).startsWith("order_db_");
        assertThat(first.tableName()).startsWith("orders_");
        assertThat(first.databaseIndex()).isBetween(0, 3);
        assertThat(first.tableIndex()).isBetween(0, 15);
    }

    @Test
    void shouldSupportNegativeHashValues() {
        HashModShardRouter router = new HashModShardRouter("user_db", 2, "user_profile", 8);

        ShardRoute route = router.route(-17L);

        assertThat(route.databaseIndex()).isBetween(0, 1);
        assertThat(route.tableIndex()).isBetween(0, 7);
    }

    @Test
    void shouldRejectInvalidShardCounts() {
        assertThatThrownBy(() -> new HashModShardRouter("order_db", 0, "orders", 16))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("databaseShardCount");
        assertThatThrownBy(() -> new HashModShardRouter("order_db", 4, "orders", 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tableShardCount");
    }
}
