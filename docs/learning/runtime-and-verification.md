# 运行配置与验证

[代码导读首页](README.md) | [上一篇：持久化与消息](persistence-and-messaging.md)

本文连接代码、配置和测试入口，不重复解释 Docker、Kubernetes、HTTP 或测试分类的基础概念。

## Spring 如何装配当前实现

从 [OrderApplication](../../order/src/main/java/com/emall/order/OrderApplication.java) 和
[OrderConfig](../../order/src/main/java/com/emall/order/config/OrderConfig.java) 看业务 Bean 与客户端装配，
再查看 `common` 的
[自动配置清单](../../common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)。

`common` 是被业务模块依赖的库，不是独立部署的服务。它通过自动配置和显式 Bean 提供幂等、持久化、
出站请求、安全和运行保护等基础设施。领域规则仍在各业务模块，具体模块性质以[模块清单](../modules.md)为准。

阅读 `@ConditionalOnProperty` 时同时核对配置来源，不能看到一个实现类就认定运行时一定使用它。
例如存储模式决定内存仓储或 MyBatis-Plus 仓储的选择；测试构造器提供的 `noop`、`direct` 或内存实现不等于生产配置。

| 问题 | 当前事实源 |
| --- | --- |
| 单个服务的默认值与属性绑定 | [order/application.yml](../../order/src/main/resources/application.yml)及对应 Properties 类 |
| 本机 Spring Boot 调试 | [项目首页](../../README.md)和[环境变量示例](../../.env.example) |
| Compose 容器内地址 | [docker-compose.yml](../../docker-compose.yml)和[本地覆盖值](../../ops/env/local.env) |
| Nacos、Dubbo 等技术栈接入 | [国内技术栈说明](../domestic-stack.md) |
| 生产部署值与入口资源 | [生产部署 Chart](../../ops/helm/emall/README.md) |

`localhost` 指向当前进程所在主机或容器，不能把本机连接地址原样搬进 Compose 容器。
生产保护检查见 [ProductionRuntimeGuard](../../common/src/main/java/com/emall/common/runtime/ProductionRuntimeGuard.java)，
密钥、证书和网络约束见[安全加固](../security-hardening.md)，不要靠关闭保护器解决配置缺失。

## 入口身份与线程模型

[GatewayAuthenticationFilter](../../gateway/src/main/java/com/emall/gateway/filter/GatewayAuthenticationFilter.java)
是 Spring Cloud Gateway 的响应式过滤器：清除不可信身份头，验证 Bearer 令牌，使用响应式 Redis 查询撤销状态，
再执行路由级授权。不要在这个 `Mono` 链上直接添加阻塞数据库访问。

业务服务还有
[ApiAuthenticationFilter](../../common/src/main/java/com/emall/common/security/ApiAuthenticationFilter.java)
及控制器、Service 的授权检查，不能将“经过网关”视为唯一安全边界。
账户权威状态检查入口之一是
[IdentityAccessGuard](../../common/src/main/java/com/emall/common/trust/IdentityAccessGuard.java)。

公网 HTTPS 与 Java 服务内部调用是不同层次。证书终止、Gateway API 和转发配置统一查
[运维配置索引](../../ops/README.md#httpstls-接入)，不在导读中维护另一份端口或部署清单。

## HTTP 与 Dubbo 的选择点

以 [InventoryClient](../../order/src/main/java/com/emall/order/integration/InventoryClient.java) 为例：

- `emall.rpc.protocol` 和已注入的 RPC 引用共同决定是否走 Dubbo 分支，接口为
  [InventoryRpcService](../../common/src/main/java/com/emall/common/rpc/InventoryRpcService.java)。
- HTTP 分支使用 Spring `RestClient`，由
  [OutboundHttpClientFactory](../../common/src/main/java/com/emall/common/web/OutboundHttpClientFactory.java)统一构建。
- `@DubboReference` 设置了超时并关闭自动重试。不要在不了解幂等与恢复链路时，又在上层增加一套无限重试。

这不是两个独立业务实现：请求和响应通过适配转换，订单工作流使用统一的本地结果类型。
协议选择、网络调用成功和资源预占成功也不是同一件事；仍须判断返回值的业务状态。

## Sentinel 与恢复控制

`InventoryClient.reserve`、`confirm`、`release` 使用 `@SentinelResource`。
`blockHandler` 处理 Sentinel 拦截，`fallback` 处理调用异常；它们返回不可用状态，不能伪装成库存已成功预占。
资源名和规则配置见
[OrderSentinelRuleConfiguration](../../order/src/main/java/com/emall/order/config/OrderSentinelRuleConfiguration.java)。

客户端还调用
[AdaptiveRecoveryController](../../governance/src/main/java/com/emall/governance/recovery/AdaptiveRecoveryController.java)
决定是否允许请求，并反馈成功或失败。阅读时对照其状态转换和
[AdaptiveRecoveryPolicy](../../governance/src/main/java/com/emall/governance/recovery/AdaptiveRecoveryPolicy.java)，
不要把 Sentinel 的熔断状态与恢复控制器的状态当作同一个对象。

普通 `new InventoryClient(...)` 的测试可以验证 Java 分支，却不会自动启用 Spring 的 Sentinel AOP 拦截。
涉及限流或熔断注解的验收，需要确认代理、规则加载和真实请求路径均生效。
治理取舍见 [ADR 003](../adr/003-sentinel-and-adaptive-recovery.md)。

## 从行为找到验证证据

不在本目录复制 Maven 和环境变量命令。执行方式统一使用[集成测试说明](../integration-testing.md)，
构建与格式化使用[项目首页](../../README.md)。下面只列与本导读直接相关的证据入口：

| 需要确认的行为 | 测试入口 |
| --- | --- |
| 网关身份头清理与令牌检查 | [GatewayAuthenticationFilterTest](../../gateway/src/test/java/com/emall/gateway/filter/GatewayAuthenticationFilterTest.java) |
| 公共自动配置装配 | [CommonAutoConfigurationIntegrationTest](../../common/src/test/java/com/emall/common/CommonAutoConfigurationIntegrationTest.java) |
| 生产配置拒绝不安全值 | [ProductionRuntimeGuardTest](../../common/src/test/java/com/emall/common/runtime/ProductionRuntimeGuardTest.java) |
| Redis 提交频率限制 | [OrderSubmissionGuardIT](../../order/src/test/java/com/emall/order/service/OrderSubmissionGuardIT.java) |
| 已部署服务的下单支付流程 | [CheckoutEndToEndIT](../../smoke/src/test/java/com/emall/smoke/CheckoutEndToEndIT.java) |
| 已部署服务的补偿恢复 | [CompensationRecoveryIT](../../smoke/src/test/java/com/emall/smoke/CompensationRecoveryIT.java) |

区分“测试存在”“实际执行”“断言通过”和“目标规模通过”。Testcontainers 被跳过或 Smoke 开关未开启时，
构建成功不代表相应链路已经验证。百万并发也不能从单机单元测试或少量并发测试推导出来。

线上观察项及故障处理以[可观测性](../observability.md)和[SLO 与故障手册](../slo-and-runbooks.md)为准。
上线前仍需满足[生产检查清单](../production-checklist.md)中的组件复验、真实环境和容量证据要求。
