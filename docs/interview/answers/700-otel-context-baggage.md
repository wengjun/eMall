# 700 OpenTelemetry Context 和 Baggage 如何跨线程、跨服务传播？

[返回按分类学习面试题](../README.md)

## Context 是传播容器

OpenTelemetry `Context` 保存当前 Span 等跨 API 边界状态。HTTP/RPC 客户端使用 Propagator 把它注入请求头，
服务端再提取；默认常见格式是 W3C `traceparent`/`tracestate`。如果只创建 Span 却不传播 Context，
各服务会产生互不关联的新 trace。

```text
入站请求头 -> extract -> Server Span/Context
                         -> inject -> 出站请求头
```

`Context` 通常是不可变值，`makeCurrent()` 返回的 `Scope` 必须在同一执行流中关闭，避免线程池复用时把上一个请求的身份带给下一个请求。

## Java 跨线程的正确写法

```java
Context captured = Context.current();
executor.execute(captured.wrap(() -> {
    Span span = tracer.spanBuilder("reserve-inventory").startSpan();
    try (Scope ignored = span.makeCurrent()) {
        inventoryClient.reserve();
    } catch (RuntimeException exception) {
        span.recordException(exception);
        throw exception;
    } finally {
        span.end();
    }
}));
```

更推荐使用经过 OpenTelemetry instrumentation 包装的 Executor、HTTP 客户端和 Dubbo/Kafka 组件，减少手工漏传。
`CompletableFuture`、Reactor、虚拟线程和消息消费有不同调度边界，必须用集成测试确认父子关系，
不能假设普通 `ThreadLocal` 自动复制。

## Baggage 与 Span Attribute 不同

Baggage 是会随请求向下游传播的键值；Span Attribute 只附着在当前 Span。
把 `tenant.tier=gold` 放入 Baggage 后，下游可用于采样或路由，但它不会自动成为每个 Span 的属性，需要显式复制。

Baggage 会穿越服务边界并增加请求头大小，不能放密码、Token、身份证号、完整用户 ID 等敏感信息，也不能让未信任客户端自由注入并影响授权。入口应做白名单、长度限制和覆盖策略。

## 消息链路的特殊点

Producer 将 Context 注入消息 header，Consumer 提取后创建 `CONSUMER` Span。
消息可能排队数小时，不能一直维持进程内 Scope；应传播序列化上下文。
批量消费多个独立 trace 时可使用 Span Link，而不是强行选择一个父 Span。

## 电商系统验收标准

从网关下单到订单、库存、支付和 outbox/Kafka 的 trace ID 应连续；线程池切换后父子关系不丢，
重试 attempt 可区分，日志自动带 trace/span ID。还要测试恶意超长 baggage、无 trace header 和错误 header，
确保观测逻辑不会影响业务可用性。

参考：

- [OpenTelemetry Context Propagation](https://opentelemetry.io/docs/concepts/context-propagation/)
- [OpenTelemetry Baggage](https://opentelemetry.io/docs/concepts/signals/baggage/)
