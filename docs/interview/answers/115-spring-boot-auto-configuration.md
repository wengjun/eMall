# 115 Spring Boot 自动配置原理是什么？

[返回按分类学习面试题](../README.md)

## 核心结论

Spring Boot 自动配置的核心是根据 classpath、配置属性和已有 Bean 条件，自动创建合适的 Bean。
它通过 `@EnableAutoConfiguration` 导入自动配置类，并使用大量 `@ConditionalOnClass`、
`@ConditionalOnMissingBean`、`@ConditionalOnProperty` 等条件注解控制是否生效。

自动配置不是魔法，本质是条件化的 Spring 配置。

## 条件注解

常见条件：

- `@ConditionalOnClass`：classpath 存在某个类。
- `@ConditionalOnMissingBean`：容器中没有某个 Bean。
- `@ConditionalOnBean`：容器中已有某个 Bean。
- `@ConditionalOnProperty`：配置项满足条件。
- `@ConditionalOnWebApplication`：当前是 Web 应用。

这些条件决定自动配置是否生效。

## 用户配置优先

自动配置通常使用 `@ConditionalOnMissingBean`。

含义是：如果用户自己定义了 Bean，Spring Boot 就不再创建默认 Bean。

这让默认配置可覆盖。

## 配置属性

自动配置通常结合 `@ConfigurationProperties`。

例如：

```text
server.port=8080
spring.datasource.url=...
```

配置属性绑定到配置类，再用于创建 Bean。

## 排查自动配置

可以用：

- Actuator conditions endpoint。
- 启动参数 `--debug`。
- 查看自动配置报告。
- 查看 Bean 定义。

当自动配置不符合预期时，不要猜，要看条件是否匹配。
