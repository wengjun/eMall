# 171 深分页有什么问题？

[返回按分类学习面试题](../README.md)

## 题目

深分页有什么问题？

## 先给面试官的短答案

深分页的问题不是“页码大”本身，而是数据库为了返回很靠后的少量数据，可能要扫描、排序、丢弃前面大量记录。
在高并发下，深分页会放大 CPU、内存、磁盘 IO 和索引回表成本，拖慢主库和核心查询链路。

生产系统应限制最大页数，并对大结果集使用游标分页、搜索引擎、异步导出或离线报表。

## 为什么 offset 会变慢？

常见 SQL：

```sql
SELECT *
FROM orders
WHERE user_id = ?
ORDER BY created_at DESC
LIMIT 20 OFFSET 100000;
```

数据库通常需要先找到前 `100020` 条符合条件的数据，再丢弃前 `100000` 条，只返回最后 `20` 条。

如果排序字段和过滤条件没有合适联合索引，数据库还可能执行大范围扫描和文件排序。

## 深分页的生产风险

风险包括：

- 慢查询增多，拖慢连接池。
- 大量回表导致磁盘 IO 上升。
- 排序使用临时表，消耗内存和磁盘。
- 主库 CPU 被查询打满，影响写入。
- 用户反复翻页导致请求放大。
- 数据变化时，页码结果可能重复或遗漏。

这些风险在电商订单、商品、搜索、日志查询中都很常见。

## 解决方式

常见方案：

- 限制最大页码和最大 `pageSize`。
- 使用基于游标的分页。
- 为筛选条件和排序字段建立联合索引。
- 对搜索类查询使用 Elasticsearch 等搜索引擎。
- 对导出类查询改为异步任务。
- 对后台运营查询使用只读库或分析库。
- 对热门列表做缓存或预计算。

核心原则是：在线接口不能让用户用翻页请求触发无限扫描。

## 在 eMall 项目中怎么讲？

商品列表可以允许有限页码浏览，但不能允许用户翻到第十万页。

订单后台如果要导出三个月订单，应提交导出任务，由任务系统分批读取数据并生成文件，
而不是让 HTTP 接口同步执行深分页查询。

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
深分页的本质问题是 offset 会让数据库扫描并丢弃大量数据。页码越深，扫描、排序、回表和 IO 成本越高。
在高并发系统里，少量深分页请求就可能占满数据库连接和 CPU，影响核心写入链路。

我会限制最大页码，用游标分页替代 offset 深分页，为查询和排序字段设计联合索引。搜索类查询交给搜索引擎，
导出类需求改成异步任务，后台分析走只读库或数仓，避免在线交易库被大查询拖垮。
```

## 回答评分点

高分答案应该覆盖：

- 深分页慢的根因是扫描和丢弃大量数据。
- 排序、回表和临时表会进一步放大成本。
- 数据变化可能导致重复和遗漏。
- 在线接口要限制最大页码。
- 游标分页、搜索引擎、异步导出是常见替代方案。
