# 029 `ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？

[返回按分类学习面试题](../README.md)

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
