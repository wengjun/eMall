# 134 如何安全管理生产密钥？

[返回按分类学习面试题](../README.md)

## 题目

如何安全管理生产密钥？

## 先给面试官的短答案

生产密钥不能写进代码、配置文件或镜像。应使用专门的密钥管理系统或云 KMS，按最小权限发放，
通过环境变量、挂载文件或动态拉取注入，并支持加密存储、访问审计、轮换、回滚和泄漏应急。

密钥管理不是简单隐藏字符串，而是完整生命周期治理。

## 不能做什么？

禁止：

- 提交到 Git。
- 写在 `application.yml`。
- 打进 Docker 镜像。
- 写进日志。
- 放在前端代码。
- 所有人共享同一密钥。
- 长期不轮换。

这些都会导致泄漏后难以控制影响范围。

## 应该怎么做？

推荐：

- 使用 KMS 或 Secret Manager。
- 密钥加密存储。
- 应用只拿运行所需最小权限。
- 按环境隔离密钥。
- 按服务隔离密钥。
- 定期轮换。
- 记录访问审计。

密钥应像生产数据一样治理。

## 注入方式

常见方式：

- Kubernetes Secret 挂载。
- 环境变量注入密钥引用。
- 启动时从密钥系统拉取。
- Sidecar 注入。
- CSI Secret Store。

无论哪种方式，都要避免在日志和异常中打印密钥。

## 轮换设计

密钥轮换要支持：

- 双密钥并存。
- 新旧密钥灰度。
- 快速回滚。
- 下游同步。
- 过期提醒。

如果系统不支持轮换，密钥泄漏时只能停机式修复。

## 在 eMall 项目中怎么讲？

支付渠道私钥、JWT 签名密钥、数据库密码、短信供应商 token 都必须由密钥系统管理。

服务日志中要对敏感字段脱敏。

支付签名密钥轮换时，要支持新旧 key 同时验证一段时间，避免正在处理的请求失败。

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
生产密钥不能进代码、镜像、普通配置和日志。应该使用 KMS 或 Secret Manager 加密存储，
按服务和环境做最小权限授权，通过安全注入方式提供给应用，并有访问审计、定期轮换和泄漏应急。

对支付、JWT、数据库这类密钥，还要设计双 key 轮换、灰度、回滚和日志脱敏，保证安全和可用性。
```

## 回答评分点

高分答案应该覆盖：

- 密钥不能进代码和镜像。
- 使用 KMS/Secret Manager。
- 最小权限和环境隔离。
- 审计、轮换、回滚。
- 日志脱敏。
