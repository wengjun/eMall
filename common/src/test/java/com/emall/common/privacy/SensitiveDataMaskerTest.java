package com.emall.common.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {
    @Test
    void masksKnownSensitiveTypes() {
        assertThat(SensitiveDataMasker.mask(SensitiveDataType.MOBILE, "13800000000")).isEqualTo("138****0000");
        assertThat(SensitiveDataMasker.mask(SensitiveDataType.PAYMENT_REFERENCE, "trade-abcdef-123456"))
                .doesNotContain("abcdef");
        assertThat(SensitiveDataMasker.mask(SensitiveDataType.TOKEN, "abcdefghijklmnopqrstuvwxyz")).startsWith("abcd")
                .endsWith("wxyz");
    }

    @Test
    void masksSensitiveValuesInsideTextPayload() {
        String payload = """
                {"mobile":"13800000000","channelTradeNo":"trade-secret","signature":"secret"}
                """;

        String masked = SensitiveDataMasker.maskFreeText(payload);

        assertThat(masked).contains("138****0000");
        assertThat(masked).doesNotContain("trade-secret");
        assertThat(masked).doesNotContain("secret");
        assertThat(SensitiveDataMasker.containsRawSensitiveData(masked)).isFalse();
    }
}
