package com.emall.common.sharding;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

public class HttpShardRouteDirectory implements ShardRouteDirectory {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private final RestClient client;

    public HttpShardRouteDirectory(RestClient.Builder builder, String endpoint, String internalOperationKey) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("route directory endpoint must not be blank");
        }
        this.client =
                builder.clone().baseUrl(endpoint).defaultHeader(INTERNAL_TOKEN_HEADER, internalOperationKey).build();
    }

    @Override
    public Optional<ShardRouteRecord> resolve(String namespace, String lookupHash) {
        try {
            RouteResponse response =
                    client.get().uri("/internal/shard-routes/{namespace}/{lookupHash}", namespace, lookupHash)
                            .retrieve().body(RouteResponse.class);
            return Optional.ofNullable(response == null ? null : response.data());
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "persistent shard route request is invalid");
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public ShardRouteRecord bind(String namespace, String lookupHash, long shardKey, Instant expiresAt,
            boolean unique) {
        try {
            RouteResponse response = client.put()
                    .uri("/internal/shard-routes/{namespace}/{lookupHash}", namespace, lookupHash)
                    .body(new BindRouteRequest(shardKey, expiresAt, unique)).retrieve().body(RouteResponse.class);
            if (response == null || response.data() == null) {
                throw new IllegalStateException("route directory returned an empty binding");
            }
            return response.data();
        } catch (HttpClientErrorException.Conflict ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "global route key already belongs to another entity");
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "persistent shard route request is invalid");
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public boolean removeIfOwned(String namespace, String lookupHash, long shardKey, Long expectedVersion) {
        try {
            BooleanResponse response = client.delete().uri(builder -> {
                var uri = builder.path("/internal/shard-routes/{namespace}/{lookupHash}").queryParam("shardKey",
                        shardKey);
                if (expectedVersion != null) {
                    uri.queryParam("expectedVersion", expectedVersion);
                }
                return uri.build(namespace, lookupHash);
            }).retrieve().body(BooleanResponse.class);
            return response != null && Boolean.TRUE.equals(response.data());
        } catch (HttpClientErrorException.Conflict ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "global route changed before it could be removed");
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "persistent shard route request is invalid");
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public ShardRoutePage scan(String cursor, int limit) {
        try {
            PageResponse response = client.get().uri(builder -> {
                var uri = builder.path("/internal/shard-routes").queryParam("limit", limit);
                if (cursor != null && !cursor.isBlank()) {
                    uri.queryParam("cursor", cursor);
                }
                return uri.build();
            }).retrieve().body(PageResponse.class);
            return response == null || response.data() == null ? new ShardRoutePage(null, null) : response.data();
        } catch (RuntimeException ex) {
            throw unavailable(ex);
        }
    }

    private BusinessException unavailable(RuntimeException cause) {
        BusinessException exception = new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE,
                "persistent shard route directory is unavailable");
        exception.initCause(cause);
        return exception;
    }

    public record BindRouteRequest(long shardKey, Instant expiresAt, boolean unique) {
    }

    private record RouteResponse(boolean success, String code, String message, ShardRouteRecord data,
            Instant timestamp) {
    }

    private record BooleanResponse(boolean success, String code, String message, Boolean data, Instant timestamp) {
    }

    private record PageResponse(boolean success, String code, String message, ShardRoutePage data, Instant timestamp) {
    }
}
