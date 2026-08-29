# 093 为什么生产代码不能随意使用公共 ForkJoinPool？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

公共 `ForkJoinPool` 是 JVM 级共享资源，多个框架和业务都可能使用。随意把阻塞 IO、慢任务、
大计算放进去，会造成线程饥饿和业务互相影响。它也不方便按业务设置队列、拒绝、限流和监控。

生产服务应该使用显式、命名、有界、可监控的业务线程池。

## commonPool 的定位

`ForkJoinPool.commonPool()` 是公共共享池。

它常被这些能力隐式使用：

- `CompletableFuture` 默认异步方法。
- parallel stream。
- 某些框架内部任务。

这意味着你不是唯一使用者。

## 风险一：阻塞任务占满线程

ForkJoinPool 适合 fork/join 风格的计算任务。

如果放入阻塞 IO：

- HTTP 调用。
- 数据库查询。
- Redis 调用。
- 文件 IO。

线程会被长时间占住，其他任务无法执行。

## 风险二：缺少业务隔离

公共池没有业务边界。

推荐系统慢可能影响订单查询，报表任务可能影响实时请求。

这类故障很难排查，因为表面看是“线程池慢”，本质是多个业务争抢同一公共资源。

## 风险三：可观测性差

生产线程池需要：

- 线程名。
- active count。
- queue size。
- rejected count。
- task latency。
- 业务标签。

commonPool 很难按业务维度做精细治理。

## parallelStream 的坑

`parallelStream()` 默认也会使用 commonPool。

在 Web 请求中随意使用：

```java
orders.parallelStream().map(this::calculate).toList();
```

可能让请求线程把任务扔进公共池，和其他业务争抢资源。

除非明确评估，否则核心链路应避免随意使用 parallel stream。

## 在 eMall 项目中怎么讲？

营销规则计算如果使用 parallel stream，可能占满 commonPool。

此时订单详情页的 `CompletableFuture` 默认任务也使用 commonPool，就会被营销计算拖慢。

这就是典型的共享池故障扩散。
