# 066 如何减少不必要的对象分配？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

减少对象分配要先用 JFR 或 allocation profiler 找热点，再针对性优化。常见手段包括避免循环内重复创建对象、
控制集合容量、减少中间集合、优化字符串拼接和日志、分页处理大数据、复用昂贵资源、避免无意义装箱。

原则是先测量，再优化；优先消除真正热点，而不是消灭所有对象。

## 先定位分配热点

不要凭感觉优化。

应该先看：

- 哪些类分配最多。
- 哪些调用栈分配最多。
- allocation rate 是否异常。
- young GC 是否频繁。
- P99 是否与 GC 或分配峰值相关。

工具包括：

- JFR。
- async-profiler allocation mode。
- JDK Mission Control。
- YourKit 或 JProfiler。

## 避免循环内重复创建

循环中重复创建相同对象很常见。

低效示例：

```java
for (OrderLine line : lines) {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    result.add(formatter.format(line.createdAt()));
}
```

如果对象不可变且线程安全，可以移到循环外或定义为常量。

```java
private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
```

注意不是所有对象都能共享，必须确认线程安全。

## 控制集合容量

集合扩容会产生额外数组和复制成本。

如果能预估大小，可以指定容量。

```java
List<OrderView> views = new ArrayList<>(orders.size());
```

这适合批量转换、查询结果映射、导出数据准备等场景。

## 减少中间集合

链式流式处理可读性好，但在热点路径中可能创建额外对象。

例如多次 `map`、`filter`、`collect` 可能产生中间对象和 lambda 开销。

如果 profiling 证明它是热点，可以改成单次循环。

但不要全局禁用 Stream。非热点代码中，可读性更重要。

## 优化字符串和日志

字符串分配是常见热点。

注意：

- 避免在循环中使用低效拼接。
- 日志使用占位符。
- DEBUG 日志前判断是否启用。
- 避免记录超大对象。
- JSON 序列化避免重复转换。

示例：

```java
log.debug("Create order request userId={}, skuId={}", userId, skuId);
```

不要提前拼接：

```java
log.debug("Create order request userId=" + userId + ", skuId=" + skuId);
```

## 避免无意义装箱

频繁装箱会创建对象。

例如：

```java
Long total = 0L;
for (long value : values) {
    total += value;
}
```

热点代码中可以使用 primitive：

```java
long total = 0L;
```

集合泛型无法存 primitive 时，可以考虑专门集合库，但要权衡依赖和复杂度。

## 分页和流式处理

一次性加载大量数据会产生大量对象。

更好的方式：

- 分页查询。
- 游标处理。
- 流式导出。
- 批次提交。
- 限制最大返回条数。

电商后台导出订单、对账、报表都需要避免一次性全量加载。

## 复用昂贵资源

不是所有对象都需要复用。

适合复用的是昂贵资源：

- 线程池。
- 数据库连接。
- HTTP 连接。
- 大 direct buffer。
- JSON mapper。
- 正则 Pattern。

普通业务 DTO 不建议复杂复用。

## 电商系统实践

如果营销规则计算分配过高，优化步骤应该是：

- 用 JFR 找到分配最多的类和栈。
- 看是否循环内重复创建规则上下文。
- 看是否反复序列化商品和用户画像。
- 看是否多次构造中间集合。
- 对热点路径做容量预估、缓存不可变对象和减少中间对象。

这样比盲目重写所有代码更有效。
