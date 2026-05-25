# 126 事务隔离级别如何配置？

[返回按分类学习面试题](../README.md)

## 题目

事务隔离级别如何配置？

## 先给面试官的短答案

Spring 可以通过 `@Transactional(isolation = Isolation.xxx)` 配置隔离级别，例如 `READ_COMMITTED`、
`REPEATABLE_READ`、`SERIALIZABLE`。隔离级别决定脏读、不可重复读、幻读和锁竞争的权衡。
实际效果还取决于数据库实现，例如 MySQL InnoDB 默认通常是 `REPEATABLE_READ`。

生产中不要盲目提高隔离级别，要结合一致性需求和性能成本。

## 常见隔离级别

常见级别：

- `READ_UNCOMMITTED`。
- `READ_COMMITTED`。
- `REPEATABLE_READ`。
- `SERIALIZABLE`。
- `DEFAULT`。

`DEFAULT` 表示使用数据库默认隔离级别。

## Spring 配置方式

示例：

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void createOrder() {
}
```

也可以在数据库连接池或数据库层设置默认隔离级别。

项目要避免同一服务中隔离级别混乱。

## 隔离级别解决什么？

主要问题：

- 脏读：读到未提交数据。
- 不可重复读：同一事务两次读同一行结果不同。
- 幻读：同一事务两次范围查询结果集不同。

隔离越高，一致性越强，但并发性能和锁冲突成本通常越高。

## 数据库差异

不同数据库实现不同。

例如 MySQL InnoDB 的 `REPEATABLE_READ` 通过 MVCC 和锁机制处理很多场景。

PostgreSQL 的隔离语义也有自己的实现细节。

面试中要说明：Spring 只是传递隔离级别，最终行为由数据库决定。

## 不要滥用 SERIALIZABLE

`SERIALIZABLE` 一致性最强，但并发成本高。

在高并发电商系统中，盲目使用可能导致：

- 锁等待。
- 死锁。
- 吞吐下降。
- P99 升高。

通常优先用业务约束、唯一键、乐观锁和条件更新解决具体一致性问题。

## 在 eMall 项目中怎么讲？

库存扣减不一定靠提高隔离级别解决。

更常见是：

```sql
update inventory
set available = available - ?
where sku_id = ? and available >= ?
```

用条件更新保证不超卖。

隔离级别只是事务一致性工具之一，不替代业务并发设计。

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../assets/spring-service-stack.svg)

Spring 题要从框架机制讲到业务边界。Controller 负责协议适配，Service 负责业务事务，Repository 负责数据访问；
事务、AOP、校验、错误码、配置和观测都是为了让微服务在复杂调用中保持稳定。

## 深度增强：Java 17 分层示例

```java
record CreateOrderCommand(long userId, long skuId, int quantity) {
}

record CreateOrderResult(long orderId, String status) {
}

interface OrderApplicationService {
    CreateOrderResult create(CreateOrderCommand command);
}

final class OrderControllerAdapter {
    private final OrderApplicationService service;

    OrderControllerAdapter(OrderApplicationService service) {
        this.service = service;
    }

    CreateOrderResult submit(CreateOrderCommand command) {
        return service.create(command);
    }
}
```

这个示例表达分层边界：接口层不堆业务逻辑，业务层不依赖 Web 协议，命令和结果对象形成稳定契约。

## 深度增强：生产边界

框架默认值不能替代设计。事务传播、异常回滚、异步线程池、连接池、序列化、超时和重试都要显式治理。
尤其在订单、支付、库存链路中，要避免长事务、隐式重试和跨服务事务误用。

## 深度增强：面试高分表达

我会先解释框架原理，再说明在电商系统里怎么落地。高分回答要能把自动配置、AOP、事务、MVC、WebFlux、
校验和错误处理，连接到可维护性、可观测性、稳定性和故障恢复。

## 专家级完整回答

```text
Spring 通过 @Transactional(isolation = Isolation.xxx) 配置隔离级别，也可以使用数据库默认。
隔离级别影响脏读、不可重复读和幻读的处理，但最终语义取决于数据库实现。隔离越高，并发成本通常越大。

生产中我不会简单把隔离级别调到最高，而会用唯一键、乐观锁、条件更新和状态机解决具体业务一致性问题。
```

## 回答评分点

高分答案应该覆盖：

- `@Transactional(isolation = ...)`。
- 常见隔离级别。
- 脏读、不可重复读、幻读。
- 数据库实现差异。
- 不盲目使用最高隔离级别。
