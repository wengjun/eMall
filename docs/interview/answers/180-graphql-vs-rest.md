# 180 GraphQL 和 REST 如何取舍？

[返回按分类学习面试题](../README.md)

## 题目

GraphQL 和 REST 如何取舍？

## 先给面试官的短答案

REST 适合资源边界清晰、缓存友好、治理成熟的开放接口和内部服务接口。
GraphQL 适合前端需要灵活组合字段、减少多接口聚合、页面形态变化快的场景。

但 GraphQL 不能绕过服务边界和权限治理，否则会变成高风险的远程查询语言。

## REST 的特点

优点：

- 简单直观。
- HTTP 语义清晰。
- 缓存和网关支持成熟。
- 监控和限流容易。
- 契约和版本治理成熟。

不足：

- 页面聚合可能需要多次请求。
- 字段过多或过少的问题常见。
- BFF 层可能需要适配多个端。

## GraphQL 的特点

优点：

- 客户端可以声明需要的字段。
- 减少过度返回。
- 适合复杂页面聚合。
- Schema 可描述类型关系。

风险：

- 查询复杂度难控制。
- 权限校验容易遗漏到字段级。
- N+1 查询问题明显。
- 缓存和限流比 REST 更复杂。
- 对开放平台治理成本更高。

## 生产治理要点

如果使用 GraphQL，需要：

- 限制查询深度。
- 限制查询复杂度。
- 做字段级权限。
- 做 DataLoader 或批量加载。
- 禁止任意高成本查询。
- 记录字段级访问日志。
- 对 Schema 做兼容性治理。

GraphQL 网关不是数据库查询入口。

## 在 eMall 项目中怎么讲？

eMall 的开放平台更适合 REST，因为商家接入需要稳定、可文档化、可限流、可签名的接口。

移动端首页可能适合在 BFF 内部使用 GraphQL 聚合商品、活动、推荐和广告数据，
但底层核心交易服务仍保持清晰的 REST 或 RPC 契约。

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
REST 更适合资源清晰、治理成熟、缓存友好和开放平台场景。GraphQL 更适合前端页面变化快、需要灵活字段组合和聚合的场景。
但 GraphQL 必须治理查询深度、复杂度、字段权限、N+1 和缓存，否则容易成为高成本远程查询入口。

在电商系统中，我会让开放 API 和核心服务优先使用 REST 或 RPC，在 BFF 层按需引入 GraphQL 做页面聚合。
```

## 回答评分点

高分答案应该覆盖：

- REST 简单、稳定、网关和缓存支持成熟。
- GraphQL 灵活，适合页面聚合。
- GraphQL 要治理查询复杂度和字段权限。
- 开放平台通常更适合 REST。
- BFF 层可以按需使用 GraphQL。
