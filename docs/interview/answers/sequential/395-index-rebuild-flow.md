# 395 如何设计索引重建流程？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

如何设计索引重建流程？

## 先给面试官的短答案

索引重建应使用新索引全量构建、增量事件追平、数据校验、灰度查询、别名切换和可回滚流程。
不能直接在旧索引上破坏性重建。

核心目标是线上无感、数据可校验、失败可恢复。

## 标准流程

流程：

- 创建新索引和 mapping。
- 从主数据源全量扫描。
- 批量写入新索引。
- 记录构建水位。
- 消费水位之后的增量事件。
- 校验数量和抽样数据。
- 灰度流量验证。
- 切换别名。
- 保留旧索引一段时间。

别名切换让回滚更简单。

## 水位设计

水位可以是：

- 数据库 `updated_at`。
- binlog 位点。
- Kafka offset。
- 事件时间加版本号。

水位用于保证全量构建期间发生的增量变更不会丢。

## 校验方式

校验：

- 文档数量。
- 核心字段抽样。
- 上下架状态。
- 类目品牌聚合。
- 搜索关键词结果。
- 错误和失败记录。

不能只看写入成功数量。

## 在 eMall 项目中怎么讲？

eMall 重建商品索引时，先创建 `product_search_v3`，从商品库和相关读模型全量构建。

构建开始时记录 Kafka offset，全量完成后消费这之后的商品变更事件追平。校验通过后切换
`product_search_current` 别名。

## 深度增强：索引重建图

![搜索索引作为可重建读模型](../../assets/search-read-model.svg)

索引重建的核心是“旧索引继续服务，新索引在旁路构建”。不能在旧索引上直接删除重建，
否则失败时线上查询会不可用，也很难回滚。

## 深度增强：重建任务模型

```java
public enum RebuildPhase {
    CREATE_INDEX,
    FULL_BUILD,
    CATCH_UP_EVENTS,
    VERIFY,
    CANARY_QUERY,
    SWITCH_ALIAS,
    COMPLETED
}

public record IndexRebuildTask(
        String taskId,
        String sourceIndex,
        String targetIndex,
        RebuildPhase phase,
        long startOffset,
        long currentOffset) {
}
```

别名切换必须是原子操作：

```java
public interface SearchIndexAdmin {

    void createIndex(String indexName, String mapping);

    void bulkWrite(String indexName, List<ProductSearchDocument> documents);

    void switchAlias(String alias, String oldIndex, String newIndex);
}
```

## 深度增强：校验策略

- 数量校验：商品库可搜索商品数量和索引文档数量。
- 字段校验：抽样校验标题、类目、品牌、上下架、版本。
- 查询校验：核心关键词、类目聚合、品牌过滤结果。
- 增量校验：水位之后的事件是否全部追平。
- 灰度校验：小比例查询走新索引并比较结果。

## 深度增强：面试高分表达

```text
我会创建新索引旁路构建，记录构建开始时的事件水位，全量导入后消费水位之后的增量事件追平。
校验通过后用别名原子切换，并保留旧索引作为回滚。关键是不能直接覆盖旧索引，也不能忽略全量期间的增量变更。
```

## 专家级完整回答

```text
索引重建不能直接覆盖旧索引。应创建新索引，全量构建，记录水位，消费增量事件追平，再做数量、
字段、状态和搜索结果校验。通过灰度验证后切换别名，并保留旧索引用于回滚。

关键点是处理全量期间的增量变更，避免重建完成后索引已经落后。
```

## 回答评分点

高分答案应该覆盖：

- 新索引全量构建。
- 记录水位并增量追平。
- 校验和灰度。
- 别名切换。
- 保留旧索引回滚。
