# 167 如何设计 BFF？

[返回按分类学习面试题](../README.md)

## 题目

如何设计 BFF？

## 先给面试官的短答案

BFF 是 Backend For Frontend，用于为特定端侧聚合、裁剪和适配后端能力。它适合处理移动端、PC、开放平台不同展示需求，
聚合多个后端接口，减少端侧多次请求。BFF 不应承载核心业务规则和持久化事务。

BFF 关注体验和编排，核心业务仍在领域服务中。

## BFF 解决什么？

前端页面通常需要多个后端数据。

如果端侧直接调多个服务：

- 请求次数多。
- 编排复杂。
- 端侧逻辑重。
- 版本适配困难。

BFF 把端侧需要的数据聚合成一个接口。

## BFF 应该做什么？

适合：

- 多接口聚合。
- 字段裁剪。
- 端侧格式适配。
- 多端差异处理。
- 简单展示逻辑。
- 缓存非核心展示数据。

例如商品详情 BFF 聚合商品、价格、评价、推荐和活动标签。

## BFF 不应该做什么？

不适合：

- 库存扣减。
- 支付扣款。
- 订单状态机。
- 复杂优惠规则。
- 数据库事务。
- 核心一致性决策。

这些属于后端业务服务。

## BFF 和网关区别

网关是通用入口治理。

BFF 是端侧业务聚合。

网关应保持通用，BFF 可以按端或场景定制。

## 在 eMall 项目中怎么讲？

移动端商品详情 BFF 可以返回更少字段和移动端布局信息。

PC 商品详情 BFF 可以聚合更多推荐和营销模块。

但下单接口仍然调用订单服务，由订单服务做库存、价格、优惠和幂等校验。

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
BFF 面向特定前端或场景，负责聚合、裁剪和适配后端服务，减少端侧复杂度。它适合处理移动端和 PC
展示差异、字段裁剪和多接口聚合，但不应该承载核心交易规则、状态机和数据库事务。

我会把 BFF 定位为体验层和编排层，核心业务规则仍放在订单、商品、库存、支付等领域服务中。
```

## 回答评分点

高分答案应该覆盖：

- BFF 面向端侧。
- 聚合和裁剪数据。
- 不承载核心业务事务。
- 区分 BFF 和网关。
- 能结合商品详情场景。
