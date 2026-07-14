package com.emall.payment.security;

import com.emall.common.api.ErrorCode;
import com.emall.common.exception.BusinessException;
import com.emall.payment.service.PaymentCallbackCommand;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentCallbackReplayGuard {
    private static final String KEY_PREFIX = "emall:payment:callback-nonce:";
    private static final int MAXIMUM_LOCAL_ENTRIES = 10_000;
    private final StringRedisTemplate redisTemplate;
    private final PaymentSecurityProperties properties;
    private final Map<String, LocalNonce> localNonces = new ConcurrentHashMap<>();

    public PaymentCallbackReplayGuard(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            PaymentSecurityProperties properties) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.properties = properties;
    }

    public PaymentCallbackReplayGuard(PaymentSecurityProperties properties) {
        this.redisTemplate = null;
        this.properties = properties;
    }

    public void claim(PaymentCallbackCommand command) {
        String key = KEY_PREFIX + sha256(command.channel() + ':' + command.nonce());
        String digest = digest(command);
        Duration ttl = Duration.ofSeconds(Math.max(60, properties.getCallbackAllowedSkewSeconds() * 2));
        if (redisTemplate != null) {
            claimRedis(key, digest, ttl);
            return;
        }
        if (properties.isReplayStoreFailClosed()) {
            throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE,
                    "payment callback replay store is unavailable");
        }
        claimLocal(key, digest, ttl);
    }

    private void claimRedis(String key, String digest, Duration ttl) {
        try {
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, digest, ttl))) {
                return;
            }
            String existing = redisTemplate.opsForValue().get(key);
            if (!digest.equals(existing)) {
                throw replayDetected();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (properties.isReplayStoreFailClosed()) {
                throw new BusinessException(ErrorCode.DOWNSTREAM_UNAVAILABLE,
                        "payment callback replay store is unavailable");
            }
            claimLocal(key, digest, ttl);
        }
    }

    private void claimLocal(String key, String digest, Duration ttl) {
        Instant now = Instant.now();
        if (localNonces.size() >= MAXIMUM_LOCAL_ENTRIES) {
            localNonces.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
            if (localNonces.size() >= MAXIMUM_LOCAL_ENTRIES) {
                throw new BusinessException(ErrorCode.SYSTEM_BUSY, "local callback replay store reached its limit");
            }
        }
        LocalNonce supplied = new LocalNonce(digest, now.plus(ttl));
        LocalNonce existing = localNonces.compute(key,
                (ignored, current) -> current == null || !current.expiresAt().isAfter(now) ? supplied : current);
        if (!existing.digest().equals(digest)) {
            throw replayDetected();
        }
    }

    private String digest(PaymentCallbackCommand command) {
        return sha256(command.channel() + '\n' + command.channelTradeNo() + '\n' + command.paymentId() + '\n'
                + command.paidAmount().stripTrailingZeros().toPlainString() + '\n' + command.timestamp());
    }

    private String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private BusinessException replayDetected() {
        return new BusinessException(ErrorCode.FORBIDDEN, "payment callback nonce was already used");
    }

    private record LocalNonce(String digest, Instant expiresAt) {
    }
}
