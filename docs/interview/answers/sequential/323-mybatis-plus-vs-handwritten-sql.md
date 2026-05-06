# 323 MyBatis Plus 和手写 SQL 如何取舍？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

MyBatis Plus 和手写 SQL 如何取舍？

## 先给面试官的短答案

MyBatis Plus 适合标准 CRUD、简单条件查询、审计字段填充和通用分页。手写 SQL 适合复杂查询、
强性能要求、明确索引设计、批处理、状态机更新和需要控制锁范围的场景。

原则是：非核心简单路径用 MyBatis Plus 提升效率，核心高并发路径用手写 SQL 保证可控。

## 适合 MyBatis Plus 的场景

适合：

- 用户资料查询。
- 地址管理。
- 配置表维护。
- 后台简单分页。
- 字典数据管理。
- 单表普通 CRUD。

这些场景的 SQL 简单，性能风险相对低。

## 适合手写 SQL 的场景

适合：

- 库存条件扣减。
- 订单状态机流转。
- 支付单唯一约束写入。
- Outbox 批量扫描。
- 慢查询优化后的固定 SQL。
- 多表复杂查询。
- 需要使用特定索引的查询。

这些场景需要明确 SQL 形态和执行计划。

## 判断标准

可以按以下问题判断：

- SQL 是否在核心链路。
- QPS 是否高。
- 是否涉及锁竞争。
- 是否必须命中特定索引。
- 是否需要条件更新保证幂等。
- 是否需要批量处理。
- 是否方便做 SQL 审计。

越核心、越高并发、越复杂，就越应该手写和评审。

## 在 eMall 项目中怎么讲？

eMall 的 `user` 模块可以大量使用 MyBatis Plus 处理资料维护。

`inventory` 的扣库存必须手写：

```sql
UPDATE sku_inventory
SET available = available - #{quantity}
WHERE sku_id = #{skuId}
  AND available >= #{quantity}
```

这个 SQL 的条件更新是防超卖的核心，不能交给通用 CRUD 隐式生成。

## 深度增强：索引和 SQL 可控性图

![索引设计从访问路径出发](../../assets/index-design.svg)

MyBatis Plus 的价值是提升简单 CRUD 效率；手写 SQL 的价值是让核心链路的 SQL 形态、索引、锁范围和执行计划可控。
高分回答不是否定 MyBatis Plus，而是说明哪里可以用，哪里必须手写。

## 深度增强：Java 17 分层示例

普通配置表可以使用 MyBatis Plus：

```java
@Service
public class AddressBookService {

    private final UserAddressMapper userAddressMapper;

    public List<UserAddressEntity> listUserAddresses(long userId) {
        return userAddressMapper.selectList(
                Wrappers.<UserAddressEntity>lambdaQuery()
                        .eq(UserAddressEntity::getUserId, userId)
                        .eq(UserAddressEntity::getDeleted, false)
                        .orderByDesc(UserAddressEntity::getUpdatedAt));
    }
}
```

库存扣减、订单状态机和 Outbox 扫描要手写：

```java
public interface OrderStateMapper {

    int markPaid(
            @Param("orderId") long orderId,
            @Param("paymentId") long paymentId,
            @Param("paidAt") Instant paidAt);
}
```

```xml
<update id="markPaid">
    UPDATE orders
    SET status = 'PAID',
        payment_id = #{paymentId},
        paid_at = #{paidAt}
    WHERE id = #{orderId}
      AND status = 'PENDING_PAYMENT'
</update>
```

这类 SQL 的条件就是业务状态机约束，不能丢给通用更新。

## 深度增强：取舍标准

- 简单单表 CRUD、后台维护、字典表、地址表：优先 MyBatis Plus。
- 高 QPS 核心链路、库存扣减、支付确认、订单状态机：优先手写 SQL。
- 需要命中特定索引、控制锁范围、控制批次大小：优先手写 SQL。
- 复杂报表和分析：不要强行用 ORM 拼，走数仓或专门查询模型。
- 所有核心 SQL 都要做执行计划评审和慢 SQL 监控。

## 深度增强：面试高分表达

```text
我会把 MyBatis Plus 当作提升工程效率的工具，而不是所有 SQL 的替代品。
用户地址、配置表这类简单 CRUD 可以用；库存扣减、订单状态流转、Outbox 扫描这类核心链路必须手写，
因为我要控制 where 条件、索引命中、锁范围和影响行数。核心原则是非核心路径提效，核心路径可控。
```

## 专家级完整回答

```text
MyBatis Plus 和手写 SQL 不是替代关系，而是分层取舍。简单单表 CRUD 用 MyBatis Plus 可以减少
重复代码，核心交易链路用手写 SQL 可以控制索引、锁范围、条件更新和执行计划。

我的原则是非核心路径追求效率，核心路径追求可控。上线前还要把关键 SQL 纳入慢 SQL 监控和
执行计划评审。
```

## 回答评分点

高分答案应该覆盖：

- 不把 MyBatis Plus 和手写 SQL 对立。
- 简单 CRUD 用 MyBatis Plus。
- 核心链路手写 SQL。
- 说明库存扣减、订单状态机等场景。
- 强调 SQL 可控和可观测。
## 深度完善：专项验收清单

围绕「MyBatis Plus 和手写 SQL 如何取舍？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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
