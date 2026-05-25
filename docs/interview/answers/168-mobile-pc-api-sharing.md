# 168 移动端和 PC 端 API 是否应该完全共用？

[返回按分类学习面试题](../README.md)

## 题目

移动端和 PC 端 API 是否应该完全共用？

## 先给面试官的短答案

不一定。核心业务能力可以复用同一后端服务，但端侧展示 API 不一定完全共用。移动端和 PC 在字段、布局、
网络条件、版本发布节奏和交互流程上不同，适合通过 BFF 或适配层提供差异化 API。

共用的是核心能力，不一定共用同一个接口响应。

## 可以共用什么？

可以共用：

- 用户认证。
- 订单创建。
- 支付提交。
- 库存校验。
- 商品基础服务。
- 优惠计算服务。

这些是核心业务能力，应该保持一致。

## 不一定共用什么？

不一定共用：

- 首页布局。
- 商品详情展示字段。
- 推荐模块。
- 图片规格。
- 营销标签展示。
- 页面聚合结构。

这些和端侧体验强相关。

## 完全共用的风险

风险：

- 响应字段过大。
- 移动端加载慢。
- PC 需求影响移动端。
- 移动端老版本兼容困难。
- 接口字段越来越混乱。

最终会变成一个超大万能接口。

## 推荐方式

推荐：

- 核心服务统一。
- BFF 按端适配。
- 公共 DTO 谨慎复用。
- 保持端侧契约稳定。
- 字段按需返回。

这样兼顾复用和体验。

## 在 eMall 项目中怎么讲？

订单创建接口移动端和 PC 可以共用，因为业务规则一致。

商品详情页不一定共用，因为移动端需要轻量响应，PC 可能展示更多营销和推荐模块。

可以用 mobile BFF 和 pc BFF 调用相同商品、价格、库存服务。

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
移动端和 PC 不应该简单追求所有 API 完全共用。核心业务能力应复用同一后端服务，保证规则一致；
展示聚合接口可以按端通过 BFF 适配，满足字段、布局、网络和版本差异。

否则一个接口会不断膨胀，既影响移动端性能，又让兼容成本越来越高。
```

## 回答评分点

高分答案应该覆盖：

- 核心能力可复用。
- 展示 API 可分端适配。
- 完全共用可能导致接口膨胀。
- BFF 是常见方案。
- 订单和商品详情可举例。
