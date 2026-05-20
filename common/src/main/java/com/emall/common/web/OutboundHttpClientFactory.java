package com.emall.common.web;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class OutboundHttpClientFactory {
    private final OutboundHttpClientProperties properties;
    private final ExecutorService executor;
    private final HttpClient httpClient;

    public OutboundHttpClientFactory(OutboundHttpClientProperties properties) {
        this.properties = properties;
        this.executor = Executors.newFixedThreadPool(properties.getMaxConnections());
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).executor(executor).build();
    }

    public RestClient restClient(String clientName, String baseUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
                .requestInterceptor(new TraceIdClientHttpRequestInterceptor())
                .requestInterceptor(new OutboundHttpClientInterceptor(clientName, properties)).build();
    }

    public Duration timeoutBudget() {
        return properties.getConnectTimeout().plus(properties.getReadTimeout());
    }
}
