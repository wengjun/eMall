package com.emall.common.crypto;

public interface FieldEncryptor {
    String encrypt(String plaintext);

    String decrypt(String ciphertext);

    String lookupHash(String plaintext);
}
