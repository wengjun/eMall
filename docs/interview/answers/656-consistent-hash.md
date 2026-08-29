# 656 手写一致性 hash。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.SortedMap;
import java.util.TreeMap;

final class ConsistentHash {
    private final SortedMap<Integer, String> ring = new TreeMap<>();

    void add(String node) {
        for (int i = 0; i < 128; i++) {
            ring.put((node + "#" + i).hashCode(), node);
        }
    }

    String locate(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("empty ring");
        }
        int hash = key.hashCode();
        SortedMap<Integer, String> tail = ring.tailMap(hash);
        return ring.get(tail.isEmpty() ? ring.firstKey() : tail.firstKey());
    }
}
```

### 必测用例

- 相同键稳定路由，新增或删除节点后只有相邻区间的键迁移。
- 验证虚拟节点改善分布，并处理哈希碰撞、空环和重复节点。
- 并发更新环与查询路由时不能看到部分构建状态。

### 生产化差异

- 需要权重、健康状态、复制因子和不可变快照更新，不能只实现一个 `TreeMap`。
- 节点变化仍会迁移数据，必须定义预热、双读或回退策略。
