# 148 如何设计 starter 或 auto-configuration？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

starter 用来封装公共依赖和默认配置，auto-configuration 用条件化 Bean 创建公共能力。
设计时要提供清晰的 properties、合理默认值、条件注解、用户可覆盖 Bean、最小依赖、自动配置元数据和测试。

好的 starter 应该开箱即用，但不绑死业务。

## starter 包含什么？

通常包含：

- 依赖声明。
- 自动配置类。
- properties 配置类。
- 默认 Bean。
- 条件注解。
- 文档和示例。

starter 不应该包含业务强耦合逻辑。

## 条件化配置

常用条件：

- `@ConditionalOnClass`。
- `@ConditionalOnMissingBean`。
- `@ConditionalOnProperty`。
- `@ConditionalOnBean`。

这样只有在依赖和配置满足条件时才创建 Bean。

## 用户可覆盖

公共 starter 不应强行覆盖用户配置。

通常使用：

```java
@ConditionalOnMissingBean
```

用户自定义 Bean 时，默认 Bean 自动退让。

## 配置属性

用 `@ConfigurationProperties` 暴露配置。

要求：

- 命名清晰。
- 默认值安全。
- 有说明文档。
- 支持配置元数据。
- 避免过度动态化。

## 测试

starter 要测试：

- 条件满足时 Bean 创建。
- 条件不满足时不创建。
- 用户 Bean 能覆盖默认 Bean。
- properties 能正确绑定。
- 多模块使用不冲突。

## 在 eMall 项目中怎么讲？

eMall 可以把统一 HTTP client、trace 透传、错误码、限流、熔断、审计日志封装成 starter。

业务模块引入 starter 后获得默认能力，但仍可覆盖超时、线程池和降级策略。
