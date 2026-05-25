# 174 批量接口如何设计部分成功？

[返回按分类学习面试题](../README.md)

## 题目

批量接口如何设计部分成功？

## 先给面试官的短答案

批量接口不能只返回一个总成功或总失败，因为生产环境中经常出现部分参数错误、部分资源不存在、部分冲突。
合理设计是为每个元素返回独立结果，并提供批次号、幂等键、错误码和可重试标记。

是否允许部分成功，要由业务语义决定；不能为了接口简单而破坏一致性。

## 响应结构

示例：

```json
{
    "batchId": "batch-20260430-001",
    "successCount": 2,
    "failureCount": 1,
    "items": [
        {
            "clientItemId": "1",
            "resourceId": "sku-1",
            "success": true
        },
        {
            "clientItemId": "2",
            "success": false,
            "errorCode": "SKU_NOT_FOUND",
            "retryable": false
        }
    ]
}
```

调用方可以根据每一项的结果决定是否重试。

## 设计要点

关键点包括：

- 请求项必须有客户端侧唯一标识。
- 响应项要和请求项可对应。
- 每一项有独立错误码。
- 标明失败是否可重试。
- 限制单批次最大数量。
- 支持批次级幂等。
- 记录审计日志和处理明细。

批量接口必须防止调用方一次提交过大请求。

## 事务边界

有两种常见语义：

- 全部成功或全部失败。
- 允许部分成功。

库存扣减、支付扣款这类强一致操作通常不适合随意部分成功。

商品批量上下架、批量修改标签、批量导入配置等场景更适合部分成功。

## 在 eMall 项目中怎么讲？

商家批量修改商品标签可以部分成功，因为每个商品之间相对独立。

但用户下单扣减多个商品库存时，不能简单返回“部分扣减成功”，否则订单状态会混乱。
这种场景要使用库存预占、状态机和补偿机制。

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
批量接口要先明确业务是否允许部分成功。如果允许，响应必须返回每个请求项的处理结果、错误码和可重试标记，
并通过 clientItemId 保证调用方能对应原始请求。批量接口还要限制单批次大小，支持幂等和审计。

如果业务要求原子性，例如支付或订单核心链路，就不应该随意部分成功，而要用事务、状态机或补偿方案保证一致性。
```

## 回答评分点

高分答案应该覆盖：

- 部分成功取决于业务语义。
- 响应要包含每个元素的结果。
- 请求项需要客户端唯一标识。
- 要有错误码和可重试标记。
- 批量大小、幂等和审计不可缺少。
