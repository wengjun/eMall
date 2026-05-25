# 131 参数校验和业务校验如何区分？

[返回按分类学习面试题](../README.md)

## 题目

参数校验和业务校验如何区分？

## 先给面试官的短答案

参数校验判断输入形态是否合法，例如必填、长度、格式、范围和集合大小；业务校验判断当前业务状态是否允许操作，
例如库存是否足够、订单是否可取消、优惠券是否可用、用户是否有权限。参数校验通常在 Controller 边界完成，
业务校验应放在 Service 或领域层。

简单说：参数校验保证“输入像不像”，业务校验保证“事情能不能做”。

## 参数校验

参数校验关注请求本身。

例如：

- userId 不能为空。
- skuId 不能为空。
- quantity 必须大于 0。
- 手机号格式正确。
- orderLines 至少一项。

这些不需要访问复杂业务状态。

## 业务校验

业务校验关注业务规则。

例如：

- 商品是否上架。
- 库存是否足够。
- 用户是否可购买。
- 优惠券是否满足门槛。
- 订单状态是否允许取消。
- 支付金额是否和订单金额一致。

这些通常需要查数据库、缓存或调用领域服务。

## 分层原则

推荐分层：

- Controller：结构性参数校验。
- Application Service：用例级业务校验和编排。
- Domain：核心业务不变量。
- Repository：数据访问约束。
- Database：唯一键和条件更新兜底。

不要把复杂业务规则塞进 Controller。

## 错误码差异

参数校验失败通常返回参数错误。

业务校验失败通常返回明确业务错误。

例如：

- 参数错误：`quantity must be greater than zero`。
- 业务错误：`inventory not enough`。

这有利于前端提示和问题定位。

## 在 eMall 项目中怎么讲？

创建订单时，`quantity > 0` 是参数校验。

库存是否足够是业务校验。

最终扣减库存还要用数据库条件更新兜底，因为校验后到写入前可能被其他请求修改。

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
参数校验解决输入格式和结构问题，适合在 Controller 边界用 Bean Validation 完成；业务校验解决业务状态和规则问题，
应放在 Service 或领域层。库存、优惠、权限、状态流转都属于业务校验。

对并发敏感规则，业务校验之后还要有数据库唯一键、条件更新或状态机兜底，不能只靠提前检查。
```

## 回答评分点

高分答案应该覆盖：

- 参数校验是结构和格式。
- 业务校验是状态和规则。
- 分层位置不同。
- 错误码不同。
- 并发场景需要资源端兜底。
