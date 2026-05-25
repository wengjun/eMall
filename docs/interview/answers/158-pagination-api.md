# 158 分页接口如何设计？

[返回按分类学习面试题](../README.md)

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
