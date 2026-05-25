# 154 API 版本如何设计？

[返回按分类学习面试题](../README.md)

## 题目

API 版本如何设计？

## 先给面试官的短答案

API 版本设计要保证客户端兼容和服务端可演进。常见方式包括 URL 版本、Header 版本和媒体类型版本。
工程上最常用的是 URL 版本，例如 `/api/v1/orders`，简单直观；内部服务也可以用 Header 或契约版本管理。

版本不是每次改字段都要加，只有破坏兼容时才需要新版本。

## URL 版本

示例：

```text
/api/v1/orders
/api/v2/orders
```

优点：

- 直观。
- 易路由。
- 易文档化。
- 前端和网关容易识别。

缺点是 URL 带版本，资源路径不够纯粹。

## Header 版本

示例：

```text
X-API-Version: 1
```

优点：

- URL 更干净。
- 适合内部服务或高级客户端。

缺点：

- 调试不如 URL 直观。
- 网关和文档要额外支持。

## 什么时候不用新版本？

兼容变更通常不需要新版本：

- 响应增加可选字段。
- 新增可选请求参数。
- 新增枚举但客户端能兼容。
- 修复 bug 且不改变契约。

但要保证老客户端不受影响。

## 版本治理

要定义：

- 支持周期。
- 废弃策略。
- 下线通知。
- 兼容测试。
- 文档版本。
- SDK 版本。

没有治理的版本会越积越多。

## 在 eMall 项目中怎么讲？

对外开放平台可以使用 `/openapi/v1` 和 `/openapi/v2`。

内部订单服务接口如果只是新增响应字段，不需要新版本。

如果订单金额字段单位从元改成分，这是破坏兼容，必须新版本或新字段迁移。

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
API 版本用于兼容客户端和支持服务演进。常见方式有 URL、Header 和媒体类型版本，工程上 URL 版本最直观。
不是所有变更都需要新版本，新增可选字段通常兼容；删除字段、修改语义、改变单位、改变错误码语义才是破坏性变更。

版本要有生命周期、废弃公告、兼容测试和下线计划，否则会形成长期维护负担。
```

## 回答评分点

高分答案应该覆盖：

- URL/Header 等版本方式。
- URL 版本最直观。
- 兼容变更不一定新版本。
- 破坏性变更需要新版本。
- 要有版本生命周期治理。
