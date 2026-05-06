# 129 @ControllerAdvice 如何做统一异常处理？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`@ControllerAdvice` 如何做统一异常处理？

## 先给面试官的短答案

`@ControllerAdvice` 可以集中处理 Controller 层抛出的异常，配合 `@ExceptionHandler` 把业务异常、
参数异常和系统异常转换成统一响应体和 HTTP 状态码。它能避免每个 Controller 重复 try-catch。

生产中要区分业务错误和系统错误，不能把堆栈直接返回给前端。

## 基本写法

示例：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusiness(BusinessException ex) {
        return ApiResponse.fail(ex.code(), ex.getMessage());
    }
}
```

`@RestControllerAdvice` 等于 `@ControllerAdvice` 加 `@ResponseBody`。

## 应该处理哪些异常？

常见类型：

- 业务异常。
- 参数校验异常。
- 类型转换异常。
- 认证授权异常。
- 限流异常。
- 下游超时异常。
- 未知系统异常。

不同异常要映射不同错误码和 HTTP 状态。

## 参数校验异常

Bean Validation 失败时，常见异常：

- `MethodArgumentNotValidException`。
- `ConstraintViolationException`。

应该返回清晰字段错误，但不要泄漏内部类名和堆栈。

## 系统异常

未知异常要：

- 记录完整日志。
- 返回通用错误码。
- 带 trace ID。
- 不返回堆栈。
- 触发告警或指标。

前端只需要知道请求失败和错误码，排查用 trace ID 关联日志。

## 错误响应体

统一响应通常包含：

- code。
- message。
- traceId。
- timestamp。
- details。

`details` 要谨慎，避免泄漏敏感信息。

## 在 eMall 项目中怎么讲？

订单接口中，库存不足是业务异常，可以返回明确业务错误码。

数据库连接失败是系统异常，应记录堆栈并返回系统繁忙。

支付下游超时可返回可重试错误，并带 trace ID 方便排查。

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
@ControllerAdvice 配合 @ExceptionHandler 可以集中把 Controller 层异常转换成统一错误响应。
我会区分业务异常、参数异常、认证授权异常、下游异常和未知系统异常，分别映射错误码和 HTTP 状态。

未知异常只在服务端记录堆栈，客户端返回通用错误和 traceId，避免泄漏内部实现和敏感信息。
```

## 回答评分点

高分答案应该覆盖：

- `@ControllerAdvice` 和 `@ExceptionHandler`。
- 业务异常和系统异常区分。
- 参数校验异常处理。
- 统一错误码和 trace ID。
- 不返回堆栈给前端。

## 深度完善：面向 L6 的回答框架

围绕「@ControllerAdvice 如何做统一异常处理？」，高分答案不能停在概念定义，而要把「Bean 生命周期、AOP、事务、配置、HTTP 客户端、健康检查和公共配置」讲成一条可验证的工程链路。
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

本题复习重点：`@ControllerAdvice` 如何做统一异常处理？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

