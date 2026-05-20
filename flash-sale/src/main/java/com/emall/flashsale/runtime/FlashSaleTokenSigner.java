package com.emall.flashsale.runtime;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class FlashSaleTokenSigner {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;

    public FlashSaleTokenSigner(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(long tokenId, long campaignId, long userId, long skuId, int quantity, Instant expiresAt) {
        String body = base36(tokenId) + "." + base36(campaignId) + "." + base36(userId) + "." + base36(skuId) + "."
                + Integer.toString(quantity, 36) + "." + base36(expiresAt.getEpochSecond());
        return body + "." + signature(body);
    }

    public boolean verify(String token) {
        int lastDot = token.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == token.length() - 1) {
            return false;
        }
        String body = token.substring(0, lastDot);
        String providedSignature = token.substring(lastDot + 1);
        return MessageDigest.isEqual(providedSignature.getBytes(StandardCharsets.UTF_8),
                signature(body).getBytes(StandardCharsets.UTF_8));
    }

    private String signature(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8))).substring(0, 22);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to sign flash sale token", ex);
        }
    }

    private String base36(long value) {
        return Long.toString(value, 36);
    }
}
