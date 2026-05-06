# 126 事务隔离级别如何配置？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

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

![Spring 微服务调用栈和治理边界](../../assets/spring-service-stack.svg)

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

## 深度完善：面向 L6 的回答框架

围绕「事务隔离级别如何配置？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「Spring Boot 和服务工程」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `各服务的 Controller、ApplicationService、MyBatis Plus Mapper、Actuator 和 RestClient` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：事务失败率、健康检查状态、依赖调用耗时、配置变更次数、启动耗时。
- 验证证据：Spring Boot 测试、集成测试、配置审计、Actuator 指标和链路 Trace。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：事务隔离级别如何配置？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

