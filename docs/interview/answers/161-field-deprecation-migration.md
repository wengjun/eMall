# 161 如何做字段废弃和迁移？

[返回按分类学习面试题](../README.md)

## 题目

如何做字段废弃和迁移？

## 先给面试官的短答案

字段废弃要走兼容迁移：先新增新字段，旧字段继续保留；服务端双写或双读；客户端灰度迁移到新字段；
监控旧字段使用量；达到下线条件后标记废弃并最终删除。不能直接改字段语义或删除字段。

核心目标是老客户端不受影响，新客户端可平滑迁移。

## 为什么不能直接删除？

客户端可能还在使用旧字段。

直接删除会导致：

- 前端展示异常。
- 移动端老版本崩溃。
- 外部合作方调用失败。
- 数据解释错误。

尤其移动端版本不可控，字段废弃周期要更长。

## 推荐流程

推荐：

- 新增字段。
- 保留旧字段。
- 双写或转换。
- 文档标记 deprecated。
- 客户端灰度使用新字段。
- 监控旧字段使用。
- 通知下线计划。
- 删除旧字段。

每一步都要可回滚。

## 字段语义迁移

例如金额字段从元改为分。

不要直接把：

```text
amount
```

从元改成分。

更好是新增：

```text
amountCents
```

旧字段继续保持原语义，直到客户端迁移完成。

## API 文档标记

文档要标明：

- 废弃字段。
- 替代字段。
- 废弃原因。
- 最早下线时间。
- 迁移示例。

只在代码里加注解不够。

## 在 eMall 项目中怎么讲？

订单金额从 `amountYuan` 迁移到 `amountCents`。

服务端同时返回两个字段，前端和移动端逐步使用 `amountCents`。

监控老版本调用量接近 0 后，再安排下线 `amountYuan`。

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
字段废弃要先新增后迁移，不能直接删除或改语义。服务端保留旧字段并新增新字段，必要时双写双读；
客户端灰度切换；通过日志或埋点确认旧字段使用量；文档标记 deprecated，并给出替代字段和下线时间。

对移动端和开放 API，废弃周期要更长，因为客户端升级不可控。
```

## 回答评分点

高分答案应该覆盖：

- 先新增后废弃。
- 不能直接改字段语义。
- 双写双读和灰度。
- 监控旧字段使用。
- 文档和下线周期。
