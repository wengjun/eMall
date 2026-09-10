# 029 `ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？

[返回按分类学习面试题](../README.md)

## Java 容器速查

| 类型 | 主要用途 | Java 使用注意 |
| --- | --- | --- |
| `ArrayList` | 顺序保存、随机访问 | 默认列表选择；扩容会复制引用，元素对象不是内联存储 |
| `LinkedList` | 已定位迭代器处插入删除 | 先按下标查找仍为 O(n)，节点分配和局部性通常不如数组 |
| `HashMap` | 无序键值查找 | 允许 null；不是线程安全容器 |
| `TreeMap` | 有序键、范围查询 | 按 comparator 判定键等价，比较结果为 0 会视为同一个 key |
| `ArrayDeque` | 栈、双端队列 | 通常比 LinkedList 更适合队列用途，不允许 null |

不展开数组、链表和树的通用实现。只需特别记住：Java 集合存放对象引用，
`List<Integer>` 涉及装箱，不等同于连续的 `int[]`。

```java
import java.util.NavigableMap;
import java.util.TreeMap;

NavigableMap<Long, String> orders = new TreeMap<>();
orders.put(100L, "created");
orders.put(200L, "paid");
System.out.println(orders.subMap(100L, true, 200L, false));
```

`subMap` 是原 Map 的视图，修改会相互影响。需要独立快照时复制。
树容器的比较规则最好与 equals 一致，例如 BigDecimal 的数值排序与 equals 就存在差异。
