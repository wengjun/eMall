# 124 事务传播行为有哪些？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

事务传播行为有哪些？

## 先给面试官的短答案

事务传播行为定义一个事务方法被另一个事务方法调用时，应该加入现有事务、创建新事务、挂起事务还是非事务执行。
Spring 常见传播行为包括 `REQUIRED`、`REQUIRES_NEW`、`NESTED`、`SUPPORTS`、`NOT_SUPPORTED`、
`MANDATORY` 和 `NEVER`。

最常用的是 `REQUIRED`，最容易误用的是 `REQUIRES_NEW` 和 `NESTED`。

## REQUIRED

默认传播行为。

如果当前有事务，就加入当前事务。

如果没有事务，就新建事务。

适合大多数业务写操作。

## REQUIRES_NEW

总是新建一个事务。

如果当前已有事务，会挂起当前事务。

适合：

- 独立审计日志。
- 独立操作记录。
- 主事务失败也希望保留的记录。

但它会额外占用数据库连接，不能滥用。

## NESTED

在已有事务中创建嵌套事务，通常依赖 savepoint。

内部事务回滚可以回到保存点，不一定回滚整个外部事务。

前提是事务管理器和数据库支持保存点。

## SUPPORTS

如果有事务就加入。

如果没有事务就非事务执行。

适合查询方法，但实际项目中查询是否需要只读事务要按一致性需求判断。

## NOT_SUPPORTED

总是非事务执行。

如果当前有事务，会挂起当前事务。

适合不希望被长事务包住的非事务操作。

## MANDATORY

必须在已有事务中运行。

如果当前没有事务，抛异常。

适合强制某些内部方法只能在事务上下文中调用。

## NEVER

必须在非事务中运行。

如果当前有事务，抛异常。

使用较少。

## 在 eMall 项目中怎么讲？

订单创建主流程通常使用 `REQUIRED`。

审计日志如果希望主事务回滚后仍保留，可以使用 `REQUIRES_NEW`，但要注意连接池压力。

不要用 `REQUIRES_NEW` 包远程调用，也不要用传播行为掩盖事务边界混乱。

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../../assets/spring-service-stack.svg)

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
事务传播行为定义事务方法相互调用时如何处理事务上下文。REQUIRED 是默认，有事务加入、无事务创建；
REQUIRES_NEW 总是创建新事务并挂起外部事务；NESTED 使用保存点做嵌套回滚；SUPPORTS 有事务就加入；
NOT_SUPPORTED 挂起事务非事务执行；MANDATORY 要求必须有事务；NEVER 要求不能有事务。

生产中我最关注传播行为是否符合业务边界，以及 REQUIRES_NEW 是否造成额外连接和一致性风险。
```

## 回答评分点

高分答案应该覆盖：

- 能说出七种传播行为。
- `REQUIRED` 是默认。
- `REQUIRES_NEW` 挂起外部事务。
- `NESTED` 依赖 savepoint。
- 能联系连接池和业务边界。

## 二次深度补强

题目：事务传播行为有哪些？

二次补强标记：已完成

### 面试官真正想确认的能力

服务接口题要覆盖契约、参数校验、超时、错误码、降级和兼容性。
围绕这道题，要进一步把概念、项目实现、线上风险和验证闭环连起来。

### 深度和广度补充

- 先说清接口契约：请求字段、响应字段、错误码、幂等键和版本。
- 再说明服务层如何承接业务规则，而不是把逻辑堆在 Controller。
- 随后补齐 HTTP 客户端治理：连接池、超时、重试、熔断和限流。
- 最后说明如何用集成测试和契约测试保护 API 演进。

### 图片讲解

![二次补强图解](../../assets/spring-service-stack.svg)

- 图中把 Controller、Application Service、Domain、Client 和 Repository 分层。
- 读图时要说明每层职责，避免接口层污染领域模型。
- 面试官通常关心你能否维护长期演进的服务边界。

### Java17 HTTP 调用边界示例

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public record DownstreamEndpoint(URI uri, Duration timeout) {
}

final class DownstreamCaller {
    private final HttpClient client = HttpClient.newHttpClient();

    HttpResponse<String> get(DownstreamEndpoint endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint.uri())
                .timeout(endpoint.timeout())
                .header("Accept", "application/json")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
```

### 高分表达要点

- 不要只回答定义，要说明为什么这样设计、在什么条件下失效、如何监控和回滚。
- 把答案和当前电商项目联系起来，例如订单、库存、支付、履约、搜索、风控或发布链路。
- 主动给出边界条件和反例，能让面试官看到你具备生产系统判断力。

## 逐题专项补强

逐题专项补强标记：已完成

### 本题专项切入

- 本题要围绕「事务传播行为有哪些？」展开，不要只复述分类模板。
- 先明确 API 契约、错误码、参数校验、版本兼容和幂等语义。
- 再说明 Controller、Service、Client 和 Repository 的职责边界。

### 专项图解说明

![逐题专项图解](../../assets/outbox-flow.svg)

- 这张图用于把「事务传播行为有哪些？」放回生产链路中理解，重点看入口、状态、数据和恢复闭环。
- 面试时可以先按图说明主路径，再补失败路径、监控指标和回滚手段。

### 贴合本题的实现示例

```java
import java.util.List;

interface OutboxRepository {
    List<OutboxMessage> lockPending(int limit);

    void markSent(long id);
}

record OutboxMessage(long id, String topic, String payload) {
}

final class OutboxRelay {

    void relay(OutboxRepository repository, MessagePublisher publisher) {
        for (OutboxMessage message : repository.lockPending(100)) {
            publisher.publish(message.topic(), message.payload());
            repository.markSent(message.id());
        }
    }
}

interface MessagePublisher {
    void publish(String topic, String payload);
}
```

### 进一步追问时的回答边界

- 如果面试官继续追问，要主动说明这个实现是核心模型，不等于完整生产组件。
- 生产级落地还需要接入鉴权、幂等、限流、熔断、监控、告警、灰度和数据修复。
- 回答时把复杂度、失败场景、验证方式和 eMall 项目中的落地位置一起说清楚。

## 面试实战补强

面试实战补强标记：已完成

### 面试追问路线

- 接口契约如何做版本兼容，哪些字段能加，哪些字段不能随便改？
- 下游超时、连接池耗尽、错误码漂移时，上游应该怎么保护自己？
- Controller、应用服务、领域服务和客户端适配层如何分工？

### eMall 项目落点

- 可以落到模块：gateway、openapi、user、order。
- 回答「事务传播行为有哪些？」时，要从这些模块里选一个主链路做例子。
- 讲清入口、状态变化、数据写入、异步事件、失败补偿和观测指标。

### 生产验证指标

- 接口错误率
- 下游超时率
- 连接池等待时间
- 契约测试通过率

### 低分陷阱

- 只背定义，不说明业务场景和失败场景。
- 只讲正常路径，不讲超时、重试、回滚、补偿和监控。
- 只给方案，不给验证指标和取舍边界。

### 30 秒高分收束

这道题我会用 Spring Boot、API、HTTP 的视角回答。
先给结论，再给项目例子，然后补失败场景、验证指标和取舍边界。
这样能让面试官看到我不是只会背知识点，而是能把知识点落到生产系统。

## 架构取舍与反驳补强

架构取舍补强标记：已完成

### 先给立场

- 回答「事务传播行为有哪些？」时，不能只给单一方案，要先说明约束、目标和失败边界。
- 高分回答要让面试官看到你能在正确性、可用性、成本、复杂度和团队能力之间做判断。

### 可选方案对比

- 同步 HTTP：语义直接、实现简单，但容易被下游延迟拖垮。
- 异步事件：解耦能力强，但一致性和排障复杂度更高。
- 聚合网关：前端体验好，但网关容易变厚并成为瓶颈。

### 反驳和防守

- 如果面试官问为什么不直接上最复杂方案，可以回答：复杂方案只有在规模和风险证明必要时才值得引入。
- 如果面试官问为什么不用最简单方案，可以回答：简单方案可以做第一期，但必须提前设计观测和迁移边界。
- 我的判断原则是：如果约束不明确，先补齐规模、延迟、可用性、一致性、成本和团队能力，再做选择。

### 决策证据

- 业务指标
- 稳定性指标
- 成本指标
- 灰度和回滚记录

### 一句话总结

我会先用简单可靠的方案解决当前确定性问题，同时保留观测、灰度和迁移能力。
当指标证明瓶颈存在，再演进到更复杂的架构，而不是为了显得高级提前复杂化。

## 生产落地验收补强

生产验收补强标记：已完成

### 上线前检查

- 针对「事务传播行为有哪些？」，先确认它影响的是正确性、稳定性、性能、安全还是成本。
- 确认 API 契约、错误码、超时、重试、熔断和版本兼容。
- 灰度期间观察接口错误率、下游超时率和连接池等待。

### 灰度和回滚

- 先在测试环境和影子流量中验证，再做 1%、5%、25%、50%、100% 分阶段灰度。
- 每个阶段都设置自动暂停条件和人工回滚负责人。
- 回滚不是只回代码，还要确认配置、数据、缓存、消息和任务状态能一起回到安全状态。

### 监控和验收证据

- 测试报告
- 灰度看板
- 告警规则
- 回滚记录

### 面试表达

我不会只说方案能实现，还会说明上线前怎么验收、上线中怎么看指标、出问题怎么回滚。
这能证明我关注的是长期稳定运行，而不是只完成一次功能开发。

## 规模化与成本治理补强

规模成本补强标记：已完成

### 规模化视角

- 回答「事务传播行为有哪些？」时，要主动放到 10 亿用户、1 亿 DAU、100W 峰值并发的背景下思考。
- 按接口 QPS、连接池、超时和下游容量估算 API 承载能力。
- 同步接口越多，越要关注雪崩传播和成本放大。

### 成本治理

- 用单位成本看问题，例如单请求成本、单订单成本、单消息成本和单 GB 存储成本。
- 先优化浪费最高的环节，而不是平均用力。

### 自动化和 owner

- 为关键指标建立看板、告警、owner 和 Runbook。
- 把经验沉淀成自动化检查、流水线门禁或平台能力。

### 面试表达

我会补一句：方案能跑只是第一步，大规模下还要回答容量怎么估、成本怎么控、故障谁负责。
这能体现我不是只会实现单点功能，而是能长期运营一个高并发业务系统。

