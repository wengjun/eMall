package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.emall.common.api.ApiResponse;
import com.emall.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpVirtualShardPlacementProviderTest {
    private static final String TOKEN = "internal-token";

    @Test
    void permitsStaleReadsButFailsClosedForWritesAfterControlPlaneFailure() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-15T10:00:00Z"));
        VirtualShardPlacement placement = placement("order", 42, ShardMigrationState.STABLE);
        server.expect(once(), requestTo("http://routing/internal/virtual-shards/order/42"))
                .andExpect(header("X-Internal-Token", TOKEN))
                .andRespond(withSuccess(json(ApiResponse.ok(placement)), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("http://routing/internal/virtual-shards/order/42"))
                .andRespond(withServerError());
        server.expect(once(), requestTo("http://routing/internal/virtual-shards/order/42"))
                .andRespond(withServerError());
        HttpVirtualShardPlacementProvider provider = provider(builder, clock);

        assertThat(provider.resolve("order", 42, ShardAccessMode.WRITE)).isEqualTo(placement);
        clock.advance(Duration.ofSeconds(31));
        assertThat(provider.resolve("order", 42, ShardAccessMode.READ)).isEqualTo(placement);
        assertThatThrownBy(() -> provider.resolve("order", 42, ShardAccessMode.WRITE))
                .isInstanceOf(BusinessException.class).hasMessageContaining("unavailable");
        server.verify();
    }

    @Test
    void preservesCutoverWriteFencingError() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        VirtualShardPlacement placement = placement("order", 42, ShardMigrationState.CUTOVER_PENDING);
        server.expect(requestTo("http://routing/internal/virtual-shards/order/42"))
                .andRespond(withSuccess(json(ApiResponse.ok(placement)), MediaType.APPLICATION_JSON));
        HttpVirtualShardPlacementProvider provider =
                provider(builder, new MutableClock(Instant.parse("2026-07-15T10:00:00Z")));

        assertThatThrownBy(() -> provider.resolve("order", 42, ShardAccessMode.WRITE))
                .isInstanceOf(ShardWriteFencedException.class);
        assertThat(provider.resolve("order", 42, ShardAccessMode.READ)).isEqualTo(placement);
        server.verify();
    }

    private HttpVirtualShardPlacementProvider provider(RestClient.Builder builder, Clock clock) {
        ShardRoutingProperties properties = new ShardRoutingProperties();
        properties.setDatabasePrefix("emall_order");
        properties.setVirtualShardCount(4096);
        properties.setDatabaseShardCount(8);
        properties.setTables(Map.of("orders", new ShardRoutingProperties.TableRule("orders", 64)));
        return new HttpVirtualShardPlacementProvider(builder, "http://routing", TOKEN,
                new StaticVirtualShardPlacementProvider(properties), Duration.ofSeconds(30), Duration.ofHours(1),
                clock);
    }

    private VirtualShardPlacement placement(String namespace, int virtualShard, ShardMigrationState state) {
        PhysicalShardPlacement source =
                new PhysicalShardPlacement("emall_order_02", 2, "cn-east-1", "cell-a", Map.of("orders", "orders_05"));
        PhysicalShardPlacement target = state.migrationActive()
                ? new PhysicalShardPlacement("emall_order_10", 10, "cn-east-1", "cell-b", Map.of("orders", "orders_05"))
                : null;
        return new VirtualShardPlacement(namespace, virtualShard, 6L, 1L, state, source, target,
                state == ShardMigrationState.CUTOVER_PENDING ? Instant.parse("2026-07-15T10:01:00Z") : null,
                Instant.parse("2026-07-15T10:00:00Z"));
    }

    private String json(Object value) throws Exception {
        return new ObjectMapper().findAndRegisterModules().writeValueAsString(value);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
