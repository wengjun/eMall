# 162 如何设计开放 API 签名？

[返回按分类学习面试题](../README.md)

## 题目

如何设计开放 API 签名？

## 先给面试官的短答案

开放 API 签名通常使用 appId 标识调用方，使用 appSecret 或私钥生成签名。签名内容应包含 HTTP 方法、
路径、查询参数、请求体摘要、时间戳、nonce 和版本，并按固定规则 canonicalize 后计算 HMAC 或非对称签名。
服务端验签、校验时间窗口和 nonce，防止篡改和重放。

签名规则必须稳定、明确、可测试。

## 签名要解决什么？

主要解决：

- 调用方身份识别。
- 请求未被篡改。
- 防止重放攻击。
- 访问权限控制。
- 审计追踪。

签名不是加密，请求内容如果敏感还需要 HTTPS 和字段级保护。

## 参与签名的字段

通常包括：

- appId。
- timestamp。
- nonce。
- HTTP method。
- path。
- query parameters。
- body hash。
- signature version。

请求体不一定直接参与签名，可以用 body hash。

## canonicalization

签名前要规范化：

- 参数按字典序排序。
- URL 编码规则一致。
- 空值处理一致。
- JSON 序列化规则明确。
- 大小写规则明确。

很多签名失败来自规范化不一致。

## 签名算法

常见：

- HMAC-SHA256。
- RSA-SHA256。
- ECDSA。

内部合作方可用 HMAC。

开放平台更适合非对称签名，私钥由调用方保存，平台保存公钥。

## 在 eMall 项目中怎么讲？

商家开放 API 请求带：

```text
X-App-Id
X-Timestamp
X-Nonce
X-Signature
X-Signature-Version
```

服务端校验签名、时间窗口、nonce 去重和接口权限，再处理业务。

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
开放 API 签名要把身份、请求内容和时间信息绑定起来。签名串通常包含 method、path、query、body hash、
timestamp、nonce、appId 和版本，按固定 canonicalization 规则排序编码后，用 HMAC 或非对称算法签名。

服务端验签后还要校验时间窗口、nonce 是否使用过、app 权限和限流。签名规则要稳定，并提供 SDK 和测试用例。
```

## 回答评分点

高分答案应该覆盖：

- appId/appSecret 或公私钥。
- method/path/query/body hash。
- timestamp 和 nonce。
- 参数规范化。
- 验签后还要权限和限流。
