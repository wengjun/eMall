# 657 手写 Top K 统计。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

static List<Integer> topK(List<Integer> values, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>(Comparator.naturalOrder());
    for (int value : values) {
        heap.offer(value);
        if (heap.size() > k) {
            heap.poll();
        }
    }
    return heap.stream().sorted(Comparator.reverseOrder()).toList();
}
```

### 必测用例

- 覆盖 `k=0`、`k=1`、`k` 大于元素数、重复值、负数和空输入。
- 与全量排序结果对照，并验证最小堆方向没有写反。
- 大输入下确认内存复杂度保持为 O(k)。

### 生产化差异

- 流式数据使用有界堆，分布式场景先分片 Top K 再归并。
- 对象排序必须定义并列规则、稳定性和比较器溢出边界。
