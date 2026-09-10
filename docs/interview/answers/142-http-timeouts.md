# 142 Java HTTP 超时分别在哪里设置？

[返回按分类学习面试题](../README.md)

## 用具体 API 区分超时

Java 17 JDK HttpClient 示例，外层方法需处理 IOException 和 InterruptedException：

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(200))
        .build();

HttpRequest request = HttpRequest.newBuilder(URI.create("https://catalog.example/products/42"))
        .timeout(Duration.ofMillis(800))
        .GET()
        .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

200 ms 用于新连接建立，复用已有连接时不是每次重新计时。
800 ms 是请求层超时配置，不是通用的 socket 每次 read 超时，也不覆盖业务代码在调用前后的全部工作。
若改用流式 BodyHandler，还要管理响应体读取和关闭，不能把它当任意流处理的绝对 deadline。

## 不同库的名字不等价

Reactor Netty 的 responseTimeout、池的 pendingAcquireTimeout、JDK request.timeout
不是同一个计时区间。先识别底层 transport，再配置 Spring 封装层；不要只看属性名字中都有 timeout。

`CompletableFuture.orTimeout` 主要让 Future 超时完成，不保证底层 IO 已经取消。
上层总 deadline、实际 IO 取消和资源释放需要分别验证，取消机制看 [102](102-timeout-does-not-stop-thread.md)。

JDK 请求超时契约见 [HttpRequest.Builder](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpRequest.Builder.html)。
