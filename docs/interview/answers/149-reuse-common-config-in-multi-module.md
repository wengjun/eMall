# 149 如何在多模块项目中复用公共配置？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

多模块项目复用公共配置要区分构建配置、代码配置和运行配置。构建配置放父 POM 的 dependencyManagement 和 pluginManagement；
代码公共能力放 common 或 starter；运行配置通过 profile、配置中心和环境变量管理。不要让业务模块互相复制配置。

公共配置要可覆盖、可测试、版本可控。

## 构建配置

父 POM 管理：

- Java 版本。
- 依赖版本。
- Maven 插件版本。
- Checkstyle。
- Surefire/Failsafe。
- 编译参数。

子模块只声明需要什么，不重复声明版本。

## 代码公共配置

公共代码可以放：

- common。
- shared library。
- Spring Boot starter。
- auto-configuration。

适合封装：

- 统一异常。
- 统一响应。
- HTTP client。
- trace 透传。
- 安全配置。
- MyBatis Plus 配置。

## 运行配置

运行配置不应简单复制到每个模块。

应通过：

- `application.yml` 基线。
- profile。
- 配置中心。
- 环境变量。
- Kubernetes ConfigMap/Secret。

不同环境和服务可以覆盖自己的值。

## 避免过度公共化

不是所有配置都应该公共。

公共配置适合稳定横切能力。

业务特定配置应留在业务模块。

过度抽取会导致公共模块变成“大杂烩”。

## 版本治理

公共配置升级要注意：

- 语义化版本。
- 向后兼容。
- 变更说明。
- 灰度升级。
- 回滚路径。

公共库影响多个服务，不能随意破坏。

## 电商系统实践

大型电商系统的父 POM 管理依赖和插件版本。

common 提供错误码、响应体、审计、基础异常。

统一 HTTP client 和 trace 透传可以做成 starter，由订单、库存、支付等模块引入。
