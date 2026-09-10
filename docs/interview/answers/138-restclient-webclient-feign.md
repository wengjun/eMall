# 138 RestClient、WebClient、Feign 如何取舍？

[返回按分类学习面试题](../README.md)

## 对应 Java 编程模型

| 客户端 | 业务代码看到的模型 | 需要学习的部分 |
| --- | --- | --- |
| Spring RestClient | 同步返回值 | builder、request factory、状态码处理 |
| Spring WebClient | Mono / Flux | 订阅、操作符、调度与上下文 |
| Spring Cloud OpenFeign | 声明式接口 | 代理、编码/解码器、底层传输与配置 |

RestClient 从 Spring Framework **6.1** 提供，Java 17 可以使用。
Feign 接口短不代表没有阻塞；WebClient 返回 Mono 也不代表其中调用的 MyBatis 变成非阻塞。

```java
import org.springframework.web.client.RestClient;

record ProductView(long id, String name) {
}

RestClient client = RestClient.builder().baseUrl("https://catalog.example").build();
ProductView product = client.get().uri("/products/{id}", 42)
        .retrieve().body(ProductView.class);
```

这是调用形态示例，域名为占位地址。真实连接池和超时由底层传输配置决定，见
[141](141-http-client-connection-pool.md)、[142](142-http-timeouts.md)。

重点掌握已有代码使用的客户端即可，不需要为了学习而在同一接口接入三套实现。
Dubbo 是另一套 RPC 调用链，见 [693](693-dubbo-invocation-pipeline.md)。
