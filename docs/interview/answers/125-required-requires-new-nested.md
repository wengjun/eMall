# 125 REQUIRED、REQUIRES_NEW、NESTED 有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

`REQUIRED`、`REQUIRES_NEW`、`NESTED` 有什么区别？

## 先给面试官的短答案

`REQUIRED` 是默认行为，有事务就加入，没有就创建；`REQUIRES_NEW` 总是创建独立新事务，并挂起外部事务；
`NESTED` 在外部事务中创建保存点，内部回滚可回到保存点。`REQUIRES_NEW` 是两个物理事务，
`NESTED` 通常是同一个物理事务中的保存点。

## REQUIRED

外部有事务：

```text
outer transaction
  inner REQUIRED joins outer
```

内部异常如果未处理，通常会导致整个事务回滚。

适合主业务流程。

## REQUIRES_NEW

外部有事务时：

```text
outer transaction suspended
  inner new transaction commits or rolls back
outer transaction resumes
```

内外事务独立提交或回滚。

外部回滚不一定影响内部已提交事务。

风险是需要额外数据库连接。

## NESTED

外部有事务时：

```text
outer transaction
  savepoint
  inner nested
  rollback to savepoint if inner fails
outer continues
```

它依赖数据库保存点。

外部事务最终回滚时，嵌套事务结果也会一起回滚。

## 关键区别

| 行为 | 事务关系 | 外部回滚影响内部 | 内部失败影响外部 |
| --- | --- | --- | --- |
| REQUIRED | 同一事务 | 影响 | 通常影响 |
| REQUIRES_NEW | 独立事务 | 不影响已提交内部事务 | 可捕获后不影响 |
| NESTED | 同一事务保存点 | 影响 | 可回滚到保存点 |

## 使用建议

建议：

- 主流程默认 `REQUIRED`。
- 独立审计或日志可用 `REQUIRES_NEW`。
- 需要部分回滚且数据库支持保存点时考虑 `NESTED`。
- 不要用传播行为修补糟糕的业务边界。

## 在 eMall 项目中怎么讲？

订单创建和订单明细保存通常在同一个 `REQUIRED` 事务中。

审计日志可以用 `REQUIRES_NEW`，确保主事务失败时仍记录失败原因。

如果批量处理多个子项，允许单个子项失败回滚到保存点，可以考虑 `NESTED`，但要验证数据库支持。

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
REQUIRED 是默认传播，有事务加入、无事务创建；REQUIRES_NEW 会挂起外部事务并创建独立物理事务，
内外提交回滚相互独立；NESTED 通常基于保存点，是同一个物理事务中的嵌套回滚。

工程上默认用 REQUIRED，独立审计可用 REQUIRES_NEW，但要注意连接池；NESTED 要确认事务管理器和数据库支持 savepoint。
```

## 回答评分点

高分答案应该覆盖：

- `REQUIRED` 加入外部事务。
- `REQUIRES_NEW` 独立事务并挂起外部事务。
- `NESTED` 是保存点。
- 外部回滚对三者影响不同。
- 连接池和数据库支持要考虑。
