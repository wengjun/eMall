# 139 Java 阻塞调用如何接入 Reactor？

[返回按分类学习面试题](../README.md)

## 只补 Reactor 的执行语义

Mono/Flux 描述处理过程，不等于已经执行。
尤其要分清 `Mono.just(load())` 会先同步调用 load，而 `Mono.fromCallable(this::load)`
把调用推迟到订阅。冷发布者被订阅多次时，工作也可能执行多次。

下面是“已有同步查询接入 WebFlux”的局部适配，不是把 JDBC 改成了非阻塞驱动：

```java
Mono<OrderView> result = Mono.fromCallable(() -> orderQuery.load(orderId))
        .subscribeOn(Schedulers.boundedElastic());
```

`subscribeOn` 影响上游订阅/执行的调度；`publishOn` 主要切换其后的操作符执行线程。
不要在 Netty event loop 中直接执行阻塞查询或调用 block。

## Spring 事务与上下文

如果 orderQuery 是另一个 Spring Bean 上的 @Transactional 方法，代理可以在实际执行查询的工作线程开启事务。
反过来，在返回 Mono 的外围命令式方法标注事务，并不能让未来订阅后的 JDBC 工作自动继承事务。

Reactor Context 与 ThreadLocal 是不同机制；线程切换后的 MDC 和事务上下文不能依赖原线程。
调度池有界也不等于整个业务无限可接收，出现排队或拒绝要显式处理。
