# 655 手写分布式 ID 生成器简化版。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.function.LongSupplier;

final class SnowflakeLikeId {
    private static final long EPOCH = 1_700_000_000_000L;
    private static final long MAX_WORKER_ID = 1_023L;
    private static final long MAX_SEQUENCE = 4_095L;

    private final long workerId;
    private final LongSupplier clock;
    private long lastMillis = -1;
    private long sequence;

    SnowflakeLikeId(long workerId, LongSupplier clock) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("invalid worker ID");
        }
        this.workerId = workerId;
        this.clock = clock;
    }

    synchronized long nextId() {
        long now = clock.getAsLong();
        if (now < lastMillis) {
            throw new IllegalStateException("clock moved backwards");
        }
        if (now == lastMillis) {
            if (sequence == MAX_SEQUENCE) {
                now = waitForNextMillis(lastMillis);
                sequence = 0;
            } else {
                sequence++;
            }
        } else {
            sequence = 0;
        }
        lastMillis = now;
        return ((now - EPOCH) << 22) | (workerId << 12) | sequence;
    }

    SnowflakeLikeId(long workerId) {
        this(workerId, System::currentTimeMillis);
    }

    private long waitForNextMillis(long previousMillis) {
        long now = clock.getAsLong();
        while (now <= previousMillis) {
            Thread.onSpinWait();
            now = clock.getAsLong();
        }
        return now;
    }
}
```

### 必测用例

- 单线程与高并发生成均唯一，并验证同毫秒序列溢出行为。
- 模拟时钟小幅和大幅回拨，确认等待、拒绝或切换策略符合约定。
- 两个节点使用重复 worker ID 时测试必须暴露冲突。

### 生产化差异

- worker ID 需要租约或集中分配，时钟偏差、序列耗尽和生成失败必须告警。
- 示例在序列耗尽时短暂自旋；生产实现必须限制等待时间，避免时钟异常导致业务线程永久阻塞。
- ID 只保证技术唯一性；订单号等外部标识还要考虑枚举风险和业务格式。
