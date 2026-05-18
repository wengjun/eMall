package com.emall.chaos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ApiContractDocumentationIT {

    private static final Path WEB_APP_CHECKOUT_API = Path.of("..", "docs", "api", "web-app-checkout.openapi.yml");

    @Test
    void shouldDocumentWebAndAppCheckoutContract() throws IOException {
        String contract = Files.readString(WEB_APP_CHECKOUT_API);

        assertThat(contract)
                .contains("https://api.emall.example.com")
                .contains("X-Device-Id")
                .contains("X-Client-Type")
                .contains("X-Client-Channel")
                .contains("clientType")
                .contains("deviceId")
                .contains("channel")
                .contains("WEB")
                .contains("APP");
    }
}
