# 179 如何设计错误信息，既便于排障又不泄露内部实现？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

如何设计错误信息，既便于排障又不泄露内部实现？

## 先给面试官的短答案

对外错误信息要稳定、可理解、可追踪，但不能暴露 SQL、堆栈、服务器路径、内部服务名和安全细节。
正确做法是返回标准错误码、用户可读信息、请求追踪 ID 和是否可重试，详细异常只写入受控日志和链路追踪系统。

错误信息要服务两类人：调用方能处理，内部工程师能定位。

## 对外响应包含什么？

建议包含：

- `errorCode`。
- `message`。
- `traceId`。
- `retryable`。
- `details`。

示例：

```json
{
    "errorCode": "ORDER_NOT_FOUND",
    "message": "Order does not exist.",
    "traceId": "01HX8Z7P9Q",
    "retryable": false
}
```

`details` 只放安全的字段级校验信息。

## 不能暴露什么？

不能暴露：

- SQL 语句。
- 数据库表名和字段名。
- Java 堆栈。
- 服务器本地路径。
- 内部 IP。
- 内部服务拓扑。
- 密钥、令牌和签名材料。
- 风控命中规则。

这些信息会帮助攻击者理解系统内部结构。

## 内部排障怎么做？

内部要记录：

- 完整异常栈。
- 请求参数脱敏摘要。
- 用户或商家标识。
- `traceId`。
- 下游调用结果。
- 错误码映射过程。
- 关键业务状态。

日志要脱敏，并按权限访问。

## 在 eMall 项目中怎么讲？

用户下单失败时，不能返回“inventory_db lock wait timeout”。

对外可以返回 `ORDER_SUBMIT_BUSY` 和 `traceId`，提示稍后重试。
内部日志记录库存服务超时、SKU、仓库、traceId 和下游响应，工程师用 traceId 追踪完整链路。

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
错误信息要分为对外响应和内部诊断。对外返回稳定错误码、可理解 message、traceId 和 retryable，
避免暴露 SQL、堆栈、路径、内部 IP、服务名和安全策略。内部日志和 tracing 记录完整上下文，但要脱敏和权限控制。

这样调用方可以按错误码处理，用户能理解结果，工程师也能通过 traceId 定位问题。
```

## 回答评分点

高分答案应该覆盖：

- 对外错误信息要稳定且安全。
- 不暴露 SQL、堆栈、路径和内部拓扑。
- 返回 `traceId` 支持排障。
- 内部日志保留完整上下文但要脱敏。
- `retryable` 能指导调用方行为。

## 深度完善：面向 L6 的回答框架

围绕「如何设计错误信息，既便于排障又不泄露内部实现？」，高分答案不能停在概念定义，而要把「REST 契约、幂等、版本、错误码、签名、安全和客户端兼容」讲成一条可验证的工程链路。
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

本题复习重点：如何设计错误信息，既便于排障又不泄露内部实现？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
