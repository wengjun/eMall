# 141 如何配置 Java HTTP 客户端的连接池？

[返回按分类学习面试题](../README.md)

## 先认底层实现，别套用同一组参数

Spring 的调用 API 和实际传输库是两层。RestClient 使用 ClientHttpRequestFactory，
WebClient 使用 ClientHttpConnector；连接复用、池上限和租借超时属于底层实现。

下面是 WebClient + Reactor Netty 的配置片段，需要 spring-webflux 和 reactor-netty-http：

```java
import java.time.Duration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

ConnectionProvider provider = ConnectionProvider.builder("catalog")
        .maxConnections(32)
        .pendingAcquireMaxCount(64)
        .pendingAcquireTimeout(Duration.ofMillis(100))
        .maxIdleTime(Duration.ofSeconds(30))
        .build();

WebClient client = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
        .build();
```

数值只演示 API，不是推荐容量。pendingAcquireMaxCount 限制等待获取连接的数量，
pendingAcquireTimeout 限制等待时间，两者与建立新连接的 connect timeout 不同。
自建 provider 应随应用关闭，不能在每次请求里创建一个池。

## JDK 17 HttpClient 的区别

JDK HttpClient 也复用连接，但其公共 builder 不提供上述 Reactor Netty 的 maxConnections 或
pendingAcquire 配置。不要把这些配置项直接套到 JDK 客户端上。
客户端对象应复用，不能每次请求 new 一个来“获得隔离”。
JDK 客户端的公开能力见 [Java 17 HttpClient](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html)。

HTTP/2 会复用连接上的多个流，所以连接数不等于并发请求数。
