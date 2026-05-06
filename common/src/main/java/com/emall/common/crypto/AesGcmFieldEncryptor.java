package com.emall.common.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesGcmFieldEncryptor implements FieldEncryptor {
    private static final String CIPHER_PREFIX = "v1:";
    private static final String HASH_PREFIX = "v1:";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKeySpec encryptionKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom secureRandom;

    public AesGcmFieldEncryptor(String keyMaterial) {
        if (keyMaterial == null || keyMaterial.isBlank()) {
            throw new IllegalArgumentException("field encryption key must not be blank");
        }
        this.encryptionKey = new SecretKeySpec(sha256("aes:" + keyMaterial), "AES");
        this.hmacKey = new SecretKeySpec(sha256("hmac:" + keyMaterial), "HmacSHA256");
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer payload = ByteBuffer.allocate(iv.length + encrypted.length);
            payload.put(iv);
            payload.put(encrypted);
            return CIPHER_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("failed to encrypt field", ex);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        if (!ciphertext.startsWith(CIPHER_PREFIX)) {
            return ciphertext;
        }
        byte[] payload = Base64.getUrlDecoder().decode(ciphertext.substring(CIPHER_PREFIX.length()));
        if (payload.length <= IV_BYTES) {
            throw new IllegalArgumentException("encrypted field payload is invalid");
        }
        byte[] iv = new byte[IV_BYTES];
        byte[] encrypted = new byte[payload.length - IV_BYTES];
        System.arraycopy(payload, 0, iv, 0, IV_BYTES);
        System.arraycopy(payload, IV_BYTES, encrypted, 0, encrypted.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("failed to decrypt field", ex);
        }
    }

    @Override
    public String lookupHash(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] digest = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return HASH_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("failed to hash field", ex);
        }
    }

    private static byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
