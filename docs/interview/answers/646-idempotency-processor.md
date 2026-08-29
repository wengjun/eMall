# 646 手写幂等处理器。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

final class IdempotencyProcessor {
    private final Map<String, String> results = new ConcurrentHashMap<>();

    String execute(String requestId, Supplier<String> action) {
        return results.computeIfAbsent(requestId, ignored -> action.get());
    }
}
```

### 必测用例

- 相同键和相同请求只执行一次并返回同一结果；相同键不同参数必须冲突。
- 并发提交相同键时只能有一个执行者，其余等待或复用结果。
- 业务失败、进程中断和处理中记录超时后能否安全重试。

### 生产化差异

- 幂等声明和业务写入应使用持久化唯一约束，并尽量处于同一本地事务。
- 还要保存请求摘要与结果，设计处理中租约、记录 TTL、分区和归档。
