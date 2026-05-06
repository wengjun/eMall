package com.emall.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class CheckoutSmokeApplicationTest {
    @Test
    void shouldTrimTrailingSlashFromBaseUrl() throws Exception {
        Method method = CheckoutSmokeApplication.class.getDeclaredMethod("trimTrailingSlash", String.class);
        method.setAccessible(true);

        assertThat(method.invoke(null, "http://localhost:8080/")).isEqualTo("http://localhost:8080");
        assertThat(method.invoke(null, "http://localhost:8080")).isEqualTo("http://localhost:8080");
    }
}
