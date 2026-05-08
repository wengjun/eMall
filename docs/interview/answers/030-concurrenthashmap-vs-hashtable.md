# 030 `ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？

[返回按分类学习面试题](../README.md)

## 题目

`ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？

## 先给面试官的短答案

`Hashtable` 通过 synchronized 锁住大部分方法，锁粒度粗，并发性能差。
`ConcurrentHashMap` 针对并发访问设计，读操作通常不阻塞，写操作也尽量局部化，
并提供 `computeIfAbsent`、`putIfAbsent` 等原子复合操作。

所以并发场景下，`ConcurrentHashMap` 通常比 `Hashtable` 更合适。

## 从零基础理解

多个线程同时访问 Map 时，普通 `HashMap` 不安全。
早期 Java 提供 `Hashtable`，它通过给方法加锁保证线程安全：

```java
public synchronized V get(Object key) {
}
```

问题是锁太粗。一个线程写入时，其他线程读写也容易被阻塞。

`ConcurrentHashMap` 的目标是提高并发度，让不同 key 的操作尽量不要互相阻塞。

## ConcurrentHashMap 的优势

### 并发性能更好

读操作通常不需要锁住整个 Map。写操作也不是锁整张表。

### 提供原子复合操作

并发代码中，先 get 再 put 不是原子的：

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

两个线程可能同时进入。

更好的写法：

```java
map.putIfAbsent(key, value);
```

或：

```java
RateLimiter limiter = limiters.computeIfAbsent(key, ignored -> new RateLimiter());
```

### 迭代弱一致

遍历时允许并发修改，不会像普通集合那样轻易抛 `ConcurrentModificationException`。
但它也不保证遍历看到的是某一瞬间的完整快照。

### 不允许 null

`ConcurrentHashMap` 不允许 null key 和 null value，避免并发场景下无法区分不存在和 value 为 null。

## Hashtable 的问题

- 老旧。
- 锁粒度粗。
- API 设计过时。
- 并发度低。
- 现代项目基本不推荐新代码使用。

## 需要注意的坑

### Map 线程安全不代表 value 线程安全

```java
ConcurrentHashMap<String, List<String>> map = new ConcurrentHashMap<>();
```

Map 结构安全，但 `List` 不是线程安全的。

### 多步骤业务逻辑仍要原子化

如果逻辑是：

```text
读取库存 -> 判断 -> 修改库存
```

不能只靠 `ConcurrentHashMap`。多实例部署下还要依赖数据库条件更新、分布式锁或幂等。

### 本地并发容器不能解决分布式并发

`ConcurrentHashMap` 只在当前 JVM 内有效。多个 Pod 各有一份 Map，不能用它做全局幂等。

## 在 eMall 项目中怎么讲？

适合用 `ConcurrentHashMap` 的地方：

- 本地内存限流器缓存。
- 本地测试用 InMemoryRepository。
- 本地任务状态。
- 非关键路径的进程内缓存。

不适合：

- 生产全局幂等。
- 订单去重最终兜底。
- 库存防超卖。
- 支付回调去重最终兜底。

这些必须依赖数据库唯一约束、持久化记录或分布式协调。

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
Hashtable 通过 synchronized 锁住方法，锁粒度粗，并发性能差。
ConcurrentHashMap 是为并发访问设计的，读操作通常不阻塞，写操作尽量局部化，
并提供 putIfAbsent、computeIfAbsent 这类原子复合操作。

但 ConcurrentHashMap 只保证当前 JVM 内 Map 结构的线程安全，不代表 value 线程安全，
也不能解决多实例分布式并发。在电商系统里，它适合本地缓存和本地限流器，
但订单幂等、库存防超卖、支付回调去重必须由数据库约束或持久化幂等记录兜底。
```

## 回答评分点

高分答案应该覆盖：

- Hashtable 锁粒度粗。
- ConcurrentHashMap 并发度更高。
- 能说出 putIfAbsent、computeIfAbsent。
- 能指出 value 不一定线程安全。
- 能指出它不能解决分布式并发。

## 深度完善：面向 L6 的回答框架

围绕「`ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

重复补强内容已合并到 [面试补强共享框架](../deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：`ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
