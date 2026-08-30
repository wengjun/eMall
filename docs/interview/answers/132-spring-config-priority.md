# 132 Spring 配置加载优先级是什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Spring Boot 配置有一套明确优先级，常见高优先级来源包括命令行参数、环境变量、系统属性、外部配置文件，
低优先级包括 jar 包内的 `application.yml` 和默认值。相同配置项通常高优先级覆盖低优先级。

生产排查配置问题时，要先确认配置来自哪里，以及最终生效值是什么。

## 常见配置来源

常见来源：

- 命令行参数。
- Java system properties。
- 操作系统环境变量。
- 外部 `application.yml`。
- jar 包内 `application.yml`。
- profile 专属配置。
- 配置中心。
- 默认配置。

不同版本 Spring Boot 细节可能略有差异，但覆盖原则一致。

## 为什么优先级重要？

同一个配置可能出现在多个地方。

例如：

```text
server.port=8080
```

如果环境变量、命令行和配置文件都设置了，最终以高优先级为准。

不知道优先级会导致“我明明改了配置但没生效”的问题。

## profile 配置

profile 用于区分环境：

```text
application-dev.yml
application-prod.yml
```

激活方式：

```text
spring.profiles.active=prod
```

生产环境不应依赖开发 profile 默认值。

## 配置中心

配置中心引入后，还要明确：

- 加载时机。
- 本地配置和远程配置谁覆盖谁。
- 是否支持动态刷新。
- 刷新后哪些 Bean 生效。
- 配置变更是否审计。

关键配置不能随意动态修改。

## 如何排查最终值？

可以使用：

- Actuator env endpoint。
- Actuator configprops endpoint。
- 启动日志。
- 配置中心发布记录。
- 容器环境变量。

生产上暴露这些端点要做好权限控制。

## 电商系统实践

订单服务的数据库连接、线程池大小、下游超时、熔断阈值都可能来自不同配置源。

如果线上超时配置不符合预期，要查配置中心、环境变量、profile 和启动参数的最终覆盖关系。
