package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CheckoutLoadTestApplicationTest {
    @Test
    void shouldParseCliOptionsAndNormalizeBaseUrl() throws Exception {
        Class<?> optionsType = Class.forName("com.emall.loadtest.CheckoutLoadTestApplication$LoadTestOptions");
        Method from = optionsType.getDeclaredMethod("from", String[].class);
        from.setAccessible(true);

        Object options =
                from.invoke(null, (Object) new String[]{"http://localhost:8080/", "10", "5", "3", "read-heavy"});
        Object scenario = invoke(optionsType, options, "scenario");

        assertThat(invoke(optionsType, options, "baseUrl")).isEqualTo("http://localhost:8080");
        assertThat(invoke(optionsType, options, "ratePerSecond")).isEqualTo(10);
        assertThat(invoke(optionsType, options, "duration")).isEqualTo(Duration.ofSeconds(5));
        assertThat(invoke(optionsType, options, "maxConcurrency")).isEqualTo(3);
        assertThat(invoke(scenario.getClass(), scenario, "cliName")).isEqualTo("read-heavy");
    }

    private Object invoke(Class<?> type, Object target, String methodName) throws Exception {
        Method method = type.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
