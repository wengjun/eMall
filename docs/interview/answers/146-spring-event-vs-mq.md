# 146 Spring 事件如何绑定执行线程和事务阶段？

[返回按分类学习面试题](../README.md)

## 学 Spring 的机制，不重讲 MQ 架构

默认 ApplicationEventPublisher 通过同步事件广播器调用监听器，发布调用可能直到监听器返回才结束。
显式配置异步执行器或 @Async 后，线程、异常与事务传播规则会变化，不能再依赖发布线程的 ThreadLocal。

```java
record ProductChanged(long productId) {
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onChanged(ProductChanged event) {
    localCache.invalidate(event.productId());
}
```

这是监听 Bean 中的片段，需要事务事件注解的 imports，localCache 为注入依赖。
默认只有发布时存在事务才会安排这个监听器；无事务时不会因为名字叫 AFTER_COMMIT 就立即执行。
`fallbackExecution = true` 才允许无事务时也执行。

## AFTER_COMMIT 最容易误用的点

监听器执行时数据库事务已经提交，不能靠抛异常撤销原提交。
此时原事务资源可能仍可访问，但再写数据不能假定会被提交；
确需新写入时，应通过另一个代理 Bean 的 REQUIRES_NEW 方法开启独立事务。

进程崩溃可能丢失尚未执行的监听器。事务事件用于阶段回调，不是持久消息，
@Async 也不会给它增加落盘、重放或跨进程投递能力。
