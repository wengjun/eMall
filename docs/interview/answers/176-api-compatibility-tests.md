# 176 如何做 API 兼容性测试？

[返回按分类学习面试题](../README.md)

## 题目

如何做 API 兼容性测试？

## 先给面试官的短答案

API 兼容性测试用于防止新版本破坏老客户端。它要检查路径、方法、参数、字段类型、字段可选性、枚举值、错误码和语义。
生产系统应把 OpenAPI diff、契约测试、消费者回放测试和灰度验证放入 CI/CD。

兼容性不是靠口头约定，而是靠自动化测试和发布门禁。

## 什么是破坏性变更？

常见破坏包括：

- 删除接口。
- 修改路径或方法。
- 删除响应字段。
- 修改字段类型。
- 把可选字段改为必填。
- 删除枚举值。
- 改变错误码语义。
- 改变分页或排序语义。
- 响应时间明显变差。

有些变更在代码层能编译通过，但会破坏客户端。

## 测试手段

常见做法：

- OpenAPI 文件做差异检查。
- 消费者驱动契约测试。
- 回放线上真实请求样本。
- 对关键客户端跑端到端测试。
- 灰度阶段比较新旧响应。
- 对废弃字段做监控。

这些测试要成为发布流水线的一部分。

## 兼容性策略

策略包括：

- 新增字段默认兼容。
- 删除字段必须先废弃再下线。
- 枚举只能新增，不能随意删除或改义。
- 必填字段新增要走新版本。
- 错误码语义不能随意复用。
- 响应结构要保持稳定。

接口契约一旦被外部依赖，就要按产品承诺治理。

## 在 eMall 项目中怎么讲？

开放平台的订单查询接口如果删除 `orderStatus` 字段，商家 ERP 可能直接解析失败。

因此 eMall 应在 CI 中比较 OpenAPI 变更，并运行商家侧契约测试。灰度时还要采样比较新旧响应字段。

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
API 兼容性测试要保护老客户端。测试范围包括路径、方法、参数、响应字段、字段类型、枚举值、错误码和语义。
我会在 CI 中做 OpenAPI diff 和消费者契约测试，并在灰度阶段回放真实请求，比较新旧响应。

兼容变更通常是新增可选字段；删除字段、改类型、把可选改必填、改变错误码语义都属于高风险破坏性变更。
```

## 回答评分点

高分答案应该覆盖：

- 兼容性测试保护老客户端。
- OpenAPI diff 和契约测试是核心手段。
- 删除字段、改类型、改必填是破坏性变更。
- 灰度回放能发现语义差异。
- 兼容性要成为发布门禁。
