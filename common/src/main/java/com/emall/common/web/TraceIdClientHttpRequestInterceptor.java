package com.emall.common.web;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class TraceIdClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        String traceId = TraceHeaders.currentTraceId();
        if (traceId != null && !traceId.isBlank()) {
            request.getHeaders().set(TraceHeaders.TRACE_ID_HEADER, traceId);
        }
        String requestId = TraceHeaders.currentRequestId();
        if (requestId != null && !requestId.isBlank()) {
            request.getHeaders().set(TraceHeaders.REQUEST_ID_HEADER, requestId);
        }
        return execution.execute(request, body);
    }
}
