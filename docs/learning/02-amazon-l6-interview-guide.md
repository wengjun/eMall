# Amazon L6 面试导向指南

[返回学习手册首页](README.md) | [返回技术能力地图](../technical-skill-map.md)

## Amazon L6 面试导向深度指南

这一章把 eMall 项目映射到 Amazon L6/Senior SDE 面试需要展示的能力。目标不是背答案，而是训练你能在
系统设计、编码、生产经验和行为面试中讲出“有判断、有取舍、有落地、有结果”的方案。

需要先明确：L6 不是“会很多技术点”，而是能独立负责复杂系统或大模块，能在模糊问题中做正确拆解，
能发现风险，能推动方案落地，并能用生产指标证明效果。

### L6 面试看什么

常见评价维度：

- 技术深度：能讲清楚关键实现，不停留在名词。
- 架构判断：能解释为什么选这个方案，为什么不选另一个方案。
- 规模意识：能做容量估算、瓶颈分析、降级和扩展设计。
- 生产意识：能考虑监控、告警、回滚、数据修复和事故复盘。
- 代码质量：能写出清晰、可测试、可维护的代码。
- 领导力：能体现 Ownership、Dive Deep、Invent and Simplify、Deliver Results。
- 沟通能力：能把复杂问题讲清楚，能接受追问并调整方案。

高评价回答通常具备这些特征：

- 先澄清需求和约束，再设计。
- 先给整体架构，再深入关键链路。
- 能主动指出风险和边界。
- 能量化容量、延迟、错误率、数据规模。
- 能讲失败场景和恢复机制。
- 能解释业务取舍，而不是堆技术名词。

### 系统设计回答框架

遇到“设计一个京东/Amazon 类电商系统”时，不要一上来画一堆服务。按这个顺序答：

1. 澄清需求。
2. 定义核心 API。
3. 估算规模。
4. 设计数据模型。
5. 设计高层架构。
6. 深挖核心交易链路。
7. 处理一致性和失败恢复。
8. 处理高并发和热点。
9. 设计可观测、运维和发布。
10. 说明取舍和后续演进。

示例开场：

```text
我先聚焦核心交易链路：商品浏览、下单、库存预占、支付、履约。
搜索、推荐、广告、客服和数据平台可以作为异步或扩展域，不放进同步交易主链路。
对于一致性，我不会使用全局分布式事务，而会用本地事务、幂等、Outbox、补偿和对账。
```

这个开场体现：

- 你会控制范围。
- 你知道核心链路是什么。
- 你知道分布式事务不是默认答案。
- 你能把复杂系统拆成同步主链路和异步扩展链路。

### 需求澄清要问什么

L6 候选人需要主动澄清约束。

可以问：

- 是只设计核心下单，还是完整 marketplace？
- 是否需要支持秒杀和大促？
- 峰值 QPS、DAU、订单量级是多少？
- 是否要求多区域 active-active？
- 支付和库存是否要求强一致？
- 搜索和推荐能否最终一致？
- 是否需要商家入驻、结算、售后？
- 可用性和延迟目标是什么？

然后主动给出假设：

```text
如果没有更多约束，我假设读流量远大于写流量，商品详情和搜索是读多场景；
下单、库存、支付是强约束写场景；搜索、推荐和评价允许最终一致。
```

这比直接说“我用 Redis、Kafka、MySQL”更像 L6。

### 容量估算怎么做

L6 系统设计必须能估算规模。估算不要求绝对准确，但要合理。

假设：

```text
注册用户：10 亿
DAU：1 亿
峰值并发：100 万
日订单量：5000 万
峰值下单 QPS：10 万
商品详情 QPS：100 万
搜索 QPS：30 万
支付回调 QPS：5 万
```

订单存储估算：

```text
5000 万订单 / 天
每个订单主表约 1 KB
订单明细平均 3 行，每行约 500 B
每天订单数据约：50 GB + 75 GB = 125 GB
一年约 45 TB，不含索引和副本
```

由估算推导设计：

- 订单表必须分库分表或按时间归档。
- 商品详情必须缓存。
- 搜索必须用 OpenSearch 这类读模型。
- 下单链路必须限流和削峰。
- Kafka 消息积压要有监控和扩容方案。
- 历史订单不能永远留在核心交易库。

面试时要讲“估算如何影响架构”，不要只报数字。

### 高层架构怎么讲

推荐按层讲：

```text
客户端 -> CDN/WAF -> Gateway -> 核心服务 -> 数据和中间件 -> 观测和运维
```

核心服务拆分：

- `user`：用户和账号状态。
- `product`：商品读写和上下架。
- `pricing`：价格和价格快照。
- `marketing`：优惠券和促销。
- `inventory`：库存预占、确认、释放。
- `order`：订单状态机。
- `payment`：支付单、回调、退款和对账。
- `fulfillment`：履约。
- `search`：搜索读模型。

关键解释：

```text
我会让订单、库存、支付分别拥有自己的数据库，因为它们有不同的数据所有权和扩缩容需求。
跨服务流程不共享数据库表，而是通过 API、事件和补偿协作。
```

如果面试官追问“为什么不单库”，可以回答：

```text
单库早期简单，但高峰下订单、库存、支付会互相抢资源。
服务边界不清晰时，任何团队都可能改核心表，长期会导致发布和故障半径不可控。
拆库后跨服务一致性更复杂，所以必须补幂等、Outbox、补偿和对账。
```

这体现了取舍，而不是教条式微服务。

### 核心 API 怎么设计

核心交易 API：

```text
POST /api/orders
GET /api/orders/{orderId}
POST /api/orders/{orderId}/pay
POST /api/orders/{orderId}/cancel
GET /api/inventory/{skuId}
POST /api/inventory/reservations
POST /api/inventory/reservations/{requestId}/confirm
POST /api/inventory/reservations/{requestId}/release
POST /api/payments
POST /api/payments/{paymentId}/callbacks
POST /api/payments/{paymentId}/refund
```

下单请求必须包含：

```json
{
  "userId": 10001,
  "skuId": 20001,
  "quantity": 1,
  "requestId": "client-generated-id"
}
```

为什么要 `requestId`：

- 用户可能重复点击。
- 客户端超时可能重试。
- 网关可能重试。
- 服务内部补偿可能重复触发。

L6 级别回答要强调：幂等是 API contract 的一部分，不是实现细节。

### 数据模型怎么讲

核心表要围绕业务不变量设计。

订单表关键字段：

```text
order_id
user_id
request_id
status
sku_id
quantity
payable_amount
price_version
coupon_id
created_at
updated_at
```

库存表关键字段：

```text
sku_id
available
reserved
sold
updated_at
```

预占表关键字段：

```text
request_id
sku_id
quantity
status
expire_at
```

支付表关键字段：

```text
payment_id
order_id
amount
status
channel
channel_trade_no
```

关键唯一约束：

```sql
unique key uk_order_request(request_id);
unique key uk_reservation_request(request_id);
unique key uk_channel_trade(channel, channel_trade_no);
unique key uk_idempotency(biz_type, request_id);
```

面试中要主动说：

```text
唯一约束是幂等和防重的最后防线。只靠代码先查再写，在并发下不可靠。
```

### 下单链路怎么深入讲

同步主链路：

```text
Gateway
-> Order
-> Pricing
-> Marketing
-> Inventory Reserve
-> Order DB + Outbox
-> Response
```

关键取舍：

- 价格是强依赖，失败则拒绝下单。
- 营销是弱依赖，失败可降级为无优惠。
- 库存是强依赖，但库存服务短暂失败可进入 `PENDING_RETRY`。
- 履约、通知、搜索、推荐不在同步下单主链路里。

伪代码：

```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    IdempotencyResult idem = idempotencyService.tryStart("create-order", request.requestId());
    if (idem.completed()) {
        return orderRepository.findResponseByRequestId(request.requestId());
    }

    PriceSnapshot price = pricingClient.quote(request.skuId(), request.quantity());
    PromotionSnapshot promotion = marketingClient.quoteOrNoDiscount(request);

    try {
        inventoryClient.reserve(request.skuId(), request.quantity(), request.requestId());
        Order order = Order.created(request, price, promotion);
        orderRepository.save(order);
        outboxRepository.save(OrderCreatedEvent.from(order));
        idempotencyService.markCompleted(request.requestId(), order.orderId().toString());
        return OrderResponse.from(order);
    } catch (DownstreamTimeoutException ex) {
        Order order = Order.pendingRetry(request, price, promotion, ex.getMessage());
        orderRepository.save(order);
        return OrderResponse.from(order);
    }
}
```

面试追问点：

- 为什么营销可以降级，价格不行？
- `PENDING_RETRY` 会不会让用户困惑？
- 库存预占失败后是否应该创建订单？
- 如果库存最终不足，订单如何关闭？
- 如果响应失败但订单创建成功，客户端怎么查？

高评价回答要能承认边界：

```text
不是所有失败都适合 PENDING_RETRY。库存明确不足应该直接失败；
只有网络超时、下游临时不可用这类不确定失败，才进入可补偿状态。
```

### 库存防超卖怎么深入讲

库存不变量：

```text
available >= 0
reserved >= 0
sold >= 0
available + reserved + sold = total
```

预占 SQL：

```sql
update inventory
set available = available - ?,
    reserved = reserved + ?
where sku_id = ?
  and available >= ?;
```

为什么这条 SQL 能防超卖：

- 判断和扣减在数据库内一次完成。
- 数据库行锁保证同一行并发更新串行化。
- 更新行数为 0 表示库存不足。

确认库存：

```sql
update inventory
set reserved = reserved - ?,
    sold = sold + ?
where sku_id = ?
  and reserved >= ?;
```

释放库存：

```sql
update inventory
set reserved = reserved - ?,
    available = available + ?
where sku_id = ?
  and reserved >= ?;
```

更深入的点：

- 只更新库存表不够，还要有预占记录。
- 确认和释放要根据预占记录状态判断。
- 重复释放不能重复加库存。
- 热点 SKU 会让单行锁成为瓶颈。
- 库存桶可以降低单行竞争。

库存桶设计：

```text
inventory_bucket(sku_id, bucket_no, available, reserved, sold)
bucket_no = hash(requestId) % bucket_count
```

取舍：

- 优点：降低单行锁竞争。
- 缺点：查询总库存要聚合多个桶。
- 缺点：桶分配不均可能导致某个桶先卖完。
- 适用：大促、秒杀、热点 SKU。

### 支付链路怎么深入讲

支付比订单更敏感，因为涉及资金。

支付回调必须处理：

- 重复回调。
- 回调乱序。
- 回调延迟。
- 金额不一致。
- 签名无效。
- 本地成功但订单确认失败。

核心逻辑：

```java
@Transactional
public void handleCallback(PaymentCallback callback) {
    signatureVerifier.verify(callback);
    Payment payment = paymentRepository.findById(callback.paymentId());

    if (payment.succeeded()) {
        return;
    }
    if (!payment.sameAmount(callback.amount())) {
        throw new BusinessException("PAYMENT_AMOUNT_MISMATCH", "Amount mismatch");
    }

    payment.markSucceeded(callback.channelTradeNo());
    paymentRepository.update(payment);
    ledgerRepository.append(callback.toLedgerEntry());
    outboxRepository.save(PaymentSucceededEvent.from(payment));
}
```

为什么写支付流水：

- 方便审计。
- 方便对账。
- 方便排查重复回调。
- 避免覆盖历史信息。

如果订单确认失败：

```text
支付状态：SUCCESS
订单状态：CREATED
补偿任务：retry-order-confirmation
```

为什么不能简单回滚支付：

- 钱可能已经在渠道侧支付成功。
- 本地回滚不能让渠道自动退款。
- 正确做法是记录本地状态，然后确认订单或走退款流程。

### Outbox 和 MQ 怎么深入讲

Outbox 解决本地事务和消息发送之间的不一致。

正确流程：

```text
本地事务：
写订单表
写 Outbox 表
提交

异步 Relay：
扫描 Outbox
发送 Kafka
标记已发布
```

为什么不能在事务里直接发 MQ：

- 事务提交前消息已发，下游可能读不到订单。
- 事务提交后 MQ 发送失败，事件丢失。
- MQ 发送耗时会拉长数据库事务。

Relay 多实例问题：

```sql
update outbox_event
set status = 'PUBLISHING'
where event_id = ?
  and status in ('NEW', 'FAILED');
```

只有抢占成功的实例发送。

消费者幂等：

```text
message_id + consumer_name 建唯一键
插入成功才处理
插入冲突直接跳过
```

高评价回答要说清楚：

- Outbox 保证不丢事件，不保证只发送一次。
- Kafka 通常按至少一次投递设计。
- Exactly once 很难跨业务系统端到端保证。
- 所以业务消费者必须幂等。

### 高并发保护怎么深入讲

高并发不是只加机器。

分层保护：

```text
CDN：静态资源和部分缓存
WAF：攻击和恶意流量
Gateway：限流、鉴权、路由
Service：熔断、降级、超时、线程池
DB：索引、分库分表、热点拆分
MQ：削峰、异步化
Cache：读扩展
```

下单链路保护：

- 用户维度限流。
- IP 和设备维度限流。
- SKU 维度限流。
- 秒杀令牌。
- 库存桶。
- 支付回调独立线程池。
- 非核心服务降级。

线程池隔离例子：

```text
order-core-pool：下单核心逻辑
marketing-client-pool：营销调用
payment-callback-pool：支付回调
outbox-publisher-pool：事件发布
```

为什么隔离：

- 营销慢不能耗尽下单线程。
- Outbox 积压不能影响 HTTP 请求。
- 支付回调高峰不能影响用户查询。

### 可观测性怎么深入讲

L6 必须能讲“出了问题怎么定位”。

核心指标：

```text
order_create_qps
order_create_success_rate
inventory_reserve_failure_rate
payment_callback_failure_rate
outbox_pending_count
kafka_consumer_lag
http_server_p99_latency
db_connection_pool_usage
redis_cache_hit_ratio
```

日志必须包含：

```text
traceId
userId
orderId
paymentId
skuId
requestId
errorCode
downstreamService
latencyMs
```

排障路径示例：

```text
告警：下单成功率下降
-> 看网关 5xx 和限流
-> 看订单服务 P99
-> 看库存预占失败率
-> 看库存 DB 锁等待
-> 看热点 SKU 分布
-> 决定是否限流、扩容、开启秒杀队列或下架活动
```

高评价回答要包含：

- 先保护核心链路。
- 再定位瓶颈。
- 再恢复失败状态。
- 最后复盘并修复机制。

### L6 行为面试怎么结合项目讲

Amazon 行为面试重视 Leadership Principles。你需要准备真实或接近真实的 STAR 故事。

STAR 结构：

```text
Situation：背景是什么
Task：你的目标是什么
Action：你具体做了什么
Result：结果如何量化
```

Ownership 示例：

```text
S：下单链路存在库存预占失败后订单状态不一致的问题。
T：我需要让失败状态可恢复，并降低人工排查成本。
A：我设计了 PENDING_RETRY 状态、补偿任务、Outbox 重放和内部运维接口。
R：失败订单可以自动重试，无法自动恢复的订单能通过审计接口人工处理。
```

Dive Deep 示例：

```text
S：压测时下单 P99 突然升高。
T：定位是应用瓶颈、DB 瓶颈还是下游瓶颈。
A：我按 traceId、库存失败率、DB 锁等待、热点 SKU 分布逐层排查。
R：发现单 SKU 库存行锁竞争严重，于是引入库存桶降低竞争。
```

Invent and Simplify 示例：

```text
S：直接引入复杂分布式事务成本过高。
T：需要在可控复杂度下解决订单、库存、支付一致性。
A：采用本地事务、幂等、Outbox、补偿和对账组合。
R：避免了 XA 的性能和可用性问题，同时保留可恢复能力。
```

Deliver Results 示例：

```text
S：工程模块多，验证成本高。
T：需要保证每次修改不会破坏核心链路。
A：建立单元测试、集成测试、smoke 测试和 Maven profile。
R：可以通过 mvn test 和 mvn verify 分层验证。
```

行为题不要只说“我参与了”，要说清楚：

- 你负责什么。
- 你做了什么决策。
- 你遇到了什么反对意见。
- 你如何用数据证明方案。
- 结果是什么。

### L6 自测清单

如果你想用这个项目冲 L6，至少要能做到：

- 30 分钟讲清楚 eMall 总体架构。
- 20 分钟讲清楚下单、库存、支付一致性。
- 10 分钟写出幂等核心代码。
- 10 分钟写出库存防超卖 SQL。
- 10 分钟解释 Outbox 和消费者幂等。
- 10 分钟解释支付重复回调和对账。
- 10 分钟做容量估算。
- 10 分钟讲清楚系统降级和熔断。
- 10 分钟讲清楚监控指标和事故排查路径。
- 准备 6 个以上 STAR 行为故事。

如果只能背概念，但写不出代码、画不出数据流、讲不清失败恢复，通常达不到 L6 强评价。

### 面试高频追问和好答案方向

追问：为什么不用分布式事务？

```text
因为下单链路涉及订单、库存、支付多个服务。XA 会增加锁持有时间，降低可用性，
并且任一下游慢都会拖垮主链路。我会把强一致限制在服务本地事务内，
跨服务用幂等、Outbox、补偿和对账实现最终一致。
```

追问：Outbox 会不会重复发消息？

```text
会。Outbox 解决的是不丢消息，不是端到端 exactly once。
所以消费者必须用 messageId 或业务唯一键做幂等。
```

追问：库存服务超时，但实际预占成功了怎么办？

```text
订单进入不确定状态，后续用 requestId 查询或重试预占。
库存预占接口必须幂等，同一个 requestId 重试会返回同一个预占结果。
```

追问：营销服务挂了怎么办？

```text
营销不是资金安全的强依赖，可以降级为无优惠，但要记录降级指标。
价格服务不能这样降级，因为错误价格会造成资金损失。
```

追问：如何证明系统稳定？

```text
我会用单元测试证明状态机和业务规则，用集成测试证明数据库、Redis、Kafka 行为，
用 smoke 测试证明核心链路可用，用压测证明容量，并用指标和告警观察生产表现。
```
