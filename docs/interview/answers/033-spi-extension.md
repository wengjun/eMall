# 033 SPI 机制适合解决什么扩展问题？

[返回按分类学习面试题](../README.md)

## 只补 Java 的发现和加载方式

扩展接口设计你已经熟悉。JDK SPI 需要掌握的是 ServiceLoader 如何从 classpath 找到实现。
以支付渠道接口为例，下面两个 public 类型分别放在同名 Java 文件中：

```java
package example;

public interface PaymentChannel {
    String name();
}
```

```java
package example;

public final class SandboxChannel implements PaymentChannel {
    @Override
    public String name() {
        return "sandbox";
    }
}
```

资源文件 `META-INF/services/example.PaymentChannel` 写入一行 `example.SandboxChannel`。
classpath 模式的实现类需有可访问的 public 无参构造器。

```java
var loader = java.util.ServiceLoader.load(example.PaymentChannel.class);
for (example.PaymentChannel channel : loader) {
    System.out.println(channel.name());
}
```

Provider 通常按需加载并缓存；ServiceLoader 本身不保证并发安全。
找不到实现时检查资源打包、实现类可见性和线程上下文 ClassLoader。
使用 JPMS 时改由 `uses`/`provides ... with ...` 声明，不能照搬 classpath 的全部规则。
这些行为见 [Java 17 ServiceLoader](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html)。

Dubbo 的按名称查找、自适应扩展和自动激活另看 [695](695-dubbo-spi-extension.md)，不是 JDK SPI 自带功能。
