# 643 手写延迟队列。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

record DelayedTask(String id, long runAtNanos) implements Delayed {
    @Override
    public long getDelay(TimeUnit unit) {
        long delay = runAtNanos - System.nanoTime();
        return unit.convert(delay, TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
    }
}
```

### 必测用例

- 不同到期时间按顺序取出，未到期元素不能提前返回。
- 插入一个更早到期的新元素后，等待线程应被正确唤醒并重新计算等待时间。
- 覆盖相同到期时间、线程中断和空队列关闭。

### 生产化差异

- 内存延迟队列在进程重启后会丢任务，只适合可重建的短期调度。
- 重要任务应持久化并具备幂等、分片抢占、时钟偏差监控和失败补偿。
