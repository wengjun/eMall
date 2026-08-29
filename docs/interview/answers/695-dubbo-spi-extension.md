# 695 Dubbo SPI 的自适应扩展和自动激活如何工作？

[返回按分类学习面试题](../README.md)

## 它与 JDK SPI 的主要差异

JDK `ServiceLoader` 通常加载接口的全部实现；Dubbo `ExtensionLoader` 按名称延迟获取扩展，并增加依赖注入、包装类、自适应选择和条件自动激活，适合协议、集群、路由、序列化等大量可替换组件。

扩展名与实现类通过 `META-INF/dubbo/` 等目录下的配置关联，接口通常带 `@SPI`。名称是配置协议的一部分，升级时不能随意改名。

## `@Adaptive` 解决运行时选择

同一个 JVM 可能同时调用不同协议。编译期注入一个固定实现不够，Adaptive 扩展会从 URL 或 Invocation 参数中读取扩展名，再委托给对应实现。

```text
proxy=javassist -> JavassistProxyFactory
protocol=tri    -> TripleProtocol
cluster=failover -> FailoverCluster
```

若接口方法本身标记 `@Adaptive`，Dubbo 可生成自适应代理类；若实现类标记，则直接使用该实现作为自适应入口。缺少 URL、key 或默认扩展时应快速报出可诊断错误。

## `@Activate` 解决扩展链装配

Filter 等扩展不是从中选一个，而是按 group、配置 key、order、before/after 条件组成链。Consumer 和 Provider 可激活不同 Filter；显式配置还可追加、排除或重排默认项。

自动激活最危险的问题是“升级后无感多了一层行为”。因此自定义 Filter 要明确作用侧、顺序、异常语义和是否修改 attachment，并通过调用链测试固定顺序。

## Wrapper 与依赖注入

构造函数接收扩展接口的实现类会被识别为 wrapper，用装饰器方式包裹目标扩展。
ExtensionFactory 可为扩展 setter 注入依赖。多层 wrapper 的顺序和生命周期比普通 Spring Bean 更隐蔽，
不应在扩展对象里保存请求级可变状态。

## 一个安全的自定义扩展检查表

- 接口是否真需要全局 SPI，而不是普通 Spring 策略 Bean。
- 扩展名、默认值和 URL key 是否稳定。
- 是否线程安全，能否被单例复用。
- 异常是否保留原分类，不能把业务异常包装成可重试网络异常。
- Filter 顺序是否与认证、限流、trace、超时和业务调用一致。
- 是否有兼容旧配置的灰度与回滚路径。

## 在 eMall 中怎么讲

我会用 Dubbo SPI 承载协议级、框架级扩展，例如统一 deadline Filter；业务促销策略仍用 Spring Bean 组合，
避免把领域逻辑绑到 RPC 框架。测试应启动真实 Consumer/Provider，验证扩展装配和调用顺序，
而不仅单测 `intercept` 方法。

参考：[Dubbo SPI Extension List](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/spi/spi-list/)
