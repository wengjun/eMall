# 160 API 如何保证向后兼容？

[返回按分类学习面试题](../README.md)

## 题目

API 如何保证向后兼容？

## 先给面试官的短答案

API 向后兼容的核心是老客户端不改代码仍能正常工作。做法包括只新增可选字段、不删除或改语义、
字段类型和单位保持稳定、枚举可扩展、错误码稳定、版本化破坏性变更、灰度发布、契约测试和废弃周期。

兼容性是 API 治理能力，不是文档写完就结束。

## 兼容变更

通常兼容：

- 新增可选响应字段。
- 新增可选请求参数。
- 新增接口。
- 扩展不影响老客户端的能力。

前提是客户端能忽略未知字段。

## 不兼容变更

不兼容：

- 删除字段。
- 改字段类型。
- 改字段单位。
- 改字段含义。
- 改必填规则。
- 改错误码语义。
- 改默认排序。
- 改分页语义。

这些需要新版本或迁移方案。

## 枚举兼容

枚举扩展要谨慎。

客户端应该有 unknown 兜底。

服务端不要让新增枚举直接导致老客户端崩溃。

## 契约测试

可以使用契约测试确保：

- 响应结构兼容。
- 必填字段不缺失。
- 错误码不乱改。
- 老客户端场景仍通过。

内部服务同样需要契约治理。

## 废弃策略

废弃旧字段或旧版本要有：

- deprecation 标记。
- 通知。
- 监控调用量。
- 迁移期限。
- 下线窗口。
- 回滚方案。

不能直接删除。

## 在 eMall 项目中怎么讲？

订单状态新增 `PARTIALLY_SHIPPED` 时，老客户端要能显示未知状态或通用状态。

金额字段不能从元直接改成分。

应新增 `amountCents`，迁移完成后再废弃旧字段。

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
API 向后兼容的标准是老客户端不改代码仍能正确运行。兼容变更通常是新增可选字段和参数；
破坏性变更包括删除字段、改类型、改单位、改语义、改错误码和分页排序语义。

我会通过版本化、灰度、契约测试、调用量监控和废弃周期治理 API 演进，避免一次发布破坏客户端。
```

## 回答评分点

高分答案应该覆盖：

- 兼容标准是老客户端不改仍能用。
- 新增可选字段通常兼容。
- 删除、改类型、改语义不兼容。
- 枚举要有 unknown 兜底。
- 契约测试和废弃周期。
