# 514 设计一个京东/Amazon 类电商系统

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

设计一个京东/Amazon 类电商系统。

## 先给面试官的短答案

我会先明确规模和核心目标：10 亿用户、日活 1 亿、峰值并发 100 万，核心诉求是交易正确、系统高可用、
可水平扩展、可观测、可灰度和可恢复。整体架构按用户、商品、搜索、购物车、价格、促销、库存、订单、支付、
履约、售后、风控、推荐、广告、开放平台和数据平台拆分，通过网关、消息、缓存、分库分表和治理平台支撑。

## 需求拆解

功能需求包括用户登录、商品浏览、搜索、购物车、价格计算、优惠、库存预占、下单、支付、履约、售后退款、
评价、推荐、广告、商家管理和开放 API。

非功能需求包括高并发、低延迟、高可用、数据一致性、资金安全、库存正确、可扩展、可审计、可观测和合规。

核心链路优先级最高：商品详情、下单、库存、支付和订单查询。非核心链路如推荐、评价、广告可以降级。

## 总体架构

入口层包括 CDN、WAF、负载均衡、API Gateway 和 BFF。网关负责认证、限流、签名、灰度、trace 和路由。

业务层按领域拆分。用户中心负责账号和身份；商品中心负责 SPU、SKU 和类目；搜索负责索引和查询；
购物车负责用户临时购买意图；价格和促销负责金额计算；库存负责可售、预占和释放；订单负责交易状态；
支付负责支付单、渠道和回调；履约负责发货；售后负责退款退货。

数据层包括 MySQL 分库分表、Redis 缓存、Kafka 事件、Elasticsearch 搜索、对象存储、数据仓库和实时指标。

治理层包括配置中心、发布平台、限流熔断、降级、补偿平台、可观测性、审计和成本治理。

## 核心数据流

浏览链路：用户请求商品详情，网关鉴权或匿名放行，商品服务读取缓存，价格和促销返回实时价格，库存返回可售提示，
推荐和评价可异步或降级。

下单链路：订单服务校验用户、商品、价格和促销，调用库存预占，创建订单和支付单，写 Outbox 事件，返回支付参数。

支付链路：支付回调进入支付服务，验签和金额校验后更新支付单，通过事件驱动订单变更已支付，再触发履约。

履约和售后链路通过事件解耦，使用幂等、对账和补偿保证最终一致。

## 一致性设计

订单、库存和支付不能依赖单个大分布式事务。核心原则是本地事务加 Outbox、消费者幂等、状态机约束、补偿任务和对账。

库存预占要保证不超卖，支付回调要保证不重复入账，订单状态要防止非法回退。资金链路通过渠道对账和本地对账兜底。

## 高可用和扩展

读多写少的链路使用多级缓存、CDN、热点保护和异步刷新。写链路通过分库分表、消息削峰、连接池隔离和热点 SKU 保护扩展。

服务通过 Kubernetes 多可用区部署，配合 readiness、PDB、HPA、限流、熔断、降级、灰度和快速回滚。

## 在 eMall 项目中怎么讲？

eMall 已经按这些领域拆分出多个模块。面试时可以把它讲成一个可演进的生产级骨架：
先保证交易闭环正确，再补齐风控、开放平台、数据平台、运维治理、测试和可观测性。

## 深度增强：架构图

![京东/Amazon 类电商系统高层架构](../../assets/ecommerce-architecture.svg)

这张图可以按四层讲：

- 入口层：CDN、WAF、网关、BFF、认证、限流、灰度和 trace。
- 业务服务层：商品、搜索、购物车、价格、促销、订单、库存、支付、履约、售后、推荐、广告和风控。
- 数据和消息层：MySQL 分片、Redis、Kafka、OpenSearch 和数据仓库。
- 治理和可靠性：可观测、熔断降级、灰度发布、补偿对账、审计安全。

面试时不要把图讲成“模块罗列”。要讲清楚每层为什么存在，以及核心链路和非核心链路如何隔离。

## 深度增强：下单链路代码骨架

系统设计题不一定要求完整代码，但给出一个 Java 17 编排骨架，会让答案更像真实工程。
下面代码表达的是：交易链路不做全局大事务，而是用本地状态、库存预占、支付单和 Outbox 事件组合保证可恢复。

```java
public record CheckoutCommand(
        UserId userId,
        List<OrderLineCommand> lines,
        CouponId couponId,
        String idempotencyKey) {
}

public record CheckoutResult(OrderId orderId, PaymentId paymentId, Money payableAmount) {
}

@Service
public class CheckoutApplicationService {

    private final PricingClient pricingClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;

    public CheckoutApplicationService(
            PricingClient pricingClient,
            InventoryClient inventoryClient,
            PaymentClient paymentClient,
            OrderRepository orderRepository,
            OutboxRepository outboxRepository) {
        this.pricingClient = pricingClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public CheckoutResult checkout(CheckoutCommand command) {
        OrderId orderId = OrderId.newId();
        PriceQuote quote = pricingClient.quote(command.userId(), command.lines(), command.couponId());

        ReservationId reservationId = inventoryClient.reserve(orderId, command.lines());
        PaymentDraft paymentDraft = paymentClient.createDraft(orderId, quote.payableAmount());

        Order order = Order.pendingPayment(
                orderId,
                command.userId(),
                quote,
                reservationId,
                paymentDraft.paymentId());

        orderRepository.save(order);
        outboxRepository.save(OrderEvents.orderCreated(order));

        return new CheckoutResult(orderId, paymentDraft.paymentId(), quote.payableAmount());
    }
}
```

这段代码面试时要主动补充风险：

- `pricingClient.quote` 需要价格快照，不能支付时重新算导致金额变化。
- `inventoryClient.reserve` 必须幂等，重复下单或超时重试不能重复预占。
- `paymentClient.createDraft` 不能真正扣款，只创建支付意图或支付单。
- 本地事务只保护订单落库和 outbox 事件，不保护远程服务。
- 远程调用成功但本地事务失败时，需要库存释放、支付单关闭或补偿任务兜底。

## 深度增强：系统设计回答主线

高分回答建议按下面主线展开：

```text
第一步先澄清目标：用户规模、峰值并发、核心链路、正确性要求和可用性目标。
第二步按读链路、交易链路、履约链路、数据链路拆架构。
第三步重点讲交易正确性：库存不超卖、支付不重入账、订单状态不乱跳、异常可补偿。
第四步讲高并发：缓存、限流、热点隔离、分库分表、消息削峰和异步化。
第五步讲生产治理：灰度、回滚、观测、告警、压测、对账、审计和灾备。
```

如果面试官追问“100 万峰值并发怎么支撑”，不要只回答“加机器”。
应该拆成入口削峰、静态资源 CDN、商品详情缓存、秒杀令牌、交易链路限流、数据库分片、热点 SKU 隔离和降级策略。

## 专家级完整回答

```text
我会先把电商系统拆成读链路、交易链路、履约链路和数据链路。

读链路追求低延迟和高吞吐，依赖 CDN、缓存、搜索和热点保护。交易链路追求正确性，
重点是订单、库存、支付的一致性和幂等。履约链路通过事件驱动异步解耦。数据链路负责推荐、广告、风控和经营分析。

在架构上，我会用网关统一入口，按领域拆分微服务，核心数据用分库分表和本地事务保护，
跨服务一致性用 Outbox、幂等、补偿和对账解决。高可用方面通过多可用区、限流、熔断、降级、灰度发布和可观测性保障。

我不会一开始追求所有能力都复杂化。系统设计要先明确核心目标：交易不能错，资金不能错，库存不能超卖，
核心链路可恢复，非核心链路可降级。
```

## 回答评分点

高分答案应该覆盖：

- 先澄清规模、功能需求和非功能需求。
- 能按领域拆分用户、商品、搜索、订单、库存、支付、履约等服务。
- 能讲清浏览、下单、支付、履约核心数据流。
- 知道订单、库存、支付要靠幂等、Outbox、补偿和对账。
- 覆盖缓存、分库分表、消息、可观测、灰度和容灾。
