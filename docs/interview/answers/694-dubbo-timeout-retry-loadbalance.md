# 694 Dubbo 的超时、重试和容错配置如何生效？

[返回按分类学习面试题](../README.md)

## 重点是 Java 配置语义

Dubbo 的接口、方法级配置和 Consumer/Provider 默认值有覆盖关系。
定位时看具体调用的最终 URL/配置，不能只确认 application.yml 里出现过 timeout。

```java
@DubboReference(timeout = 300, retries = 0, cluster = "failfast")
private InventoryQueryService inventoryQueryService;
```

这是注入点片段，使用 Dubbo 3.x 的注解，接口定义由 API 模块提供。
300 是演示超时毫秒数；retries 表示失败后的重试次数，不是包含首次调用的总次数。
例如 failover 配置 retries=2 时，某些可重试失败最多可能触发 3 次尝试。

## 容错扩展不等于业务语义

failover 可重新选择 Provider 并重试；failfast 尽快报告一次调用失败；
failsafe 可能吞掉失败，不能把返回“成功”当作写操作已落地。
failback 的框架后台重试也不能直接当作持久任务系统。

超时不表示 Provider 没有执行，更不会自动回滚其事务。
Dubbo timeout 是否覆盖到排队、传播或其他阶段，要结合版本和调用路径检查，不能当整个 HTTP 请求的总 deadline。

负载均衡是选择 Invoker 的一环；重试、路由和 Directory 更新也会影响实际目标。
从哪个配置读值、哪里创建集群 Invoker，沿 [693](693-dubbo-invocation-pipeline.md) 的链路阅读。
此处不再展开幂等、重试风暴和容量预算的通用设计。
