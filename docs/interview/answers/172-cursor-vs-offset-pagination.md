# 172 游标分页和 offset 分页如何取舍？

[返回按分类学习面试题](../README.md)

## 题目

游标分页和 offset 分页如何取舍？

## 先给面试官的短答案

`offset` 分页适合数据量小、需要跳页、对实时一致性要求不高的场景。
游标分页适合大数据量、连续向后翻页、要求性能稳定的场景。

电商核心系统中，商品流、订单列表、消息列表更适合游标分页；后台简单配置列表可以使用 `offset`。

## offset 分页

示例：

```sql
SELECT *
FROM product
ORDER BY id DESC
LIMIT 20 OFFSET 40;
```

优点：

- 实现简单。
- 支持跳到任意页。
- 前端页码组件容易适配。

缺点：

- 深分页性能差。
- 数据新增或删除后，结果可能重复或遗漏。
- 页码越深，查询成本越高。

## 游标分页

示例：

```sql
SELECT *
FROM product
WHERE id < ?
ORDER BY id DESC
LIMIT 20;
```

优点：

- 性能稳定。
- 可以利用索引范围扫描。
- 更适合无限滚动和大数据列表。
- 数据变化时更不容易出现重复。

缺点：

- 不适合任意跳页。
- 游标设计需要包含排序字段。
- 多字段排序时实现更复杂。

## 游标怎么设计？

游标应能唯一定位上一页的边界。

如果按 `created_at DESC, id DESC` 排序，游标要包含 `created_at` 和 `id`：

```sql
SELECT *
FROM orders
WHERE user_id = ?
  AND (
      created_at < ?
      OR (created_at = ? AND id < ?)
  )
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

只用时间做游标可能重复或漏数据，因为同一毫秒可能有多条记录。

## 在 eMall 项目中怎么讲？

用户订单列表可以返回 `nextCursor`：

```json
{
    "items": [],
    "nextCursor": "eyJjcmVhdGVkQXQiOiIyMDI2LTA0LTMwVDEwOjAwOjAwWiIsImlkIjoiOTkifQ=="
}
```

客户端下一次带上 `nextCursor`，服务端解析后执行范围查询。

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../assets/spring-service-stack.svg)

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
offset 分页简单并支持跳页，但页码越深性能越差，并且数据变化时容易重复或遗漏。游标分页通过上一次返回的排序边界继续查询，
可以走索引范围扫描，性能更稳定，适合订单列表、商品流、消息流这类大数据列表。

我会在小规模后台列表使用 offset，在用户高频列表使用游标。游标不能只用时间，要包含能唯一排序的字段，例如 created_at 和 id。
```

## 回答评分点

高分答案应该覆盖：

- `offset` 的优缺点。
- 游标分页的优缺点。
- 游标要包含唯一排序边界。
- 大数据连续翻页优先游标。
- 小型后台跳页场景可以用 `offset`。
