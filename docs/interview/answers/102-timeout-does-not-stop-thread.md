# 102 任务超时后线程是否真的停止？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

不一定。很多超时只是调用方不再等待结果，底层任务线程可能仍在运行。`Future.get(timeout)` 超时不会自动杀死线程；
`CompletableFuture.orTimeout` 也主要是让 future 超时完成。要真正停止任务，需要任务支持取消、中断、超时 IO 或协作式退出。

生产中要区分“调用方超时”和“任务真正停止”。

## 调用方超时

示例：

```java
future.get(100, TimeUnit.MILLISECONDS);
```

如果 100 ms 未完成，调用方得到 `TimeoutException`。

但执行任务的线程可能还在继续跑。

这意味着资源仍然被占用。

## cancel(true)

可以调用：

```java
future.cancel(true);
```

`true` 表示尝试中断执行线程。

注意是“尝试”。

如果任务不响应中断，仍然不会停。

## 中断不是强杀

Java 中断是协作机制。

线程需要主动检查：

```java
Thread.currentThread().isInterrupted()
```

或者调用可中断阻塞方法时抛出 `InterruptedException`。

如果代码是死循环且不检查中断，中断无法让它停止。

## IO 超时很重要

下游 HTTP 或数据库调用必须设置底层超时。

否则即使上层 future 超时，底层 socket 可能仍然阻塞。

需要设置：

- connect timeout。
- read timeout。
- request timeout。
- connection acquire timeout。

超时必须贯穿调用链。

## 为什么这很危险？

如果请求层超时返回，但任务仍在后台运行：

- 线程继续被占用。
- 下游继续被调用。
- 数据库连接继续占用。
- 用户重试会叠加新任务。
- 可能造成重复写入。

这会放大雪崩风险。

## 电商系统实践

订单创建调用库存超时后，不能只让前端超时返回。

还要确认：

- 库存 HTTP 客户端是否有 read timeout。
- 后台任务是否可取消。
- 订单操作是否幂等。
- 下游是否会继续扣库存。
- 超时后是否有补偿和状态确认。

否则可能出现用户看到失败，但库存稍后被扣减。
