# 130 Bean Validation 适合做哪些校验？

[返回按分类学习面试题](../README.md)

## 题目

Bean Validation 适合做哪些校验？

## 先给面试官的短答案

Bean Validation 适合做输入参数的结构性和格式性校验，例如必填、长度、范围、枚举、邮箱格式、集合大小和嵌套对象校验。
它不适合承载复杂业务规则，例如库存是否足够、优惠券是否可用、订单状态是否允许取消。

参数校验挡住非法输入，业务校验保证业务规则正确。

## 常见注解

常见注解：

- `@NotNull`。
- `@NotBlank`。
- `@Size`。
- `@Min`。
- `@Max`。
- `@Email`。
- `@Pattern`。
- `@Valid`。

它们适合表达字段级约束。

## Controller 中使用

示例：

```java
public ApiResponse<?> create(@Valid @RequestBody CreateOrderRequest request) {
    orderService.create(request);
    return ApiResponse.success();
}
```

校验失败后由统一异常处理返回错误响应。

## 嵌套校验

如果对象中包含子对象或集合，要使用 `@Valid` 触发嵌套校验。

```java
public record CreateOrderRequest(
        @NotNull Long userId,
        @Valid @Size(min = 1) List<OrderLineRequest> lines) {
}
```

## 分组校验

不同场景校验规则不同，可以使用 validation groups。

例如创建订单和更新订单，对字段必填要求不同。

但分组过多会让规则难维护，要谨慎使用。

## 不适合做什么？

不适合：

- 查数据库判断库存。
- 判断优惠券是否过期。
- 判断用户是否有权限。
- 判断订单状态流转是否合法。
- 调用远程服务校验。

这些属于业务校验，应放在 Service 或领域层。

## 自定义校验

可以自定义注解和 Validator。

适合通用格式校验，例如手机号格式、业务编码格式。

不建议在 Validator 中做复杂数据库查询或远程调用，否则会让参数校验变慢且难治理。

## 在 eMall 项目中怎么讲？

创建订单请求中：

- userId 不能为空。
- skuId 不能为空。
- quantity 必须大于 0。
- orderLines 至少一项。

这些适合 Bean Validation。

库存是否足够、价格是否变化、优惠是否可用，应由订单应用服务校验。

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
Bean Validation 适合做请求入参的结构性和格式性校验，比如必填、长度、范围、格式、集合大小和嵌套对象。
它应该在 Controller 边界挡住明显非法输入。复杂业务规则不应放在 Bean Validation 中，尤其不要在校验器里
做远程调用或复杂数据库查询。

我的原则是：参数校验解决输入合法性，业务校验解决业务状态和规则正确性。
```

## 回答评分点

高分答案应该覆盖：

- 适合结构和格式校验。
- `@Valid` 嵌套校验。
- 分组校验要谨慎。
- 不适合复杂业务规则。
- 参数校验和业务校验要分层。
