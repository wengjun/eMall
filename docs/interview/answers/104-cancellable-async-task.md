# 104 如何设计可取消的异步任务？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

可取消异步任务要采用协作式取消：任务定期检查取消标记或中断状态，阻塞 IO 设置超时，取消时释放资源，
并保证业务幂等和状态可恢复。对 `Future` 可以使用 `cancel(true)`，但任务代码必须响应中断才有效。

取消设计要覆盖线程、IO、业务状态和补偿。

## 取消标记

可以使用取消标记：

```java
class JobContext {
    private final AtomicBoolean cancelled = new AtomicBoolean();

    boolean isCancelled() {
        return cancelled.get();
    }

    void cancel() {
        cancelled.set(true);
    }
}
```

任务循环中检查：

```java
if (context.isCancelled()) {
    return;
}
```

## 响应中断

如果任务提交到线程池，取消时可以：

```java
future.cancel(true);
```

任务内部要响应：

```java
if (Thread.currentThread().isInterrupted()) {
    return;
}
```

阻塞方法捕获 `InterruptedException` 后要恢复中断状态并退出。

## IO 超时

异步任务经常调用外部系统。

必须设置：

- HTTP connect timeout。
- HTTP read timeout。
- DB query timeout。
- Redis command timeout。
- MQ send timeout。

否则任务收到取消信号后，仍可能卡在不可中断 IO 上。

## 分阶段提交

长任务要拆成阶段。

每个阶段完成后记录进度。

取消发生时，任务可以：

- 停止后续阶段。
- 保存当前状态。
- 释放资源。
- 交给补偿任务恢复。

这比一个巨大事务跑到底更稳定。

## 幂等和补偿

取消可能发生在业务操作中间。

必须设计：

- 幂等 key。
- 状态机。
- 操作日志。
- 补偿任务。
- 可重试边界。

否则取消后重试可能造成重复扣款、重复发货或重复扣库存。

## 在 eMall 项目中怎么讲？

订单超时关闭任务可以设计成可取消：

- 扫描待关闭订单。
- 每批处理前检查取消标记。
- 数据库更新使用状态条件和幂等。
- 调用库存释放时设置超时。
- 记录处理进度。
- 服务关闭时停止拉新任务。

这样 Pod 下线不会留下不可控后台任务。
