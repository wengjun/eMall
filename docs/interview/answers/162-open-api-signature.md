# 162 如何设计开放 API 签名？

[返回按分类学习面试题](../README.md)

## 题目

如何设计开放 API 签名？

## 先给面试官的短答案

开放 API 签名通常使用 appId 标识调用方，使用 appSecret 或私钥生成签名。签名内容应包含 HTTP 方法、
路径、查询参数、请求体摘要、时间戳、nonce 和版本，并按固定规则 canonicalize 后计算 HMAC 或非对称签名。
服务端验签、校验时间窗口和 nonce，防止篡改和重放。

签名规则必须稳定、明确、可测试。

## 签名要解决什么？

主要解决：

- 调用方身份识别。
- 请求未被篡改。
- 防止重放攻击。
- 访问权限控制。
- 审计追踪。

签名不是加密，请求内容如果敏感还需要 HTTPS 和字段级保护。

## 参与签名的字段

通常包括：

- appId。
- timestamp。
- nonce。
- HTTP method。
- path。
- query parameters。
- body hash。
- signature version。

请求体不一定直接参与签名，可以用 body hash。

## canonicalization

签名前要规范化：

- 参数按字典序排序。
- URL 编码规则一致。
- 空值处理一致。
- JSON 序列化规则明确。
- 大小写规则明确。

很多签名失败来自规范化不一致。

## 签名算法

常见：

- HMAC-SHA256。
- RSA-SHA256。
- ECDSA。

内部合作方可用 HMAC。

开放平台更适合非对称签名，私钥由调用方保存，平台保存公钥。

## 在 eMall 项目中怎么讲？

商家开放 API 请求带：

```text
X-App-Id
X-Timestamp
X-Nonce
X-Signature
X-Signature-Version
```

服务端校验签名、时间窗口、nonce 去重和接口权限，再处理业务。

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
开放 API 签名要把身份、请求内容和时间信息绑定起来。签名串通常包含 method、path、query、body hash、
timestamp、nonce、appId 和版本，按固定 canonicalization 规则排序编码后，用 HMAC 或非对称算法签名。

服务端验签后还要校验时间窗口、nonce 是否使用过、app 权限和限流。签名规则要稳定，并提供 SDK 和测试用例。
```

## 回答评分点

高分答案应该覆盖：

- appId/appSecret 或公私钥。
- method/path/query/body hash。
- timestamp 和 nonce。
- 参数规范化。
- 验签后还要权限和限流。

## 深度完善：面向 L6 的回答框架

围绕「如何设计开放 API 签名？」，高分答案不能停在概念定义，而要把「REST 契约、幂等、版本、错误码、签名、安全和客户端兼容」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：如何设计开放 API 签名？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
