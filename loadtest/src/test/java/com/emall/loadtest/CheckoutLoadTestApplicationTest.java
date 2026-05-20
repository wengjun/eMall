package com.emall.loadtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
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

    @Test
    void shouldParseFlashSaleHotspotScenario() throws Exception {
        Class<?> optionsType = Class.forName("com.emall.loadtest.CheckoutLoadTestApplication$LoadTestOptions");
        Method from = optionsType.getDeclaredMethod("from", String[].class);
        from.setAccessible(true);

        Object options =
                from.invoke(null, (Object) new String[]{"http://localhost:8080", "10", "5", "3", "flash-sale-hotspot"});
        Object scenario = invoke(optionsType, options, "scenario");

        assertThat(invoke(scenario.getClass(), scenario, "cliName")).isEqualTo("flash-sale-hotspot");
    }

    @Test
    void shouldBuildReportWithBottleneckAndSafeQps() throws Exception {
        Class<?> optionsType = Class.forName("com.emall.loadtest.CheckoutLoadTestApplication$LoadTestOptions");
        Method from = optionsType.getDeclaredMethod("from", String[].class);
        from.setAccessible(true);
        Object options = from.invoke(null, (Object) new String[]{"http://localhost:8080", "10", "5", "3", "checkout"});
        Class<?> appType = Class.forName("com.emall.loadtest.CheckoutLoadTestApplication");
        var constructor = appType.getDeclaredConstructor(optionsType);
        constructor.setAccessible(true);
        Object app = constructor.newInstance(options);

        Class<?> resultType = Class.forName("com.emall.loadtest.CheckoutLoadTestApplication$Result");
        var resultConstructor = resultType.getDeclaredConstructor(boolean.class, long.class);
        resultConstructor.setAccessible(true);
        List<Object> results = List.of(resultConstructor.newInstance(true, 100L),
                resultConstructor.newInstance(true, 900L), resultConstructor.newInstance(false, 950L));
        Method buildReport = appType.getDeclaredMethod("buildReport", List.class, Duration.class);
        buildReport.setAccessible(true);

        Object report = buildReport.invoke(app, results, Duration.ofSeconds(1));

        assertThat(invoke(report.getClass(), report, "bottleneck")).isEqualTo("error-rate");
        assertThat((Double) invoke(report.getClass(), report, "safeQps")).isEqualTo(1.6d);
    }

    private Object invoke(Class<?> type, Object target, String methodName) throws Exception {
        Method method = type.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
