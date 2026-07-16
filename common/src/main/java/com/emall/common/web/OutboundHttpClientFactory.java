package com.emall.common.web;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class OutboundHttpClientFactory implements AutoCloseable {
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
        return restClient(clientName, baseUrl, httpClient);
    }

    public RestClient restClient(String clientName, String baseUrl, SSLContext sslContext) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).executor(executor)
                .sslContext(sslContext).build();
        return restClient(clientName, baseUrl, client);
    }

    private RestClient restClient(String clientName, String baseUrl, HttpClient client) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
                .requestInterceptor(new TraceIdClientHttpRequestInterceptor())
                .requestInterceptor(new OutboundHttpClientInterceptor(clientName, properties)).build();
    }

    public Duration timeoutBudget() {
        return properties.getConnectTimeout().plus(properties.getReadTimeout());
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
