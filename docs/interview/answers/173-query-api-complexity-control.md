# 173 查询接口如何防止过度复杂？

[返回按分类学习面试题](../README.md)

## 题目

查询接口如何防止过度复杂？

## 先给面试官的短答案

查询接口不能无限开放筛选、排序、聚合和关联，否则调用方会把在线服务当成临时数据库使用。
生产系统要限制字段、条件、排序、页大小、时间范围和查询成本，并把复杂分析类需求迁移到搜索、数仓或异步任务。

接口设计的目标不是“什么都能查”，而是“在可控成本内满足明确业务场景”。

## 复杂查询的风险

风险包括：

- 任意组合条件导致索引无法覆盖。
- 任意排序触发文件排序。
- 大时间范围导致全表扫描。
- 关联查询把多个服务耦合在一起。
- 大 `pageSize` 抢占数据库连接和内存。
- 查询语义不稳定，后续难以兼容。

越核心的交易系统，越不能暴露无限自由的查询能力。

## 约束维度

常见约束：

- 限制最大 `pageSize`。
- 限制最大时间范围。
- 限制可排序字段。
- 限制可筛选字段。
- 禁止任意模糊查询。
- 禁止跨多个大表实时关联。
- 对复杂条件做白名单。
- 对后台查询做权限和审计。

这些约束要写入接口文档和契约测试。

## 查询能力分层

可以按用途拆分：

- 交易接口只服务核心业务查询。
- 搜索接口支持关键词、类目、筛选和排序。
- 运营后台支持受控组合查询。
- 数仓和 BI 支持复杂分析。
- 导出接口使用异步任务。

不同层使用不同存储和 SLA，不要把所有查询都压到交易库。

## 在 eMall 项目中怎么讲？

订单查询接口可以支持用户、状态、短时间范围和游标分页。

如果运营要按地区、商家、活动、支付方式、退款状态做多维统计，应进入数据仓库或报表系统，
而不是在订单服务里追加一个万能查询接口。

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
查询接口要防止变成万能 SQL 网关。任意筛选、排序、聚合和关联会导致索引失效、慢查询和服务耦合。
我会限制 pageSize、时间范围、排序字段和筛选字段，对复杂条件做白名单。

核心交易查询走受控接口，搜索走搜索引擎，分析走数仓，导出走异步任务。这样可以把能力和成本放到正确的系统层。
```

## 回答评分点

高分答案应该覆盖：

- 万能查询接口会拖垮数据库。
- 需要限制条件、排序、页大小和时间范围。
- 复杂查询要分流到搜索、数仓或异步任务。
- 接口约束要文档化。
- 核心交易库不能承载分析查询。
