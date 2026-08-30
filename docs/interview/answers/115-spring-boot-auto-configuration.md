# 115 Spring Boot 自动配置原理是什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Spring Boot 自动配置的核心是根据 classpath、配置属性和已有 Bean 条件，自动创建合适的 Bean。
它通过 `@EnableAutoConfiguration` 导入自动配置类，并使用大量 `@ConditionalOnClass`、
`@ConditionalOnMissingBean`、`@ConditionalOnProperty` 等条件注解控制是否生效。

自动配置不是魔法，本质是条件化的 Spring 配置。

## 自动配置解决什么问题？

传统 Spring 项目需要手动配置大量 Bean。

例如：

- Web MVC。
- Jackson。
- DataSource。
- TransactionManager。
- RedisTemplate。
- Actuator。

Spring Boot 根据依赖和配置自动装配默认 Bean，让项目快速启动。

## 入口注解

常见入口：

```java
@SpringBootApplication
public class Application {
}
```

其中包含 `@EnableAutoConfiguration`。

它会导入 Spring Boot 提供的自动配置类。

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

## 电商系统实践

大型电商系统使用 Spring Boot 启动多个微服务。

数据源、Web、Validation、Actuator、HTTP client、MyBatis Plus 等都可以通过自动配置减少样板代码。

但核心交易组件要明确覆盖默认配置，例如线程池、连接池、超时和序列化策略。
