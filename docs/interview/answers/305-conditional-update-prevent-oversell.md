# 305 条件更新如何防止库存超卖？

[返回按分类学习面试题](../README.md)

## 题目

条件更新如何防止库存超卖？

## 先给面试官的短答案

条件更新把库存判断和扣减放在同一条 SQL 中原子执行，例如 `available > 0` 时才扣减。
并发情况下只有满足条件的事务能更新成功，更新行数为 0 表示库存不足或冲突，从而避免先查后扣的竞争窗口。

它是库存扣减的常见正确性兜底。

## 示例

```sql
UPDATE stock
SET available = available - 1
WHERE sku_id = ?
  AND available > 0;
```

应用根据影响行数判断是否扣减成功。

如果影响行数为 1，扣减成功。

如果影响行数为 0，库存不足或记录不存在。

## 为什么能防超卖？

数据库会对更新行做并发控制。

判断 `available > 0` 和扣减在同一个原子更新中完成，不会出现两个事务都先读到库存为 1，然后都扣成功的问题。

## 注意事项

注意：

- 条件字段要有合适索引。
- 更新要短事务。
- 热点 SKU 仍可能行锁竞争。
- 要结合幂等防重复扣减。
- 订单取消要释放库存。

条件更新保证单次扣减不超卖，不解决所有生命周期问题。

## 在 eMall 项目中怎么讲？

库存服务扣减 SKU 时，用条件更新保证可售库存大于 0 才扣减。

秒杀场景还要用令牌和库存桶减少大量请求竞争同一行。

## 深度增强：事务边界图

![库存防超卖和消费幂等的事务边界](../assets/inventory-idempotency.svg)

防超卖不是只靠“先查库存再扣库存”。正确做法是把判断和扣减放在同一条 SQL 中，
并把幂等记录、库存流水和业务状态放进同一个本地事务里。

## 深度增强：Java 17 代码实现

Mapper 层要返回影响行数，业务层根据影响行数判断是否扣减成功：

```java
public interface InventoryMapper {

    int reserveStock(
            @Param("skuId") long skuId,
            @Param("quantity") int quantity,
            @Param("orderId") long orderId);
}
```

```xml
<update id="reserveStock">
    UPDATE sku_inventory
    SET available = available - #{quantity},
        reserved = reserved + #{quantity},
        updated_at = CURRENT_TIMESTAMP
    WHERE sku_id = #{skuId}
      AND available >= #{quantity}
</update>
```

服务层必须把重复请求挡住，不能让同一个订单重复预占库存：

```java
@Service
public class InventoryReservationService {

    private final InventoryMapper inventoryMapper;
    private final ReservationRepository reservationRepository;

    public InventoryReservationService(
            InventoryMapper inventoryMapper,
            ReservationRepository reservationRepository) {
        this.inventoryMapper = inventoryMapper;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public ReservationResult reserve(ReserveStockCommand command) {
        if (reservationRepository.existsByOrderId(command.orderId())) {
            return ReservationResult.duplicate(command.orderId());
        }

        int affectedRows = inventoryMapper.reserveStock(
                command.skuId(),
                command.quantity(),
                command.orderId());

        if (affectedRows == 0) {
            return ReservationResult.outOfStock(command.skuId());
        }

        reservationRepository.save(StockReservation.pendingConfirm(command));
        return ReservationResult.success(command.orderId());
    }
}
```

## 深度增强：失败场景

- SQL 条件更新防单次超卖，但不能防重复请求，所以还要幂等键。
- 热点 SKU 会产生行锁竞争，所以秒杀要用令牌、库存桶或异步排队削峰。
- 预占成功后订单取消或支付超时，要有释放库存的补偿任务。
- 库存流水要可审计，否则出现差异时无法解释库存为什么变化。

## 深度增强：面试高分表达

```text
我不会先查库存再扣库存，因为并发下会有竞争窗口。正确做法是一条条件更新 SQL：
available >= quantity 时才扣减，并根据影响行数判断是否成功。同时，同一订单的预占记录要有唯一约束，
避免重试导致重复预占。热点 SKU 再配合限流、库存桶和秒杀令牌降低行锁竞争。
```

## 专家级完整回答

```text
条件更新通过一条 SQL 同时判断库存和执行扣减，把检查和修改放进数据库原子操作中。
并发情况下只有满足条件的事务能更新成功，应用根据影响行数判断是否扣减成功。

它能防止先查后扣导致的超卖，但热点库存仍要配合限流、令牌、库存桶、幂等和库存释放补偿。
```

## 回答评分点

高分答案应该覆盖：

- 判断和扣减要在同一 SQL。
- 根据影响行数判断成功。
- 避免先查后扣竞争窗口。
- 热点行仍有锁竞争。
- 还需要幂等和释放补偿。
## 深度完善：专项验收清单

围绕「条件更新如何防止库存超卖？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。
