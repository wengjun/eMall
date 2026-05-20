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

## 深度增强：工程化理解图

![Java 工程能力从语法到生产设计](../assets/java-engineering-model.svg)

这类题不能只停留在语法解释。生产系统更关心它如何改善建模、降低误用、保护兼容性、提升可测试性，
以及能否让团队在多人协作中保持稳定边界。回答时要从语言特性落到业务约束和工程治理。

## 深度增强：Java 17 落地示例

```java
import java.util.Objects;

record StableApiField(String name, String type, boolean required) {

    StableApiField {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        if (name.isBlank() || type.isBlank()) {
            throw new IllegalArgumentException("API field metadata must be explicit");
        }
    }
}

final class ApiCompatibilityPolicy {

    boolean canAddField(StableApiField field) {
        return !field.required();
    }
}
```

这段代码体现 Java 17 在工程建模中的价值：用 `record` 表达不可变数据，用构造校验保护边界，
用小的策略类表达兼容规则。面试中要把语法能力和 API 演进、错误预防、团队协作联系起来。

## 深度增强：生产边界

语言特性不是越新越好。核心原则是可读、可测、可维护、可兼容。任何语法选择都要能让代码意图更清晰，
而不是为了炫技。公共 API、金额、时间、状态、异常和 DTO 都要有稳定约束，避免线上数据被随意破坏。

## 深度增强：面试高分表达

我会先回答概念，再说明它在电商系统中的真实作用。例如金额要避免精度错误，状态要可兼容扩展，
DTO 和领域对象要隔离外部契约和内部模型。这样能体现我不是只会写 Java 语法，而是能做工程设计。

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

## 深度完善：面向 L6 的回答框架

围绕「如何设计清晰的包结构？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「Java 语言和工程基础」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `common、order、inventory、payment 的 DTO、值对象、异常和公共 API` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：代码评审问题数、缺陷逃逸率、兼容性测试结果、静态检查违规数。
- 验证证据：代码规范、单元测试、契约测试、兼容性用例和重构前后缺陷数据。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引
本题复习重点：如何设计清晰的包结构？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
