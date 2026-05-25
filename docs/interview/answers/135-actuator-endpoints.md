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
