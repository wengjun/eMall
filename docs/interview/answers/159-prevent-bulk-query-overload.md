# 159 如何防止批量查询拖垮系统？

[返回按分类学习面试题](../README.md)

## 题目

如何防止批量查询拖垮系统？

## 先给面试官的短答案

防止批量查询拖垮系统要限制 page size、限制查询时间范围、限制导出规模、使用异步任务、限流、超时、索引优化、
游标分页和资源隔离。大批量导出不要走在线查询接口，应进入后台任务和文件生成流程。

核心原则是让在线接口只服务低延迟查询，大任务走异步通道。

## 限制输入

必须限制：

- page size。
- 最大页数。
- 时间范围。
- 查询条件数量。
- ID 列表长度。
- 排序字段。

不要允许用户无限制查询全表。

## 索引和查询计划

批量查询要确保：

- where 条件命中索引。
- order by 命中合适索引。
- 避免函数导致索引失效。
- 避免大 offset。
- 避免返回过多列。

接口层限制和数据库索引要一起做。

## 异步导出

大导出应该：

- 创建导出任务。
- 后台分页扫描。
- 生成文件。
- 上传对象存储。
- 通知用户下载。

不要在 HTTP 请求里同步导出几十万行。

## 资源隔离

后台批量任务要和在线交易隔离：

- 独立线程池。
- 独立实例。
- 独立数据库只读副本。
- 限速。
- 低优先级队列。

避免后台任务影响下单和支付。

## 在 eMall 项目中怎么讲？

订单后台查询最多允许 100 条一页，时间范围最多 31 天。

超过范围的导出进入异步任务。

导出任务读取只读库，限速扫描，不占用订单创建主库连接池。

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
防止批量查询拖垮系统，要从入口限制 page size、时间范围、ID 数量和排序字段，从数据库保证索引和执行计划，
从架构上把大导出改成异步任务。后台任务还要和在线交易做线程池、实例、数据库连接和限速隔离。

在线接口追求低延迟，大批量处理走异步和离线通道，不能混在同一资源池里。
```

## 回答评分点

高分答案应该覆盖：

- 限制 page size 和时间范围。
- 索引和执行计划。
- 大导出异步化。
- 资源隔离。
- 在线接口和后台任务分离。
