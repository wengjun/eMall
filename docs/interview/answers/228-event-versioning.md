# 228 如何设计事件版本？

[返回按分类学习面试题](../README.md)

## 题目

如何设计事件版本？

## 先给面试官的短答案

事件版本要同时解决 schema 演进和业务状态演进。
通常事件中要包含 `eventType`、`schemaVersion`、`eventId`、`aggregateId`、`aggregateVersion`、`occurredAt` 和 trace 信息。

消费者根据版本判断是否兼容、是否重复、是否旧事件。

## 关键字段

建议字段：

```json
{
    "eventId": "evt-1",
    "eventType": "OrderPaid",
    "schemaVersion": 2,
    "aggregateId": "order-1",
    "aggregateVersion": 5,
    "occurredAt": "2026-04-30T08:00:00Z",
    "traceId": "01HX..."
}
```

`schemaVersion` 表示结构版本，`aggregateVersion` 表示业务对象版本。

## schema 版本

用于处理：

- 新增字段。
- 字段废弃。
- 字段类型变更。
- 枚举扩展。
- 兼容性检查。

新增可选字段通常兼容，删除或改类型通常不兼容。

## 业务版本

业务版本用于：

- 防止旧事件覆盖新状态。
- 判断事件顺序。
- 支持幂等处理。
- 处理重放场景。

例如订单当前版本是 `5`，消费者收到版本 `4` 的事件，应谨慎丢弃或忽略。

## 在 eMall 项目中怎么讲？

订单事件应同时带 `schemaVersion` 和 `orderVersion`。

履约服务消费订单事件时，如果事件 schema 不兼容则进入死信；如果 `orderVersion` 低于已处理版本则忽略。

## 共性一致性模型

状态机、Outbox、幂等、重试、对账和补偿的统一闭环及 Java 17 示例见
[共享模型：最终一致、状态机和补偿](../shared-answer-models.md#最终一致状态机和补偿)。

## 专家级完整回答

```text
事件版本分两类：schemaVersion 管事件结构兼容，aggregateVersion 管业务对象状态顺序。
事件还应包含 eventId、eventType、aggregateId、occurredAt 和 traceId，支持幂等、追踪和重放。

消费者通过 schemaVersion 判断能否解析，通过 aggregateVersion 防止旧事件覆盖新状态。
```

## 回答评分点

高分答案应该覆盖：

- 事件版本包括 schema 和业务对象版本。
- `eventId` 支持幂等。
- `aggregateVersion` 支持顺序判断。
- `schemaVersion` 支持兼容治理。
- 事件应包含时间和 trace 信息。
