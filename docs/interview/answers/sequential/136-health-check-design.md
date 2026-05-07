# 136 健康检查应该包含哪些内容？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

健康检查应该包含哪些内容？

## 先给面试官的短答案

健康检查要区分 liveness、readiness 和 startup。liveness 判断进程是否还活着，不能依赖过多下游；
readiness 判断实例是否可以接流量，应检查关键依赖和本地初始化；startup 判断慢启动应用是否完成启动。

健康检查设计错误会导致误重启、流量打到不可用实例或故障扩散。

## liveness

liveness 用于判断进程是否需要重启。

应该检查：

- JVM 进程可响应。
- 主线程或 Web 容器未完全卡死。
- 应用基础状态正常。

不应该强依赖数据库、Redis 和下游服务。

否则下游故障会导致所有上游被重启。

## readiness

readiness 用于判断是否可以接流量。

可以检查：

- Spring context 已启动。
- 必要配置加载完成。
- 数据库连接可用。
- 核心缓存已准备。
- 注册发现状态正常。
- 必要下游可达。

readiness 失败时，平台应停止给该实例分流，但不一定重启。

## startup

startup probe 用于慢启动应用。

它给应用更长启动时间，避免 liveness 过早杀死正在启动的实例。

适合：

- 初始化较慢。
- 类加载多。
- 缓存预热。
- 大型 Spring 应用。

## 依赖检查边界

不要把所有依赖都放进健康检查。

应区分：

- 强依赖：没有它不能接流量。
- 弱依赖：可以降级。
- 非核心依赖：不影响 readiness。

推荐服务挂了，不应让订单服务 readiness 失败。

## 在 eMall 项目中怎么讲？

订单服务 readiness 应检查订单库、必要配置和核心消息组件。

推荐、广告、实验平台不应作为订单服务强健康依赖。

支付服务如果核心支付渠道全不可用，readiness 可以失败；单个非核心渠道失败则应降级。

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
健康检查要分 liveness、readiness 和 startup。liveness 判断进程是否需要重启，不能强依赖下游；
readiness 判断是否能接流量，可以检查关键依赖和初始化；startup 用于慢启动保护。

设计时要区分强依赖和弱依赖。弱依赖故障应该降级，而不是让核心服务被摘流或重启。
```

## 回答评分点

高分答案应该覆盖：

- 区分 liveness/readiness/startup。
- liveness 不应依赖太多下游。
- readiness 检查接流量能力。
- startup 防止慢启动误杀。
- 强弱依赖要区分。

## 深度完善：面向 L6 的回答框架

围绕「健康检查应该包含哪些内容？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：健康检查应该包含哪些内容？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
