# 158 分页接口如何设计？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

分页接口如何设计？

## 先给面试官的短答案

分页接口要明确分页方式、排序规则、最大 page size、过滤条件、返回游标或总数策略。常见方式有 offset 分页和 cursor 分页。
offset 简单但深分页性能差，cursor 适合大数据和实时列表。无论哪种，都必须有稳定排序和大小限制。

分页设计目标是可用、稳定、不会拖垮数据库。

## offset 分页

示例：

```text
GET /orders?page=1&pageSize=20
```

优点：

- 简单。
- 前端容易跳页。
- 适合小数据集。

缺点：

- 深分页性能差。
- 数据变化时可能重复或漏数据。

## cursor 分页

示例：

```text
GET /orders?cursor=xxx&pageSize=20
```

优点：

- 适合大数据集。
- 深分页性能更好。
- 更适合时间线列表。

缺点：

- 不方便任意跳页。
- 实现复杂。

## 排序规则

分页必须有稳定排序。

例如：

```text
order by created_at desc, order_id desc
```

只按时间排序可能因时间相同导致顺序不稳定。

要加唯一字段兜底。

## page size 限制

必须限制最大 page size。

例如最大 100 或 200。

防止客户端一次请求 100000 条拖垮数据库和服务内存。

## total count

是否返回总数要谨慎。

大表 `count(*)` 可能很慢。

可以选择：

- 小数据返回 total。
- 大数据不返回 total。
- 异步统计。
- 近似统计。

## 在 eMall 项目中怎么讲？

用户订单列表可以用 cursor 分页，按创建时间和订单 ID 排序。

后台管理查询可以用 offset 分页，但要限制最大页数和 page size。

订单导出不应走普通分页接口，应使用异步导出任务。

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../../assets/spring-service-stack.svg)

Spring 题要从框架机制讲到业务边界。Controller 负责协议适配，Service 负责业务事务，Repository 负责数据访问；
事务、AOP、校验、错误码、配置和观测都是为了让微服务在复杂调用中保持稳定。

## 深度增强：Java 17 分层示例

```java
record CreateOrderCommand(long userId, long skuId, int quantity) {
}

record CreateOrderResult(long orderId, String status) {
}

interface OrderApplicationService {
    CreateOrderResult create(CreateOrderCommand command);
}

final class OrderControllerAdapter {
    private final OrderApplicationService service;

    OrderControllerAdapter(OrderApplicationService service) {
        this.service = service;
    }

    CreateOrderResult submit(CreateOrderCommand command) {
        return service.create(command);
    }
}
```

这个示例表达分层边界：接口层不堆业务逻辑，业务层不依赖 Web 协议，命令和结果对象形成稳定契约。

## 深度增强：生产边界

框架默认值不能替代设计。事务传播、异常回滚、异步线程池、连接池、序列化、超时和重试都要显式治理。
尤其在订单、支付、库存链路中，要避免长事务、隐式重试和跨服务事务误用。

## 深度增强：面试高分表达

我会先解释框架原理，再说明在电商系统里怎么落地。高分回答要能把自动配置、AOP、事务、MVC、WebFlux、
校验和错误处理，连接到可维护性、可观测性、稳定性和故障恢复。

## 专家级完整回答

```text
分页接口要设计分页方式、稳定排序、page size 上限、过滤条件和 total 策略。offset 简单但深分页差，
cursor 更适合大数据和实时列表。排序必须稳定，比如 created_at 加 order_id。

我会限制最大 page size，避免大查询拖垮数据库；大表 total count 要谨慎，必要时异步或近似统计。
```

## 回答评分点

高分答案应该覆盖：

- offset 和 cursor 区别。
- 稳定排序。
- page size 上限。
- 深分页风险。
- total count 成本。

## 深度完善：面向 L6 的回答框架

围绕「分页接口如何设计？」，高分答案不能停在概念定义，而要把「REST 契约、幂等、版本、错误码、签名、安全和客户端兼容」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「API 设计和网关治理」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `gateway、openapi、identity、risk、order 的外部 API 和内部服务 API` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：接口错误率、幂等冲突率、签名失败率、限流命中率、兼容性测试结果。
- 验证证据：OpenAPI 文档、契约测试、审计日志、网关指标、错误码看板和灰度记录。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：分页接口如何设计？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

