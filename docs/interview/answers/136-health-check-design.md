# 136 健康检查应该包含哪些内容？

[返回按分类学习面试题](../README.md)

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
