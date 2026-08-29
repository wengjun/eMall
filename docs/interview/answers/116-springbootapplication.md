# 116 @SpringBootApplication 包含哪些注解？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`@SpringBootApplication` 是组合注解，核心包含 `@SpringBootConfiguration`、`@EnableAutoConfiguration`
和 `@ComponentScan`。它表示当前类是 Spring Boot 配置类，启用自动配置，并从当前包开始扫描组件。

理解它有助于排查 Bean 扫描不到、自动配置不生效和包结构不合理问题。

## 三个核心注解

核心包括：

- `@SpringBootConfiguration`。
- `@EnableAutoConfiguration`。
- `@ComponentScan`。

它们共同完成启动配置、自动装配和组件扫描。

## SpringBootConfiguration

`@SpringBootConfiguration` 本质上是特殊的 `@Configuration`。

它说明当前类是配置类，可以定义 Bean。

一个应用通常只有一个主启动配置类。

## EnableAutoConfiguration

`@EnableAutoConfiguration` 启用 Spring Boot 自动配置。

它会根据 classpath、配置属性和已有 Bean 自动创建默认 Bean。

例如引入 Web 依赖后自动配置 MVC、Tomcat、Jackson 等。

## ComponentScan

`@ComponentScan` 从当前启动类所在包开始扫描组件。

会扫描：

- `@Component`。
- `@Service`。
- `@Repository`。
- `@Controller`。
- `@RestController`。
- `@Configuration`。

如果启动类包位置太深，其他包下 Bean 可能扫描不到。

## 包结构建议

启动类应放在业务根包。

例如：

```text
com.emall.order.OrderApplication
com.emall.order.application
com.emall.order.domain
com.emall.order.infrastructure
```

这样默认扫描能覆盖模块内部组件。

## 常见问题

问题：

- Bean 扫描不到。
- Mapper 未扫描。
- 自动配置被排除。
- 多个启动类包结构混乱。
- 测试启动上下文不完整。

排查时先看启动类位置和 scan base packages。

## 在 eMall 项目中怎么讲？

每个 eMall 微服务模块的启动类应位于模块根包，例如 `com.emall.payment`。

公共组件放在 `common` 时，要通过 starter、显式扫描或自动配置方式引入，不能依赖随意扩大扫描范围。

否则模块边界会变混乱。
