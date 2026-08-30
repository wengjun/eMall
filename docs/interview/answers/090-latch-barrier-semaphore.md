# 090 CountDownLatch、CyclicBarrier、Semaphore 分别适合什么场景？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`CountDownLatch` 适合一个线程等待多个任务完成，一次性使用；`CyclicBarrier` 适合一组线程互相等待，
到齐后一起继续，并且可以循环使用；`Semaphore` 适合控制并发许可数量，例如限制同时访问某个资源的线程数。

三者都是协调线程，不是替代业务限流和分布式协调的万能工具。

## CountDownLatch

`CountDownLatch` 是倒计时门闩。

初始化一个计数，任务完成后 `countDown()`，等待线程调用 `await()`。

示例：

```java
CountDownLatch latch = new CountDownLatch(3);

executor.submit(() -> {
    loadUser();
    latch.countDown();
});

latch.await();
```

适合：

- 主线程等待多个子任务完成。
- 并发测试同时发起请求。
- 启动时等待多个组件初始化。

特点是一次性使用，计数归零后不能重置。

## CyclicBarrier

`CyclicBarrier` 是循环屏障。

多个线程都调用 `await()`，等到指定数量线程都到达后，一起继续执行。

适合：

- 多线程分阶段计算。
- 所有参与者到齐后进入下一轮。
- 并行任务每轮同步。

它可以循环使用。

如果某个线程失败，屏障可能被破坏，其他线程会收到异常。

## Semaphore

`Semaphore` 是信号量，控制许可数量。

示例：

```java
Semaphore semaphore = new Semaphore(100);

if (semaphore.tryAcquire()) {
    try {
        callDownstream();
    } finally {
        semaphore.release();
    }
}
```

适合：

- 限制并发访问数量。
- 保护本地资源。
- 控制同时执行任务数。
- 做轻量舱壁隔离。

注意必须释放许可，否则会造成许可泄漏。

## 三者区别

| 工具 | 核心用途 | 是否可复用 |
| --- | --- | --- |
| CountDownLatch | 等多个任务完成 | 否 |
| CyclicBarrier | 多线程互相等待到齐 | 是 |
| Semaphore | 控制并发许可 | 是 |

## 生产使用注意

注意点：

- `await()` 要考虑超时。
- 异常时要释放资源。
- `Semaphore` 要在 finally 中 release。
- 不要在 Web 请求中无限等待。
- 不要用单机同步工具解决分布式协调。

这些工具只在当前 JVM 内有效。

## 电商系统实践

`CountDownLatch` 可以用于集成测试中等待多个异步消息处理完成。

`Semaphore` 可以用于限制某个下游在单实例内最多并发 100 个请求，避免下游被打爆。

`CyclicBarrier` 在业务服务中相对少见，更常用于并行计算或测试场景。

分布式秒杀限流不能只靠单 JVM `Semaphore`，需要网关、Redis、令牌桶或流量平台。
