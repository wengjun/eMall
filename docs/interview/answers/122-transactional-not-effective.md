# 122 @Transactional 为什么有时不生效？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`@Transactional` 不生效通常是因为调用没有经过 Spring 代理、方法不可代理、异常类型不触发回滚、
事务管理器不匹配、数据库操作不在同一事务资源中，或者事务被错误捕获。最常见的是同类自调用绕过代理。

排查时要先确认目标方法是否由 Spring 管理，并且外部调用是否经过代理对象。

## 自调用绕过代理

同一个类内部调用事务方法：

```java
public void outer() {
    inner();
}

@Transactional
public void inner() {
}
```

`inner()` 是 `this.inner()`，不会经过代理对象，因此事务切面不执行。

这是最常见原因。

## 方法不可代理

可能原因：

- private 方法。
- final 方法。
- final 类。
- 静态方法。
- 非 Spring Bean。

Spring AOP 基于代理，不能增强所有调用形式。

## 异常类型问题

默认情况下，Spring 事务对 unchecked exception 和 `Error` 回滚。

checked exception 默认不回滚。

如果需要 checked exception 回滚，要配置：

```java
@Transactional(rollbackFor = Exception.class)
```

如果异常被 catch 后吞掉，事务也可能正常提交。

## 事务管理器问题

多数据源时可能存在多个事务管理器。

如果使用了错误事务管理器，事务可能没有作用到目标数据源。

要明确：

- 使用哪个 `PlatformTransactionManager`。
- Mapper 或 Repository 连接哪个数据源。
- 是否跨库。

## 数据库和引擎问题

事务还依赖数据库能力。

例如 MySQL 中 MyISAM 不支持事务。

即使 Spring 开启事务，底层数据库不支持也无法回滚。

## 异步线程问题

事务上下文通常绑定当前线程。

如果事务方法里启动新线程或异步任务，新线程不会自动继承当前事务。

这会导致异步写库不在原事务内。

## 电商系统实践

订单创建中，如果 `createOrder()` 内部直接调用同类 `deductInventoryInTx()`，后者事务可能不生效。

更合理的是把事务边界放在外部可代理的应用服务方法，或者拆到另一个 Spring Bean 中。

同时避免事务里调用远程库存或支付服务。
