package com.emall.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AesGcmFieldEncryptorTest {
    private final AesGcmFieldEncryptor fieldEncryptor = new AesGcmFieldEncryptor("unit-test-key");

    @Test
    void encryptsWithRandomizedCiphertextAndDecryptsToOriginalValue() {
        String first = fieldEncryptor.encrypt("15500000000");
        String second = fieldEncryptor.encrypt("15500000000");

        assertThat(first).isNotEqualTo("15500000000");
        assertThat(first).isNotEqualTo(second);
        assertThat(fieldEncryptor.decrypt(first)).isEqualTo("15500000000");
        assertThat(fieldEncryptor.decrypt(second)).isEqualTo("15500000000");
    }

    @Test
    void lookupHashIsDeterministicAndDoesNotExposePlaintext() {
        String first = fieldEncryptor.lookupHash("15500000000");
        String second = fieldEncryptor.lookupHash("15500000000");

        assertThat(first).isEqualTo(second);
        assertThat(first).doesNotContain("15500000000");
    }
}
