# 640 手写阻塞队列。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.ArrayDeque;
import java.util.Queue;

final class BoundedBlockingQueue<T> {
    private final Queue<T> queue = new ArrayDeque<>();
    private final int capacity;

    BoundedBlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    synchronized void put(T item) throws InterruptedException {
        while (queue.size() == capacity) {
            wait();
        }
        queue.add(item);
        notifyAll();
    }

    synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait();
        }
        T item = queue.remove();
        notifyAll();
        return item;
    }
}
```

### 必测用例

- 验证 FIFO、容量上限，以及队列空时 `take`、队列满时 `put` 确实阻塞。
- 等待线程被中断后应退出且保留中断语义，不能吞掉 `InterruptedException`。
- 多生产者和多消费者下所有元素恰好消费一次，内部计数始终合法。

### 生产化差异

- 生产环境应优先使用 JDK 的 `ArrayBlockingQueue` 或 `LinkedBlockingQueue`。
- 队列必须有界，并配套积压指标、拒绝策略、停机唤醒和最长排队时间。
