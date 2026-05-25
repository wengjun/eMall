# 027 `equals` 和 `hashCode` 的契约是什么？

[返回按分类学习面试题](../README.md)

## 题目

`equals` 和 `hashCode` 的契约是什么？

## 先给面试官的短答案

核心契约是：如果两个对象 `equals` 返回 true，它们的 `hashCode` 必须相同。
反过来不要求成立，也就是说 hash 相同不代表对象一定相等。

此外 `equals` 要满足自反性、对称性、传递性、一致性，以及和 null 比较返回 false。
这个契约对 `HashMap`、`HashSet`、缓存 key、幂等 key 都非常重要。

## 从零基础理解

`equals` 判断两个对象是否业务上相等。
`hashCode` 用于哈希容器快速定位对象。

例如：

```java
Set<SkuKey> set = new HashSet<>();
set.add(new SkuKey(10001L, 1));
```

当你查找：

```java
set.contains(new SkuKey(10001L, 1));
```

HashSet 会先用 `hashCode` 找桶，再用 `equals` 判断是否相等。

如果 `equals` 和 `hashCode` 不一致，就会出现放进去找不到的问题。

## equals 的规则

### 自反性

```java
a.equals(a) == true
```

### 对称性

```java
a.equals(b) == b.equals(a)
```

### 传递性

如果：

```text
a equals b
b equals c
```

那么：

```text
a equals c
```

### 一致性

对象状态不变时，多次调用结果一致。

### null

```java
a.equals(null) == false
```

## hashCode 的规则

如果：

```java
a.equals(b)
```

那么必须：

```java
a.hashCode() == b.hashCode()
```

但 hash 相同不代表 equals 一定 true，因为不同对象可能 hash 冲突。

## 业务系统里的坑

### 可变字段参与 hash

如果一个对象作为 HashMap key 后，参与 hash 的字段被修改，就可能找不到。

### Entity 用 ID 判断相等

数据库实体常用 ID 判断相等，但 ID 生成前怎么办？

如果新对象 ID 为 null，两个新对象是否相等？这要谨慎设计。

### Lombok 自动生成

`@Data` 会自动生成 equals/hashCode，但可能把不该参与比较的字段放进去。
例如订单对象如果把 updatedAt 放进去，更新时间变化会影响相等性。

## 推荐实践

值对象可以使用所有不可变字段：

```java
public record SkuBucketKey(long skuId, int bucketNo) {
}
```

实体对象要谨慎：

- 有稳定 ID 后，用 ID。
- ID 未生成前，避免放入 HashSet/HashMap。
- 不要让可变业务字段参与 hash key。

## 专家级完整回答

```text
equals 和 hashCode 的核心契约是 equals 为 true 的对象 hashCode 必须相同。
equals 还要满足自反、对称、传递、一致和 null 返回 false。

这对 HashMap、HashSet、缓存 key、幂等 key 都很重要。
在业务系统里，我会让值对象使用不可变字段生成 equals/hashCode；
实体对象如果用数据库 ID，要注意 ID 生成前不要放入哈希集合。
也不会无脑用 Lombok @Data 生成领域对象的 equals/hashCode。
```

## 回答评分点

高分答案应该覆盖：

- equals true 必须 hashCode 相同。
- 能说出 equals 五个规则。
- 能解释 HashMap/HashSet 依赖。
- 能指出可变字段和实体 ID 的坑。
- 能提到 Lombok 自动生成风险。
