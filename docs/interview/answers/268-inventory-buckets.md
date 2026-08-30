# 268 库存桶如何降低热点行竞争？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

库存桶是把一个 SKU 的库存拆成多个子库存记录，请求按随机或哈希方式落到不同桶上扣减。
这样同一 SKU 的高并发写不再集中竞争一行，而是分散到多行，降低行锁等待。

库存桶提升并发，但会增加库存汇总、补桶和一致性处理复杂度。

## 基本设计

例如 SKU 总库存 1000，拆成 10 个桶：

```text
sku_id = 100, bucket = 0, stock = 100
sku_id = 100, bucket = 1, stock = 100
...
sku_id = 100, bucket = 9, stock = 100
```

扣减时选择一个桶执行条件更新。

## 优点

优点：

- 降低单行锁竞争。
- 提高并发扣减吞吐。
- 热点压力被分散。
- 可按桶扩展处理。

它适合热点 SKU 高并发扣减。

## 复杂度

复杂点：

- 如何分配初始库存。
- 某个桶扣完后如何换桶。
- 如何计算总可售库存。
- 如何防止总库存超卖。
- 如何做库存回滚和对账。

不能只拆表结构，不设计总量约束。

## 电商系统实践

秒杀 SKU 可以把库存拆成多个库存桶，请求随机选择桶扣减。

如果某个桶扣减失败，可以尝试其他桶，但要限制尝试次数，避免请求在数据库内循环放大。

## 深度增强：库存桶示意图

![库存桶降低热点行锁竞争](../assets/inventory-buckets.svg)

库存桶的本质是把“一个 SKU 一行库存”的热点写，拆成“一个 SKU 多行子库存”的分散写。
数据库仍然负责每个桶内的条件更新，应用负责选桶、失败换桶、库存汇总和释放补偿。

## 深度增强：表结构和 SQL

库存桶表可以这样建：

```sql
CREATE TABLE sku_inventory_bucket (
    sku_id BIGINT NOT NULL,
    bucket_no INT NOT NULL,
    available INT NOT NULL,
    reserved INT NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (sku_id, bucket_no)
);
```

扣减某个桶仍然使用条件更新：

```sql
UPDATE sku_inventory_bucket
SET available = available - #{quantity},
    reserved = reserved + #{quantity},
    updated_at = CURRENT_TIMESTAMP
WHERE sku_id = #{skuId}
  AND bucket_no = #{bucketNo}
  AND available >= #{quantity};
```

## 深度增强：Java 17 选桶实现

```java
public final class InventoryBucketSelector {

    private final int bucketCount;

    public InventoryBucketSelector(int bucketCount) {
        if (bucketCount <= 0) {
            throw new IllegalArgumentException("Bucket count must be positive.");
        }
        this.bucketCount = bucketCount;
    }

    public int firstBucket(long skuId, long requestId) {
        return Math.floorMod(Objects.hash(skuId, requestId), bucketCount);
    }

    public int nextBucket(int currentBucket) {
        return (currentBucket + 1) % bucketCount;
    }
}
```

应用层要限制换桶次数，避免一次请求把所有桶都扫一遍：

```java
public ReservationResult reserve(ReserveCommand command) {
    int bucket = selector.firstBucket(command.skuId(), command.requestId());
    for (int attempt = 0; attempt < 3; attempt++) {
        int affectedRows = mapper.reserveBucket(command.skuId(), bucket, command.quantity());
        if (affectedRows == 1) {
            return ReservationResult.success(bucket);
        }
        bucket = selector.nextBucket(bucket);
    }
    return ReservationResult.outOfStock(command.skuId());
}
```

## 深度增强：生产边界

- 桶越多，行锁竞争越低，但库存汇总和释放越复杂。
- 换桶重试要有上限，否则会把数据库压力放大。
- 总可售库存不能只靠缓存，要能通过桶汇总校验。
- 订单取消或支付超时要释放到原桶，避免桶间数据漂移。
- 热点 SKU 可以动态增加桶数，但扩桶需要迁移和校验。
