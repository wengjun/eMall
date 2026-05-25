# 145 Spring Cache 的使用边界是什么？

[返回按分类学习面试题](../README.md)

## 题目

Spring Cache 的使用边界是什么？

## 先给面试官的短答案

Spring Cache 适合缓存读多写少、允许短暂不一致、key 清晰、失效策略简单的查询结果。
它不适合强一致交易状态、复杂多表写入、库存扣减、支付状态这类对一致性要求很高的核心数据。

缓存不是数据库替代品，必须设计过期、失效、穿透、击穿和雪崩治理。

## 适合场景

适合：

- 商品类目。
- 地区配置。
- 运费模板。
- 活动配置快照。
- 字典数据。
- 读多写少查询。

这些数据变化频率低，短暂不一致可接受。

## 不适合场景

不适合：

- 实时库存。
- 支付状态。
- 账户余额。
- 订单状态强一致查询。
- 高频写数据。
- 复杂权限结果。

这些场景缓存容易造成错误决策。

## key 设计

缓存 key 要：

- 稳定。
- 唯一。
- 包含影响结果的参数。
- 避免过长。
- 有版本或命名空间。

key 设计错会导致脏数据或缓存污染。

## 失效策略

要设计：

- TTL。
- 主动删除。
- 更新后失效。
- 版本号。
- 缓存预热。
- 热点保护。

只加 `@Cacheable` 不设计失效，会埋线上隐患。

## 代理限制

Spring Cache 也基于 AOP。

所以存在：

- 自调用不生效。
- private 方法不生效。
- final 方法可能不生效。

这和事务代理类似。

## 在 eMall 项目中怎么讲？

商品详情基础信息可以缓存。

库存可售数量不能只靠 Spring Cache 决策扣减。

营销规则配置可以缓存，但规则变更要有版本和主动失效机制。

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
Spring Cache 适合读多写少、允许短暂不一致、key 和失效策略清晰的查询结果，比如类目、字典、配置和商品基础信息。
它不适合库存、支付、余额和强一致订单状态这类核心交易数据。

使用时必须设计 key、TTL、主动失效、预热、穿透击穿雪崩治理，并注意它基于 AOP，自调用同样可能不生效。
```

## 回答评分点

高分答案应该覆盖：

- 适合读多写少。
- 不适合强一致交易状态。
- key 和 TTL 很重要。
- 要处理穿透击穿雪崩。
- 基于 AOP 有代理限制。
