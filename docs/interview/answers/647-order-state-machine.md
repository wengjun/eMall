# 647 手写订单状态机。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.Map;
import java.util.Set;

enum OrderStatus {
    CREATED, PAID, CANCELLED, COMPLETED
}

final class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.COMPLETED),
            OrderStatus.CANCELLED, Set.of(),
            OrderStatus.COMPLETED, Set.of());

    static void check(OrderStatus from, OrderStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("illegal transition: " + from + " -> " + to);
        }
    }
}
```

### 必测用例

- 覆盖每条合法迁移，并验证跳跃、回退和终态后的迁移被拒绝。
- 同一事件重复到达应幂等；乱序事件不能覆盖更晚状态。
- 两个线程从同一前置状态迁移时只有一个条件更新成功。

### 生产化差异

- 数据库更新必须携带前置状态或版本条件，状态变更与 Outbox 事件同事务提交。
- 迁移规则要可审计，新增状态需做历史数据和事件契约兼容。
