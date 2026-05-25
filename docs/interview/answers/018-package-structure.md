# 018 如何设计清晰的包结构？

[返回按分类学习面试题](../README.md)

## 题目

如何设计清晰的包结构？

## 先给面试官的短答案

包结构应该让人一眼看出代码职责和依赖方向。大型后端服务通常按业务模块内部分层：
`api`、`service`、`domain`、`repository`、`integration`、`messaging`、`job`、`config`。

重点不是包名好看，而是控制依赖方向和变更半径。

## 推荐结构

```text
com.emall.order
  api
  service
  domain
  repository
  integration
  messaging
  job
  config
```

### api

HTTP Controller、请求 DTO、响应 DTO、异常处理。

### service

应用服务，负责编排业务流程和事务。

### domain

领域对象、枚举、状态迁移规则。

### repository

数据访问接口和实现。

### integration

下游服务客户端，例如库存、价格、营销。

### messaging

MQ 生产、消费和事件处理。

### job

定时任务、补偿任务、清理任务。

### config

Spring 配置类。

## 依赖方向

推荐：

```text
api -> service
service -> domain
service -> repository
service -> integration
service -> messaging/outbox
repository -> domain
```

不推荐：

```text
domain -> api
domain -> repository implementation
repository -> controller
```

领域对象不应该依赖 HTTP，Repository 不应该知道 Controller。

## 按技术层横向拆还是按业务拆？

单个服务内部可以分层，但整个系统应该按业务域拆模块。

不推荐：

```text
controller-service
dao-service
```

推荐：

```text
order
inventory
payment
product
user
```

每个业务服务内部再分 api、service、domain、repository。

## 包结构和微服务边界

包结构清晰后，即使早期是模块化单体，未来也更容易拆服务。

例如订单相关代码都在 `order` 模块，库存相关代码都在 `inventory` 模块。
它们通过接口、事件或 Client 协作，而不是互相访问内部类和表。

## 常见坏味道

- `utils` 里什么都放。
- `common` 里放具体业务规则。
- Controller 直接引用 Repository。
- Domain 依赖 Spring Web。
- 不同业务模块互相引用内部实现。
- 包名按人名、页面名或临时需求命名。

## 在 eMall 项目中怎么讲？

eMall 中核心模块可以这样理解：

```text
order/api          下单、支付、取消接口
order/service      订单业务编排
order/domain       订单、订单状态
order/repository   订单持久化
order/integration  库存、价格、营销客户端
```

`common` 只放跨模块基础能力，例如统一响应、错误码、Outbox 基础设施、审计、加密、Trace。
不应该把具体订单支付规则塞进 `common`。

## 专家级完整回答

```text
清晰包结构的目标是控制复杂度和变更半径。我会按业务模块拆服务，
在每个服务内部按 api、service、domain、repository、integration、messaging、job、config 分层。
依赖方向从外层协议进入应用服务，再到领域、数据和下游适配器。
同时限制 common 只放真正通用的基础能力，避免变成业务垃圾桶。
```

## 回答评分点

高分答案应该覆盖：

- 能给出清晰包结构。
- 能说明各层职责。
- 能说明依赖方向。
- 能区分业务拆分和技术层拆分。
- 能指出 common 滥用风险。
