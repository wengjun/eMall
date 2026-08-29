# 092 CompletableFuture 默认线程池有什么风险？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`CompletableFuture.supplyAsync` 如果不指定 Executor，默认使用公共 `ForkJoinPool.commonPool()`。
风险是多个业务共享同一个公共池，阻塞 IO、慢任务或突发流量会互相影响，导致线程饥饿、P99 升高和故障扩散。

生产代码应该为不同业务和下游指定有界线程池。

## 默认行为

示例：

```java
CompletableFuture.supplyAsync(() -> loadOrder(orderId));
```

没有传入 executor 时，会使用默认异步执行器，通常是 `ForkJoinPool.commonPool()`。

这对 demo 方便，对生产服务有风险。

## 风险一：业务互相影响

公共池是共享的。

如果订单查询、推荐计算、营销规则都用默认池，一个业务慢会占用公共线程，影响其他业务。

这违反了隔离原则。

## 风险二：阻塞 IO 不适合 commonPool

`ForkJoinPool` 更适合 CPU 计算和拆分任务。

如果里面执行阻塞 IO，例如 HTTP、数据库、Redis 调用，线程会被长时间占住。

结果是：

- 新任务无法调度。
- 队列堆积。
- P99 升高。
- 故障扩散。

## 风险三：不可控

默认池参数不是按你的业务容量设计的。

你很难针对某个下游设置：

- 最大并发。
- 队列长度。
- 拒绝策略。
- 线程名。
- 指标标签。
- 隔离策略。

没有这些能力，就很难做生产治理。

## 正确方式

生产中显式指定线程池：

```java
CompletableFuture.supplyAsync(() -> loadOrder(orderId), orderQueryExecutor);
```

并且线程池应该：

- 有明确名称。
- 有界队列。
- 有拒绝策略。
- 有指标监控。
- 和业务或下游隔离。
- 设置超时。

## 在 eMall 项目中怎么讲？

订单详情并行查物流、支付和售后时，不应该都丢到默认 commonPool。

更合理的是按下游或业务类型隔离：

- logisticsExecutor。
- paymentQueryExecutor。
- afterSalesExecutor。

支付查询慢不应该拖垮物流和售后查询。
