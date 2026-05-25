# 155 什么时候需要新版本 API？

[返回按分类学习面试题](../README.md)

## 题目

什么时候需要新版本 API？

## 先给面试官的短答案

当变更会破坏已有客户端兼容性时，需要新版本 API。例如删除字段、修改字段类型、改变字段单位、
改变业务语义、改变错误码语义、改变必填规则或改变幂等语义。新增可选字段通常不需要新版本。

判断标准是老客户端不改代码是否还能正确运行。

## 需要新版本的变更

典型破坏性变更：

- 删除响应字段。
- 字段类型从 string 改 number。
- 金额单位从元改分。
- 枚举值语义变化。
- 必填字段新增。
- 错误码含义变化。
- 分页语义变化。
- 幂等规则变化。

这些都会影响老客户端。

## 不一定需要新版本的变更

通常兼容：

- 响应新增可选字段。
- 新增可选请求参数。
- 新增接口。
- 新增错误码且老客户端有默认处理。
- 性能优化。

但要确保客户端使用宽松解析，不因未知字段失败。

## 灰度迁移

新版本上线要支持：

- v1 和 v2 并存。
- 客户端灰度切换。
- 监控调用量。
- 文档和 SDK 更新。
- 老版本下线计划。

不能直接替换导致老客户端失败。

## 在 eMall 项目中怎么讲？

订单金额字段从 `amountYuan` 改成 `amountCents` 是破坏性变更。

可以先新增 `amountCents`，保留 `amountYuan`，客户端迁移后再废弃老字段。

如果直接改原字段含义，老客户端会展示错误金额。

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
需要新 API 版本的标准是是否破坏老客户端兼容。删除字段、改类型、改单位、改业务语义、改必填规则、
改错误码和幂等语义都应新版本或兼容迁移。新增可选字段一般不需要。

我会优先做兼容演进：新增字段、双写双读、灰度迁移、监控老版本调用量，最后再按计划下线。
```

## 回答评分点

高分答案应该覆盖：

- 判断标准是老客户端兼容。
- 删除、改类型、改语义需要新版本。
- 新增可选字段通常兼容。
- 支持灰度和下线计划。
- 能举金额单位例子。
