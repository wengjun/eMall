# 028 为什么可变对象不适合作为 `HashMap` 的 key？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`HashMap` 根据 key 的 `hashCode` 定位桶，再用 `equals` 找具体 entry。
如果 key 放入 Map 后，参与 `hashCode` 或 `equals` 的字段被修改，后续查找会计算出不同位置，
导致这个 key 明明在 Map 里却找不到。

所以 Map key 应该稳定，最好使用不可变对象，例如 `String`、`Long`、`record` 值对象。

## 从零基础理解

假设有一个可变 key：

```java
class UserKey {
    private String type;
    private String value;
}
```

放入 Map：

```java
Map<UserKey, String> map = new HashMap<>();
UserKey key = new UserKey("mobile", "15500000000");
map.put(key, "user-1");
```

如果之后改了 key：

```java
key.setValue("16600000000");
```

再查：

```java
map.get(key)
```

可能拿不到，因为 hash 位置变了。

## 为什么这在生产中危险？

在电商系统里，key 很多：

- 缓存 key。
- 幂等 key。
- 限流 key。
- 库存桶 key。
- 用户会话 key。
- MQ 去重 key。

如果 key 不稳定，会导致：

- 缓存命中率异常下降。
- 幂等失效，重复下单。
- 限流绕过。
- 去重失败。
- 内存泄漏，因为旧 key 找不到也删不掉。

## 推荐写法

使用不可变 record：

```java
public record InventoryBucketKey(long skuId, int bucketNo) {
}
```

或使用字符串 key：

```java
String key = "inventory:" + skuId + ":" + bucketNo;
```

关键是 key 创建后不要再变化。

## 如果必须使用对象作为 key？

确保：

- 字段 final。
- 参与 equals/hashCode 的字段不可变。
- 不暴露 setter。
- 集合字段做防御性拷贝。

```java
public final class IdempotencyKey {
    private final String service;
    private final String requestId;
}
```
