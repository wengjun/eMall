# 037 如何设计稳定的公共库 API？

[返回按分类学习面试题](../README.md)

## 题目

如何设计稳定的公共库 API？

## 先给面试官的短答案

公共库 API 一旦被多个模块依赖，变更成本会很高。设计时要保持最小暴露、语义清晰、
输入输出稳定、错误模型明确、向后兼容，并配套文档和测试。

公共库不是为了“大家都能放代码”，而是提供稳定基础能力。

## 设计原则

### 暴露最小能力

只暴露调用方真正需要的接口，不暴露内部实现类。

### 参数对象优于长参数列表

不好：

```java
replay(String service, String status, int limit, String operator, String traceId);
```

更好：

```java
replayOutbox(ReplayOutboxCommand command);
```

### 返回明确结果

不要返回裸 `Map<String, Object>`。

```java
public record OperationResult(boolean success, int affected, String message) {
}
```

### 错误模型稳定

公共 API 应该明确抛什么异常或返回什么错误结果。

### 命名表达业务语义

`execute`、`handle`、`process` 太泛。
`publishOutboxEvents`、`authorizeInternalOperation` 更清楚。

## 公共库要避免什么？

- 放具体业务规则。
- 暴露可变内部对象。
- 让所有模块依赖所有东西。
- 频繁破坏性改接口。
- 使用不稳定枚举作为对外契约但不做兼容。
- 返回 Object 或 Map 逃避建模。

## 在 eMall 项目中怎么讲？

`common` 适合放：

- `ApiResponse`
- `ErrorCode`
- `BusinessException`
- Trace 工具。
- Outbox 基础设施接口。
- 审计记录模型。
- 加密接口。

不适合放：

- 订单支付规则。
- 库存扣减策略。
- 具体促销规则。
- 某个业务模块专属 SQL。

## 专家级完整回答

```text
稳定公共库 API 要最小暴露、语义清晰、输入输出强类型、错误模型稳定，并保证向后兼容。
公共库应该承载真正跨模块的基础能力，例如统一响应、错误码、审计、Outbox 基础设施和加密工具。
具体业务规则不应该进入 common，否则会让公共库变成强耦合中心。
```

## 回答评分点

高分答案应该覆盖：

- 最小暴露。
- 强类型输入输出。
- 稳定错误模型。
- 文档和测试。
- common 不应承载具体业务规则。
