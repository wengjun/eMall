# 130 Bean Validation 适合做哪些校验？

[返回按分类学习面试题](../README.md)

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

## 电商系统实践

创建订单请求中：

- userId 不能为空。
- skuId 不能为空。
- quantity 必须大于 0。
- orderLines 至少一项。

这些适合 Bean Validation。

库存是否足够、价格是否变化、优惠是否可用，应由订单应用服务校验。
