# 040 如何做模块边界和依赖方向治理？

[返回按分类学习面试题](../README.md)

## 题目

如何做模块边界和依赖方向治理？

## 先给面试官的短答案

模块边界治理的目标是控制复杂度和变更半径。要明确每个模块的数据所有权、职责范围、
对外契约和允许依赖方向，并用构建规则、代码评审、架构测试和文档持续约束。

在微服务电商系统里，订单、库存、支付应该各自拥有数据，通过 API 或事件协作，不能直接访问彼此数据库。

## 治理什么？

### 职责边界

每个模块负责什么，不负责什么。

例如：

- order 负责订单状态。
- inventory 负责库存数量和预占。
- payment 负责支付单、流水、对账。

### 数据所有权

谁拥有表，谁能写表。

订单服务不能直接写支付表，支付服务不能直接改库存表。

### 依赖方向

允许：

```text
business module -> common
api -> service -> domain/repository/integration
```

不允许：

```text
domain -> api
business module -> another module internal repository
```

### 对外契约

模块对外通过：

- HTTP API。
- MQ 事件。
- SDK 接口。
- 文档化 DTO。

## 工具手段

### Maven 依赖控制

子模块只声明允许的依赖。

### Checkstyle 或 ArchUnit

可以写规则禁止某些包互相依赖。

例如：

```text
domain should not depend on api
order should not depend on payment.repository
```

### Code Review

评审时关注：

- 是否跨模块访问内部类。
- 是否把业务规则放进 common。
- 是否绕过 API 直接访问表。
- 是否引入循环依赖。

### 文档

模块清单要说明职责、数据所有权、接口和事件。

## common 模块怎么治理？

适合放入 common：

- `ApiResponse`
- `ErrorCode`
- `BusinessException`
- Trace 工具。
- Outbox 基础接口。
- 审计基础模型。
- 加密接口。

不适合：

- 订单状态迁移规则。
- 具体支付渠道逻辑。
- 库存扣减策略。
- 促销计算规则。

common 如果不治理，会变成所有模块互相耦合的中心。

## 在 eMall 项目中怎么讲？

eMall 现在有多个业务模块。治理重点是：

- 每个模块有自己的领域模型和 Repository。
- 公共能力抽到 common，但不抽具体业务。
- 服务间通过 Client 或事件交互。
- 根 POM 管理依赖版本。
- 文档维护模块职责和 profile。

## 专家级完整回答

```text
模块边界治理要从数据所有权和依赖方向入手。订单拥有订单数据，库存拥有库存数据，
支付拥有支付和流水数据。跨模块不能直接访问彼此表和内部 Repository，
只能通过 API、事件或明确契约协作。

工程上我会用 Maven 依赖、包结构、ArchUnit/Checkstyle、代码评审和模块文档共同治理。
common 只放真正通用的基础能力，不放具体业务规则，避免变成耦合中心。
```

## 回答评分点

高分答案应该覆盖：

- 数据所有权。
- 依赖方向。
- API/事件契约。
- Maven、架构测试、Code Review。
- common 治理边界。
