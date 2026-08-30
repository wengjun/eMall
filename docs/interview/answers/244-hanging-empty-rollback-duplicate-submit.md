# 244 如何处理悬挂、空回滚和重复提交？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

悬挂、空回滚和重复提交通常出现在 TCC 或补偿事务里。
解决核心是为每个业务事务记录 Try、Confirm、Cancel 状态，并让 Confirm 和 Cancel 都幂等且可识别当前事务阶段。

不要假设 Try 一定先于 Cancel，也不要假设 Confirm 只会来一次。

## 三个概念

空回滚：

- Try 没执行成功，Cancel 先到了。

悬挂：

- Cancel 已执行，之后迟到的 Try 又来了。

重复提交：

- Confirm 被重复调用，导致资源重复扣减。

这些都是网络乱序和重试导致的正常异常。

## 处理方式

需要：

- 事务记录表。
- 全局事务 ID。
- 分支事务状态。
- Try 幂等。
- Confirm 幂等。
- Cancel 幂等。
- Cancel 后拒绝迟到 Try。

状态记录是判断当前动作是否合法的依据。

## 状态示例

状态可以是：

- TRYING。
- CONFIRMED。
- CANCELED。

如果收到 Cancel 且没有 Try 记录，可以插入 CANCELED 记录，表示空回滚已处理。

之后迟到 Try 看到 CANCELED，就直接拒绝，避免悬挂。

## 电商系统实践

库存 TCC 中，Cancel 可能先于 Try 到达。

库存服务应记录该事务已取消，后续迟到的 Try 不能再预占库存；Confirm 重复到达时也不能重复扣减。
