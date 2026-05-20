# 135 Actuator 暴露哪些端点比较合理？

[返回按分类学习面试题](../README.md)

## 题目

Actuator 暴露哪些端点比较合理？

## 先给面试官的短答案

生产环境 Actuator 应最小化暴露。通常可以暴露 `health`、`info`、`prometheus` 或 metrics 采集端点；
`env`、`beans`、`configprops`、`heapdump`、`threaddump` 等敏感端点不应公网暴露，只能在内网、鉴权、
审计和临时授权下使用。

Actuator 是运维能力，也可能是安全风险。

## 常见安全端点

相对常见：

- `health`。
- `info`。
- `prometheus`。
- `metrics`。

即使是这些端点，也应限制访问来源。

## 敏感端点

敏感端点包括：

- `env`。
- `configprops`。
- `beans`。
- `heapdump`。
- `threaddump`。
- `loggers`。
- `shutdown`。

这些可能泄漏配置、密钥、内部类结构、线程栈和内存数据。

生产不能随便开放。

## health 也要分层

健康检查要区分：

- liveness。
- readiness。
- startup。

不要把所有下游都放进 liveness，否则下游短暂故障可能导致应用被 Kubernetes 重启。

## prometheus 端点

`prometheus` 端点通常给监控系统抓取。

应通过：

- 内网访问。
- ServiceMonitor。
- 鉴权。
- 网络策略。

不要直接公网暴露。

## 临时诊断端点

`heapdump`、`threaddump`、`env` 等可以用于排查。

但要满足：

- 内网。
- 鉴权。
- 审计。
- 临时开启。
- 脱敏。

排查结束后关闭。

## 在 eMall 项目中怎么讲？

eMall 生产服务默认只暴露健康检查和 Prometheus 指标。

配置、线程 dump、heap dump 通过运维平台临时授权获取。

支付和用户服务尤其要避免泄漏密钥、token 和用户数据。

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
生产 Actuator 要最小暴露。常规只开放 health、info 和 prometheus/metrics 给受控内网监控。
env、configprops、beans、heapdump、threaddump、loggers、shutdown 都属于敏感端点，不能公网暴露。

诊断端点应通过运维平台临时开启，并有鉴权、审计和脱敏。Actuator 既是可观测能力，也是攻击面。
```

## 回答评分点

高分答案应该覆盖：

- 最小化暴露。
- health/info/prometheus 相对常见。
- env/heapdump/threaddump 敏感。
- 内网、鉴权、审计。
- liveness/readiness 要区分。

## 深度完善：面向 L6 的回答框架

围绕「Actuator 暴露哪些端点比较合理？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「Spring Boot 和服务工程」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `各服务的 Controller、ApplicationService、MyBatis Plus Mapper、Actuator 和 RestClient` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：事务失败率、健康检查状态、依赖调用耗时、配置变更次数、启动耗时。
- 验证证据：Spring Boot 测试、集成测试、配置审计、Actuator 指标和链路 Trace。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引
本题复习重点：Actuator 暴露哪些端点比较合理？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
