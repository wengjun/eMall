# 125 REQUIRED、REQUIRES_NEW、NESTED 有什么区别？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`REQUIRED` 是默认行为，有事务就加入，没有就创建；`REQUIRES_NEW` 总是创建独立新事务，并挂起外部事务；
`NESTED` 在外部事务中创建保存点，内部回滚可回到保存点。`REQUIRES_NEW` 是两个物理事务，
`NESTED` 通常是同一个物理事务中的保存点。

## REQUIRED

外部有事务：

```text
outer transaction
  inner REQUIRED joins outer
```

内部异常如果未处理，通常会导致整个事务回滚。

适合主业务流程。

## REQUIRES_NEW

外部有事务时：

```text
outer transaction suspended
  inner new transaction commits or rolls back
outer transaction resumes
```

内外事务独立提交或回滚。

外部回滚不一定影响内部已提交事务。

风险是需要额外数据库连接。

## NESTED

外部有事务时：

```text
outer transaction
  savepoint
  inner nested
  rollback to savepoint if inner fails
outer continues
```

它依赖数据库保存点。

外部事务最终回滚时，嵌套事务结果也会一起回滚。

## 关键区别

| 行为 | 事务关系 | 外部回滚影响内部 | 内部失败影响外部 |
| --- | --- | --- | --- |
| REQUIRED | 同一事务 | 影响 | 通常影响 |
| REQUIRES_NEW | 独立事务 | 不影响已提交内部事务 | 可捕获后不影响 |
| NESTED | 同一事务保存点 | 影响 | 可回滚到保存点 |

## 使用建议

建议：

- 主流程默认 `REQUIRED`。
- 独立审计或日志可用 `REQUIRES_NEW`。
- 需要部分回滚且数据库支持保存点时考虑 `NESTED`。
- 不要用传播行为修补糟糕的业务边界。

## 在 eMall 项目中怎么讲？

订单创建和订单明细保存通常在同一个 `REQUIRED` 事务中。

审计日志可以用 `REQUIRES_NEW`，确保主事务失败时仍记录失败原因。

如果批量处理多个子项，允许单个子项失败回滚到保存点，可以考虑 `NESTED`，但要验证数据库支持。
