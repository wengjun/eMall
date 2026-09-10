# 133 Spring profile、属性绑定和动态配置如何配合？

[返回按分类学习面试题](../README.md)

## 不重讲配置治理，关注 Java 对象是否更新

profile 选择配置和 Bean 条件，不是一套把任意对象自动热更新的机制。
@ConfigurationProperties 适合成组类型化绑定，比散落的 @Value 更容易校验和理解。

```java
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("catalog.client")
public record CatalogClientProperties(Duration timeout, int maxConnections) {
    public CatalogClientProperties {
        if (timeout == null || timeout.isZero() || timeout.isNegative() || maxConnections < 1) {
            throw new IllegalArgumentException("invalid catalog client configuration");
        }
    }
}
```

配置扫描或 @EnableConfigurationProperties 注册这个类型后，才能完成绑定。
例如 catalog.client.timeout: 800ms 可以绑定 Duration；构造器校验让非法初始配置尽早暴露。

## 动态刷新并非自动重建所有依赖

Nacos 收到新文本、Spring Environment 更新、属性对象重新绑定、
HTTP 客户端重建是不同步骤。客户端在构造时复制了 timeout，修改属性对象不会自动改变已有连接池。

@RefreshScope 来自 Spring Cloud，不是 @ConfigurationProperties 的默认能力；
代理重建的范围与依赖生命周期都要验证。
不可变 record 很适合启动绑定或配置快照，但不能假设它天然支持所有 starter 的热刷新路径。

Nacos Java Listener 和完整快照发布见 [697](697-nacos-config-push-recovery.md)。
