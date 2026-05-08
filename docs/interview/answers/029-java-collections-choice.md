# 029 `ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？

## 先给面试官的短答案

`ArrayList` 基于数组，适合随机访问、遍历和尾部追加，是最常用的 List。
`LinkedList` 基于链表，理论上适合头尾插入删除，但实际业务中常被 `ArrayDeque` 替代。
`HashMap` 基于哈希，适合按 key 快速查找。
`TreeMap` 基于红黑树，key 有序，适合排序遍历和范围查询。

集合选择要看访问模式、数据量、是否有序、是否并发，而不是只背时间复杂度。

## `ArrayList`

特点：

- 底层数组。
- 按下标访问快。
- 遍历快，内存局部性好。
- 尾部追加通常快。
- 中间插入删除可能需要移动元素。

适合：

- 订单明细列表。
- 商品列表。
- 查询结果列表。
- 优惠券列表。

示例：

```java
List<OrderLine> lines = new ArrayList<>();
```

大多数业务列表优先选 `ArrayList`。

## `LinkedList`

特点：

- 双向链表。
- 随机访问慢。
- 每个节点有额外对象开销。
- 内存局部性差。
- 理论上头尾插入删除快。

业务中不常用。队列场景通常优先：

```java
Deque<Task> queue = new ArrayDeque<>();
```

面试要避免机械说“LinkedList 插入删除快”。如果你先找到位置，它确实改指针快；
但找到位置本身可能要遍历，而且对象开销大。

## `HashMap`

特点：

- 按 key 查找快。
- 无序。
- key 要稳定。
- 依赖 `equals` 和 `hashCode`。

适合：

- 按 skuId 聚合订单行。
- 按 requestId 存幂等结果。
- 按 eventId 去重。
- 按 userId 查上下文。

示例：

```java
Map<Long, Integer> quantityBySku = new HashMap<>();
```

## `TreeMap`

特点：

- key 有序。
- 底层红黑树。
- 支持范围查询。
- 查找、插入、删除通常是 O(log n)。

适合：

- 按时间排序的小任务集合。
- 价格区间规则。
- 需要 `subMap`、`headMap`、`tailMap` 的场景。

示例：

```java
TreeMap<Instant, List<Task>> tasksByTime = new TreeMap<>();
```

## 如何选择？

按问题问自己：

- 是否需要按下标访问？用 `ArrayList`。
- 是否需要按 key 查找？用 `HashMap`。
- 是否需要有序 key 或范围查询？用 `TreeMap`。
- 是否需要队列？优先 `ArrayDeque`。
- 是否多线程并发访问？考虑并发集合或外部同步。

## 在 eMall 项目中怎么讲？

- 订单明细：`ArrayList`。
- skuId 到数量聚合：`HashMap<Long, Integer>`。
- 库存桶编号到桶：`HashMap<Integer, InventoryBucket>`。
- 定时任务按执行时间排序：`TreeMap<Instant, Task>` 或数据库扫描。
- 高并发共享 Map：`ConcurrentHashMap`。

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
ArrayList 适合大多数顺序列表和遍历场景，因为数组内存局部性好。
LinkedList 理论上插入删除快，但实际业务中随机访问慢、对象开销大，队列场景我更倾向 ArrayDeque。
HashMap 适合按 key 快速查找，但 key 必须稳定并正确实现 equals/hashCode。
TreeMap 保持 key 有序，适合排序遍历和范围查询。

集合选择要基于访问模式、数据量、有序性和并发需求，而不是只背复杂度。
```

## 回答评分点

高分答案应该覆盖：

- 能说明四种集合底层特点。
- 能说明 LinkedList 的现实局限。
- 能联系业务场景。
- 能提到 key 稳定和 equals/hashCode。
- 能补充并发场景要考虑并发集合。

## 深度完善：面向 L6 的回答框架

围绕「`ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：`ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
