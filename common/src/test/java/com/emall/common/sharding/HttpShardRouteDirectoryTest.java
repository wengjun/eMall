package com.emall.common.sharding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpShardRouteDirectoryTest {
    private static final String HASH = "a".repeat(64);

    @Test
    void treatsMissingPersistentRouteAsAValidCacheMiss() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpShardRouteDirectory directory = new HttpShardRouteDirectory(builder, "http://routing:8117", "token");
        server.expect(requestTo("http://routing:8117/internal/shard-routes/order-id/" + HASH))
                .andExpect(header("X-Internal-Token", "token")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(directory.resolve("order-id", HASH)).isEmpty();
        server.verify();
    }

    @Test
    void preservesOwnershipConflictInsteadOfReportingAnOutage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpShardRouteDirectory directory = new HttpShardRouteDirectory(builder, "http://routing:8117", "token");
        server.expect(requestTo("http://routing:8117/internal/shard-routes/order-id/" + HASH))
                .andExpect(request -> assertThat(request.getMethod()).isEqualTo(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> directory.bind("order-id", HASH, 42L, null, true)).isInstanceOfSatisfying(
                BusinessException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CONFLICT));
        server.verify();
    }

    @Test
    void mapsTransportFailureToDownstreamUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpShardRouteDirectory directory = new HttpShardRouteDirectory(builder, "http://routing:8117", "token");
        server.expect(requestTo("http://routing:8117/internal/shard-routes/order-id/" + HASH))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> directory.resolve("order-id", HASH)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.DOWNSTREAM_UNAVAILABLE));
        server.verify();
    }
}
