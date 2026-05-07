# 163 如何防止重放攻击？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何防止重放攻击？

## 先给面试官的短答案

防重放通常依赖时间戳、nonce、签名和幂等记录。客户端请求带 timestamp 和 nonce，二者参与签名；
服务端校验时间窗口，例如 5 分钟内有效，并用 Redis 或数据库记录 nonce，确保同一 nonce 只能使用一次。

对有副作用接口，还要用业务幂等防止重复执行。

## 重放攻击是什么？

攻击者截获一次合法请求后，原样再次发送。

如果没有防护，系统可能重复执行：

- 下单。
- 扣款。
- 发券。
- 修改地址。

HTTPS 能防窃听，但不能替代应用层重放防护，尤其是日志泄漏或客户端被攻破时。

## timestamp

timestamp 用于限制请求有效期。

服务端校验：

```text
abs(serverTime - clientTime) <= allowedWindow
```

例如允许 5 分钟。

时间窗口过大，重放风险增大；过小，容易受时钟偏差影响。

## nonce

nonce 是一次性随机值。

服务端记录已使用 nonce：

```text
appId + nonce
```

在有效时间窗口内重复出现则拒绝。

Redis `SET NX EX` 很适合记录短期 nonce。

## 签名绑定

timestamp 和 nonce 必须参与签名。

否则攻击者可以改 timestamp 或 nonce 绕过校验。

签名还应绑定 method、path、query 和 body hash，防止内容被篡改。

## 业务幂等兜底

防重放不能替代业务幂等。

例如支付回调、创建订单、发券仍要有：

- 业务流水号。
- 唯一键。
- 状态机。
- 幂等表。

否则 nonce 记录异常时仍可能重复执行。

## 在 eMall 项目中怎么讲？

开放平台商家调用创建售后单接口时，请求必须带 timestamp、nonce 和 signature。

服务端校验签名和 nonce 后，还要用商家请求号建唯一键，防止重复创建售后单。

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
防重放通常用 timestamp + nonce + signature。timestamp 限制请求有效窗口，nonce 保证窗口内只用一次，
二者都必须参与签名。服务端可以用 Redis SET NX EX 记录 appId+nonce。

对有副作用接口，还必须有业务幂等和唯一键。安全防重放解决请求重复提交风险，业务幂等解决重复执行风险。
```

## 回答评分点

高分答案应该覆盖：

- timestamp 限制时间窗口。
- nonce 一次性。
- timestamp/nonce 要参与签名。
- Redis 去重。
- 业务幂等兜底。

## 深度完善：面向 L6 的回答框架

围绕「如何防止重放攻击？」，高分答案不能停在概念定义，而要把「REST 契约、幂等、版本、错误码、签名、安全和客户端兼容」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「API 设计和网关治理」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `gateway、openapi、identity、risk、order 的外部 API 和内部服务 API` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：接口错误率、幂等冲突率、签名失败率、限流命中率、兼容性测试结果。
- 验证证据：OpenAPI 文档、契约测试、审计日志、网关指标、错误码看板和灰度记录。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：如何防止重放攻击？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
