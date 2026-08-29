# 655 手写分布式 ID 生成器简化版。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
final class SnowflakeLikeId {
    private final long workerId;
    private long lastMillis = -1;
    private long sequence;

    synchronized long nextId() {
        long now = System.currentTimeMillis();
        if (now < lastMillis) {
            throw new IllegalStateException("clock moved backwards");
        }
        if (now == lastMillis) {
            sequence = (sequence + 1) & 4095;
        } else {
            sequence = 0;
            lastMillis = now;
        }
        return ((now - 1_700_000_000_000L) << 22) | (workerId << 12) | sequence;
    }

    SnowflakeLikeId(long workerId) {
        this.workerId = workerId;
    }
}
```

### 必测用例

- 单线程与高并发生成均唯一，并验证同毫秒序列溢出行为。
- 模拟时钟小幅和大幅回拨，确认等待、拒绝或切换策略符合约定。
- 两个节点使用重复 worker ID 时测试必须暴露冲突。

### 生产化差异

- worker ID 需要租约或集中分配，时钟偏差、序列耗尽和生成失败必须告警。
- ID 只保证技术唯一性；订单号等外部标识还要考虑枚举风险和业务格式。
