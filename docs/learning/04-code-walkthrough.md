# 代码驱动实现讲解

[返回学习手册首页](README.md) | [返回技术能力地图](../technical-skill-map.md)

## 代码驱动知识点详解

这一章用代码把概念串起来。学习方式建议是：先读解释，再手敲代码，再改参数观察结果。

### 用 Java 类表达业务对象

理论：类用来表达业务对象，字段表示状态，方法表示行为。

订单对象示例：

```java
public class Order {
    private final Long orderId;
    private final Long userId;
    private OrderStatus status;
    private Instant paidAt;

    public Order(Long orderId, Long userId) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = OrderStatus.CREATED;
    }

    public void markPaid() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Order cannot be paid from " + status);
        }
        this.status = OrderStatus.PAID;
        this.paidAt = Instant.now();
    }

    public OrderStatus status() {
        return status;
    }
}
```

订单状态枚举：

```java
public enum OrderStatus {
    CREATED,
    PAID,
    CANCELLED,
    FULFILLING,
    COMPLETED
}
```

这个例子要学会：

- 不要让外部随便改 `status`。
- 状态变化通过方法表达，例如 `markPaid`。
- 方法里检查业务规则。
- 非法状态要抛异常。

在 eMall 中，订单、库存预占、支付单、退款单都应该用类似方式表达状态和行为。

### 用 DTO 接收 HTTP 请求

理论：DTO 是接口入参或出参对象，不等同于数据库实体。

下单请求：

```java
public record CreateOrderRequest(
    Long userId,
    Long skuId,
    Integer quantity,
    String requestId
) {
}
```

下单响应：

```java
public record OrderResponse(
    Long orderId,
    String status,
    BigDecimal payableAmount
) {
}
```

为什么这样设计：

- 请求对象只表达 API 需要的字段。
- 响应对象只返回前端需要的字段。
- 不把数据库表结构直接暴露给外部。
- `requestId` 明确表达这个接口需要幂等。

进一步加校验：

```java
public record CreateOrderRequest(
    @NotNull Long userId,
    @NotNull Long skuId,
    @Positive Integer quantity,
    @NotBlank String requestId
) {
}
```

学习重点：

- `@NotNull` 防止空值。
- `@Positive` 防止数量小于等于 0。
- `@NotBlank` 防止空字符串。
- 参数校验只能检查格式，不能替代库存、价格、状态等业务校验。

### 写一个 Controller

理论：Controller 负责 HTTP 入口，不负责复杂业务。

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ApiResponse.ok(response);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getOrder(orderId));
    }
}
```

这个例子体现：

- `@RequestMapping("/api/orders")` 定义资源路径。
- `@PostMapping` 表示创建订单。
- `@GetMapping("/{orderId}")` 表示查询订单。
- `@Valid` 触发参数校验。
- Controller 调 Service，不直接写 SQL。

常见错误：

```java
@PostMapping("/createOrder")
public Object create(@RequestBody Map<String, Object> body) {
    // Bad: business logic and SQL are mixed here.
    return null;
}
```

为什么不好：

- 参数没有类型。
- 没有校验。
- Controller 过重。
- 后续测试困难。

### 统一响应和异常处理代码

统一响应：

```java
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    public static <T> ApiResponse<T> failed(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
```

业务异常：

```java
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
```

全局异常处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException ex) {
        return ApiResponse.failed(ex.code(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        return ApiResponse.failed("PARAM_INVALID", "Request parameter is invalid");
    }
}
```

学习重点：

- 业务错误和系统错误要区分。
- Controller 不要到处 try-catch。
- 错误码要稳定。
- 错误响应结构要统一。

### 用 Service 编排业务流程

理论：Service 是业务流程的核心，它负责事务、状态变化、调用下游和写 Outbox。

```java
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PricingClient pricingClient;
    private final MarketingClient marketingClient;
    private final InventoryClient inventoryClient;
    private final OutboxRepository outboxRepository;

    public OrderService(
        OrderRepository orderRepository,
        PricingClient pricingClient,
        MarketingClient marketingClient,
        InventoryClient inventoryClient,
        OutboxRepository outboxRepository
    ) {
        this.orderRepository = orderRepository;
        this.pricingClient = pricingClient;
        this.marketingClient = marketingClient;
        this.inventoryClient = inventoryClient;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        PriceSnapshot price = pricingClient.quote(request.skuId(), request.quantity());
        PromotionSnapshot promotion = marketingClient.quoteOrNoDiscount(request);
        inventoryClient.reserve(request.skuId(), request.quantity(), request.requestId());

        Order order = OrderFactory.create(request, price, promotion);
        orderRepository.save(order);
        outboxRepository.save(OrderCreatedEvent.from(order));

        return OrderResponseMapper.from(order);
    }
}
```

这段代码融合了多个知识点：

- 依赖注入。
- HTTP 入参转换为业务流程。
- 价格是强依赖。
- 营销是可降级依赖。
- 库存预占需要幂等键。
- `@Transactional` 保证订单和 Outbox 同事务。
- Service 不关心 HTTP 细节。

### Repository 和 SQL 怎么配合

理论：Repository 封装数据库访问。

接口：

```java
public interface OrderRepository {
    void save(Order order);

    Optional<Order> findById(Long orderId);

    Optional<Order> findByRequestId(String requestId);
}
```

Spring JDBC 实现示例：

```java
@Repository
public class JdbcOrderRepository implements OrderRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(Order order) {
        jdbcTemplate.update(
            """
            insert into orders(order_id, user_id, request_id, status, payable_amount, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?)
            """,
            order.orderId(),
            order.userId(),
            order.requestId(),
            order.status().name(),
            order.payableAmount(),
            Timestamp.from(order.createdAt()),
            Timestamp.from(order.updatedAt())
        );
    }
}
```

学习重点：

- SQL 字段要和表结构对应。
- Repository 不判断订单能不能支付。
- 业务状态判断放在领域对象或 Service。
- SQL 异常要转换成业务可理解的错误。

MyBatis Plus 思路：

```java
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
}
```

Service 中调用：

```java
OrderEntity entity = OrderEntity.from(order);
orderMapper.insert(entity);
```

MyBatis Plus 简化 CRUD，但不替你解决幂等、事务、状态机和补偿。

### 事务代码怎么写

创建订单时需要保证：

- 订单表写入成功。
- 订单明细写入成功。
- Outbox 事件写入成功。

代码：

```java
@Transactional
public void saveOrderWithEvent(Order order, List<OrderLine> lines) {
    orderRepository.save(order);
    orderLineRepository.saveAll(lines);
    outboxRepository.save(OrderCreatedEvent.from(order));
}
```

如果 `outboxRepository.save` 失败，前面的订单和明细也会回滚。

错误写法：

```java
public void saveOrderWithEvent(Order order) {
    orderRepository.save(order);
    kafkaTemplate.send("order-created", order.orderId().toString());
}
```

问题：

- 订单保存成功后，Kafka 发送可能失败。
- 失败后没有事件记录。
- 下游永远不知道订单创建了。

这就是 Outbox 要解决的问题。

### 幂等代码怎么写

幂等 Service：

```java
@Service
public class IdempotencyService {
    private final IdempotencyRepository repository;

    public IdempotencyService(IdempotencyRepository repository) {
        this.repository = repository;
    }

    public IdempotencyResult tryStart(String bizType, String requestId) {
        try {
            repository.insertProcessing(bizType, requestId);
            return IdempotencyResult.first();
        } catch (DuplicateKeyException ex) {
            IdempotencyRecord record = repository.find(bizType, requestId);
            if (record.completed()) {
                return IdempotencyResult.completed(record.responseRef());
            }
            return IdempotencyResult.processing();
        }
    }
}
```

下单中使用：

```java
public OrderResponse createOrder(CreateOrderRequest request) {
    IdempotencyResult result = idempotencyService.tryStart("create-order", request.requestId());
    if (result.completed()) {
        return orderRepository.findResponseByRequestId(request.requestId());
    }
    if (result.processing()) {
        throw new BusinessException("REQUEST_PROCESSING", "Request is still processing");
    }

    OrderResponse response = doCreateOrder(request);
    idempotencyService.markCompleted("create-order", request.requestId(), response.orderId().toString());
    return response;
}
```

数据库唯一键：

```sql
create unique index uk_idempotency_biz_request
on idempotency_record(biz_type, request_id);
```

学习重点：

- 幂等不是靠内存 Map。
- 并发安全靠数据库唯一键。
- 成功后要保存结果引用。
- 处理中状态要有明确返回。

### 库存防超卖代码怎么写

Repository：

```java
public boolean reserve(Long skuId, int quantity) {
    int updated = jdbcTemplate.update(
        """
        update inventory
        set available = available - ?,
            reserved = reserved + ?,
            updated_at = current_timestamp
        where sku_id = ?
          and available >= ?
        """,
        quantity,
        quantity,
        skuId,
        quantity
    );
    return updated == 1;
}
```

Service：

```java
@Transactional
public void reserve(ReserveInventoryCommand command) {
    if (reservationRepository.exists(command.requestId())) {
        return;
    }

    boolean success = inventoryRepository.reserve(command.skuId(), command.quantity());
    if (!success) {
        throw new BusinessException("INVENTORY_NOT_ENOUGH", "Inventory is not enough");
    }

    reservationRepository.save(command.toReservation());
}
```

这里的关键点：

- SQL 的 `available >= ?` 防止扣成负数。
- 更新行数为 0 表示库存不足。
- `requestId` 防止重复预占。
- 预占记录必须和库存扣减在同一个事务中。

确认库存：

```java
public void confirm(String requestId) {
    Reservation reservation = reservationRepository.findByRequestId(requestId);
    if (reservation.confirmed()) {
        return;
    }
    inventoryRepository.confirm(reservation.skuId(), reservation.quantity());
    reservationRepository.markConfirmed(requestId);
}
```

释放库存：

```java
public void release(String requestId) {
    Reservation reservation = reservationRepository.findByRequestId(requestId);
    if (reservation.released()) {
        return;
    }
    inventoryRepository.release(reservation.skuId(), reservation.quantity());
    reservationRepository.markReleased(requestId);
}
```

### 支付回调代码怎么写

支付回调入参：

```java
public record PaymentCallbackRequest(
    Long paymentId,
    String channel,
    String channelTradeNo,
    BigDecimal amount,
    String signature
) {
}
```

处理逻辑：

```java
@Transactional
public void handleCallback(PaymentCallbackRequest request) {
    signatureVerifier.verify(request);

    PaymentOrder payment = paymentRepository.findById(request.paymentId());
    if (payment.succeeded()) {
        return;
    }

    if (payment.amount().compareTo(request.amount()) != 0) {
        throw new BusinessException("PAYMENT_AMOUNT_MISMATCH", "Payment amount mismatch");
    }

    payment.markSucceeded(request.channelTradeNo());
    paymentRepository.update(payment);
    ledgerRepository.appendPaymentSuccess(payment);
    outboxRepository.save(PaymentSucceededEvent.from(payment));
}
```

数据库约束：

```sql
create unique index uk_payment_channel_trade
on payment_order(channel, channel_trade_no);
```

学习重点：

- 签名必须校验。
- 金额必须校验。
- 成功回调重复到达时直接返回。
- 渠道交易号必须唯一。
- 支付流水追加写。
- 订单确认失败要能重试。

### Outbox 代码怎么写

Outbox 事件对象：

```java
public record OutboxEvent(
    Long eventId,
    String topic,
    String eventKey,
    String eventType,
    String payload,
    OutboxStatus status,
    int retryCount,
    Instant nextRetryAt
) {
}
```

保存事件：

```java
public void save(DomainEvent event) {
    jdbcTemplate.update(
        """
        insert into outbox_event(event_id, topic, event_key, event_type, payload, status, retry_count, next_retry_at)
        values (?, ?, ?, ?, ?, 'NEW', 0, current_timestamp)
        """,
        event.eventId(),
        event.topic(),
        event.key(),
        event.type(),
        event.payload()
    );
}
```

Relay：

```java
public void publishPending(int limit) {
    List<OutboxEvent> events = outboxRepository.findPublishable(limit);
    for (OutboxEvent event : events) {
        if (!outboxRepository.markPublishing(event.eventId())) {
            continue;
        }
        try {
            kafkaTemplate.send(event.topic(), event.eventKey(), event.payload()).get();
            outboxRepository.markPublished(event.eventId());
        } catch (Exception ex) {
            outboxRepository.markFailed(event.eventId(), event.retryCount() + 1);
        }
    }
}
```

状态抢占 SQL：

```sql
update outbox_event
set status = 'PUBLISHING'
where event_id = ?
  and status in ('NEW', 'FAILED');
```

只有更新成功的实例才能发送，避免多实例重复抢同一条事件。

### Kafka 消费代码怎么写

消费者伪代码：

```java
public void onMessage(ProductChangedEvent event) {
    if (!messageDedupRepository.tryStart("search-product-consumer", event.messageId())) {
        return;
    }

    try {
        searchIndexRepository.upsert(event.toDocument());
        messageDedupRepository.markSuccess("search-product-consumer", event.messageId());
    } catch (Exception ex) {
        messageDedupRepository.markFailed("search-product-consumer", event.messageId(), ex.getMessage());
        throw ex;
    }
}
```

重点：

- 先去重，再处理。
- 处理成功后标记成功。
- 失败抛异常，让 MQ 重试。
- 搜索写入要能重复 upsert。

### Redis 缓存代码怎么写

```java
public ProductResponse getProduct(Long skuId) {
    String key = "product:" + skuId;
    ProductResponse cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return cached;
    }

    Product product = productRepository.findBySkuId(skuId)
        .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Product not found"));

    ProductResponse response = ProductResponse.from(product);
    redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(10));
    return response;
}
```

更新商品时：

```java
public void updateProductPrice(Long skuId, BigDecimal newPrice) {
    productRepository.updatePrice(skuId, newPrice);
    redisTemplate.delete("product:" + skuId);
    outboxRepository.save(ProductChangedEvent.priceChanged(skuId));
}
```

为什么更新后删除缓存：

- 数据库是最终权威数据。
- 删除缓存后，下次查询会重新加载。
- 直接更新缓存容易出现并发覆盖。

### 限流代码怎么写

简单本地限流示例：

```java
public class SimpleRateLimiter {
    private final AtomicInteger permits = new AtomicInteger(100);

    public boolean tryAcquire() {
        while (true) {
            int current = permits.get();
            if (current <= 0) {
                return false;
            }
            if (permits.compareAndSet(current, current - 1)) {
                return true;
            }
        }
    }
}
```

这个只能用于理解，不适合多实例生产。生产要用 Redis 或网关限流。

网关过滤器思路：

```java
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String key = buildRateLimitKey(exchange);
    boolean allowed = redisRateLimiter.tryAcquire(key);
    if (!allowed) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }
    return chain.filter(exchange);
}
```

学习重点：

- 限流要在请求进入核心服务前做。
- key 设计很重要。
- 多实例要用集中式计数。

### 熔断和降级代码怎么写

最简降级示例：

```java
public PromotionSnapshot quoteOrNoDiscount(CreateOrderRequest request) {
    try {
        return marketingClient.quote(request);
    } catch (Exception ex) {
        return PromotionSnapshot.noDiscount();
    }
}
```

适用场景：

- 营销失败可以无优惠下单。
- 推荐失败可以返回默认推荐。
- 评价失败可以隐藏评分。

不适用：

```java
public PriceSnapshot quoteOrZeroPrice(CreateOrderRequest request) {
    return PriceSnapshot.zero();
}
```

价格失败不能降级成 0 元，这会造成资金损失。

熔断思想伪代码：

```java
if (circuitBreaker.open()) {
    throw new BusinessException("DOWNSTREAM_UNAVAILABLE", "Inventory is unavailable");
}

try {
    inventoryClient.reserve(command);
    circuitBreaker.recordSuccess();
} catch (Exception ex) {
    circuitBreaker.recordFailure();
    throw ex;
}
```

学习重点：

- 降级要区分核心和非核心依赖。
- 熔断保护调用方，不是修复下游。
- 半开恢复要小流量探测。

### 测试代码怎么写

订单状态机单元测试：

```java
@Test
void createdOrderCanBeMarkedPaid() {
    Order order = new Order(1L, 10L);

    order.markPaid();

    assertThat(order.status()).isEqualTo(OrderStatus.PAID);
}
```

非法状态测试：

```java
@Test
void paidOrderCannotBePaidAgain() {
    Order order = new Order(1L, 10L);
    order.markPaid();

    assertThatThrownBy(order::markPaid)
        .isInstanceOf(IllegalStateException.class);
}
```

幂等集成测试：

```java
@Test
void sameRequestIdCreatesOnlyOneOrder() {
    CreateOrderRequest request = new CreateOrderRequest(1L, 100L, 1, "req-1");

    OrderResponse first = orderService.createOrder(request);
    OrderResponse second = orderService.createOrder(request);

    assertThat(second.orderId()).isEqualTo(first.orderId());
}
```

测试不是为了覆盖率好看，而是为了证明关键规则不会被改坏。

### Docker Compose 示例

本地 MySQL 示例：

```yaml
services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql

volumes:
  mysql-data:
```

学习重点：

- `image` 是使用哪个镜像。
- `environment` 是容器环境变量。
- `ports` 是端口映射。
- `volumes` 是数据持久化。

### Kubernetes Deployment 示例

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order
  template:
    metadata:
      labels:
        app: order
    spec:
      containers:
        - name: order
          image: emall/order:local
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
```

学习重点：

- `replicas` 表示副本数。
- `readinessProbe` 判断是否能接流量。
- `livenessProbe` 判断是否需要重启。
- 镜像 tag 要可追踪，生产不能随便用 `latest`。


## 核心实现详解

这一章把关键知识点落到“怎么手写实现”。你学习时可以按本章顺序，从一个最小核心交易系统开始写，
再逐步补稳定性和生产能力。

### 推荐代码分层

每个业务服务可以按下面方式组织代码：

```text
com.emall.order
├── OrderApplication.java
├── api
│   ├── OrderController.java
│   ├── CreateOrderRequest.java
│   └── OrderResponse.java
├── domain
│   ├── OrderStatus.java
│   ├── Order.java
│   └── OrderLine.java
├── service
│   ├── OrderService.java
│   └── OrderCompensationJob.java
├── repository
│   ├── OrderRepository.java
│   └── JdbcOrderRepository.java
├── client
│   ├── InventoryClient.java
│   ├── PricingClient.java
│   └── MarketingClient.java
└── config
    └── OrderConfiguration.java
```

各层职责：

- `api`：接收 HTTP 请求，做参数校验，转换响应。
- `domain`：放业务概念，例如订单状态、订单明细、库存状态。
- `service`：编排业务流程，控制事务边界，处理状态变化。
- `repository`：只负责数据访问，不写复杂业务决策。
- `client`：封装调用下游服务的细节，例如超时、错误码、trace ID。
- `config`：配置 Bean、属性绑定、HTTP 客户端、任务调度等。

常见错误：

- Controller 里直接写 SQL。
- Repository 里判断订单是否能取消。
- 下游 HTTP 调用散落在多个 Service 中。
- DTO、实体、数据库行对象混在一起。

### 一个接口从请求到数据库的完整路径

以下单接口为例：

```text
HTTP 请求
-> Gateway
-> OrderController
-> 参数校验
-> OrderService.createOrder
-> 查询价格快照
-> 查询优惠快照
-> 调用库存预占
-> 本地事务保存订单、订单明细、Outbox
-> 返回统一响应
```

Controller 只做薄薄一层：

```java
@PostMapping("/api/orders")
public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    return ApiResponse.ok(orderService.createOrder(request));
}
```

Service 才是核心：

```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    IdempotencyResult result = idempotencyService.tryStart("create-order", request.requestId());
    if (result.completed()) {
        return orderRepository.findResponseByRequestId(request.requestId());
    }

    PriceSnapshot price = pricingClient.quote(request.skuId(), request.quantity());
    PromotionSnapshot promotion = marketingClient.quoteOrNoDiscount(request);
    ReservationResult reservation = inventoryClient.reserve(request.skuId(), request.quantity(), request.requestId());

    Order order = Order.created(request, price, promotion, reservation);
    orderRepository.save(order);
    outboxRepository.save(OrderCreatedEvent.from(order));
    idempotencyService.markCompleted(request.requestId(), order.orderId());
    return OrderResponse.from(order);
}
```

这段伪代码要理解的重点：

- `requestId` 是幂等键。
- 价格失败不能继续下单。
- 营销失败可以降级为无优惠。
- 库存预占必须传同一个 `requestId`。
- 订单和 Outbox 事件必须在同一个本地事务里保存。

### 统一响应和错误码

统一响应用于让所有接口的成功和失败格式一致。

建议结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

错误响应：

```json
{
  "success": false,
  "code": "INVENTORY_NOT_ENOUGH",
  "message": "库存不足",
  "data": null
}
```

错误码应该稳定，不能随便改。前端、网关、监控和测试都会依赖错误码。

建议错误码分类：

- `PARAM_INVALID`：参数错误。
- `USER_FROZEN`：用户被冻结。
- `PRODUCT_OFF_SHELF`：商品已下架。
- `PRICE_UNAVAILABLE`：价格不可用。
- `INVENTORY_NOT_ENOUGH`：库存不足。
- `ORDER_STATUS_INVALID`：订单状态不允许操作。
- `PAYMENT_DUPLICATED`：支付回调重复。
- `DOWNSTREAM_UNAVAILABLE`：下游不可用。

### 订单状态机详解

订单状态不能随便改，必须有受控的状态机。

推荐状态：

```text
INIT
-> CREATED
-> PAID
-> FULFILLING
-> COMPLETED

CREATED
-> CANCELLED

INIT
-> PENDING_RETRY
-> CREATED

PAID
-> AFTER_SALES
```

状态转换规则：

| 当前状态 | 动作 | 目标状态 | 说明 |
| --- | --- | --- | --- |
| `INIT` | 库存预占成功 | `CREATED` | 订单创建成功 |
| `INIT` | 库存服务失败 | `PENDING_RETRY` | 等补偿任务 |
| `CREATED` | 支付成功 | `PAID` | 支付确认 |
| `CREATED` | 用户取消 | `CANCELLED` | 释放库存 |
| `CREATED` | 超时未支付 | `CANCELLED` | 释放库存 |
| `PAID` | 履约开始 | `FULFILLING` | 进入履约 |
| `FULFILLING` | 签收完成 | `COMPLETED` | 交易完成 |

实现时不要这样写：

```java
order.setStatus(request.status());
```

应该这样写：

```java
public void markPaid() {
    if (status != OrderStatus.CREATED) {
        throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
    }
    this.status = OrderStatus.PAID;
    this.paidAt = Instant.now();
}
```

状态机的价值：

- 防止非法状态跳转。
- 让代码能表达业务规则。
- 测试可以覆盖每个状态转换。
- 出问题时可以根据状态决定补偿动作。

### 核心表设计示例

订单表：

```sql
create table orders (
    order_id bigint primary key,
    user_id bigint not null,
    request_id varchar(64) not null,
    status varchar(32) not null,
    sku_id bigint not null,
    quantity int not null,
    payable_amount decimal(18, 2) not null,
    currency varchar(8) not null,
    price_version varchar(64) not null,
    coupon_id bigint null,
    created_at timestamp not null,
    updated_at timestamp not null,
    unique key uk_order_request (request_id)
);
```

库存表：

```sql
create table inventory (
    sku_id bigint primary key,
    available int not null,
    reserved int not null,
    sold int not null,
    updated_at timestamp not null
);
```

库存预占表：

```sql
create table inventory_reservation (
    reservation_id bigint primary key,
    request_id varchar(64) not null,
    sku_id bigint not null,
    quantity int not null,
    status varchar(32) not null,
    expire_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    unique key uk_reservation_request (request_id)
);
```

支付表：

```sql
create table payment_order (
    payment_id bigint primary key,
    order_id bigint not null,
    amount decimal(18, 2) not null,
    status varchar(32) not null,
    channel varchar(32) not null,
    channel_trade_no varchar(128) null,
    created_at timestamp not null,
    updated_at timestamp not null,
    unique key uk_channel_trade (channel, channel_trade_no)
);
```

Outbox 表：

```sql
create table outbox_event (
    event_id bigint primary key,
    aggregate_type varchar(64) not null,
    aggregate_id varchar(128) not null,
    event_type varchar(128) not null,
    payload text not null,
    status varchar(32) not null,
    retry_count int not null,
    next_retry_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null
);
```

幂等表：

```sql
create table idempotency_record (
    id bigint primary key,
    biz_type varchar(64) not null,
    request_id varchar(64) not null,
    status varchar(32) not null,
    response_ref varchar(128) null,
    created_at timestamp not null,
    updated_at timestamp not null,
    unique key uk_biz_request (biz_type, request_id)
);
```

设计要点：

- 幂等、防重、渠道交易号都要用唯一键兜底。
- 金额使用 `decimal`，不要用浮点数。
- 状态字段要有清晰枚举。
- 时间字段要支持排查和补偿扫描。

### 幂等的手写实现

幂等的核心不是“先查一下”，而是数据库唯一约束 + 状态记录。

典型流程：

```text
1. 插入幂等记录，状态为 PROCESSING。
2. 如果插入成功，说明第一次处理。
3. 如果唯一键冲突，查询已有记录。
4. 如果已有记录是 COMPLETED，直接返回历史结果。
5. 如果已有记录是 PROCESSING，返回处理中或稍后重试。
6. 业务成功后把幂等记录标记为 COMPLETED。
7. 业务失败后标记为 FAILED 或允许重试。
```

伪代码：

```java
public IdempotencyResult tryStart(String bizType, String requestId) {
    try {
        repository.insertProcessing(bizType, requestId);
        return IdempotencyResult.firstTime();
    } catch (DuplicateKeyException duplicate) {
        IdempotencyRecord record = repository.find(bizType, requestId);
        if (record.completed()) {
            return IdempotencyResult.completed(record.responseRef());
        }
        return IdempotencyResult.processing();
    }
}
```

常见坑：

- 只用 Redis 记录幂等，Redis 数据丢了会重复处理。
- 先查再插，在并发下两个请求都查不到。
- 业务成功了，但幂等记录没更新，导致后续重复处理。
- 幂等记录没有过期和清理策略。

### 库存预占手写实现

库存预占要同时解决防超卖和重复请求。

基本流程：

```text
1. 按 requestId 插入库存预占记录。
2. 如果 requestId 已存在，返回已有预占结果。
3. 执行条件扣减：available >= quantity。
4. 扣减成功：available 减少，reserved 增加。
5. 扣减失败：库存不足，预占失败。
6. 支付成功后 confirm：reserved 减少，sold 增加。
7. 取消或超时后 release：reserved 减少，available 增加。
```

预占 SQL：

```sql
update inventory
set available = available - ?,
    reserved = reserved + ?,
    updated_at = current_timestamp
where sku_id = ?
  and available >= ?;
```

确认 SQL：

```sql
update inventory
set reserved = reserved - ?,
    sold = sold + ?,
    updated_at = current_timestamp
where sku_id = ?
  and reserved >= ?;
```

释放 SQL：

```sql
update inventory
set reserved = reserved - ?,
    available = available + ?,
    updated_at = current_timestamp
where sku_id = ?
  and reserved >= ?;
```

库存预占记录状态：

- `RESERVED`
- `CONFIRMED`
- `RELEASED`
- `FAILED`

释放要先检查预占记录状态：

```java
if (reservation.status() == RELEASED) {
    return;
}
if (reservation.status() == CONFIRMED) {
    throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CONFIRMED);
}
```

热点 SKU 库存桶：

```text
sku_id = 10001
bucket_count = 64
bucket_no = hash(requestId) % 64
```

这样同一个 SKU 的扣减分散到 64 行，降低单行锁竞争。

### 支付回调手写实现

支付回调必须假设会重复、延迟、乱序。

处理流程：

```text
1. 校验渠道签名。
2. 用 channel + channelTradeNo 做唯一键去重。
3. 查询支付单。
4. 校验金额是否一致。
5. 如果支付单已成功，直接返回成功。
6. 写支付流水。
7. 更新支付单状态为 SUCCESS。
8. 调用订单服务确认支付。
9. 写 Outbox 支付成功事件。
10. 如果订单确认失败，记录待重试状态。
```

伪代码：

```java
@Transactional
public void handleCallback(PaymentCallback callback) {
    signatureVerifier.verify(callback);
    PaymentOrder payment = paymentRepository.findById(callback.paymentId());

    if (payment.succeeded()) {
        return;
    }
    if (payment.amount().compareTo(callback.amount()) != 0) {
        throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }

    payment.markSucceeded(callback.channelTradeNo());
    ledgerRepository.appendSuccess(payment, callback);
    outboxRepository.save(PaymentSucceededEvent.from(payment));
}
```

注意：订单确认可以同步做，也可以通过事件异步做。无论哪种方式，都要能重试。

常见坑：

- 只按 paymentId 去重，不按渠道交易号去重。
- 金额不校验。
- 支付流水覆盖更新，导致审计困难。
- 回调处理成功，但订单确认失败没有补偿。

### Outbox Relay 手写实现

Outbox relay 是一个后台任务。

扫描 SQL：

```sql
select *
from outbox_event
where status in ('NEW', 'FAILED')
  and next_retry_at <= current_timestamp
order by created_at
limit ?;
```

处理流程：

```text
1. 查询待发送事件。
2. 尝试发送 Kafka。
3. 发送成功，更新状态为 PUBLISHED。
4. 发送失败，retry_count + 1。
5. 计算下一次重试时间。
6. 超过最大重试次数，标记 DEAD。
```

伪代码：

```java
public void publishPending(int limit) {
    List<OutboxEvent> events = outboxRepository.findPublishable(limit);
    for (OutboxEvent event : events) {
        try {
            kafkaTemplate.send(event.topic(), event.key(), event.payload()).get();
            outboxRepository.markPublished(event.eventId());
        } catch (Exception ex) {
            outboxRepository.markFailed(event.eventId(), nextRetryAt(event.retryCount()));
        }
    }
}
```

实现细节：

- 多实例执行 relay 时需要锁或状态抢占。
- 发送失败不能删除事件。
- payload 要有版本，方便以后兼容。
- 下游消费者必须幂等。

状态抢占可以这样做：

```sql
update outbox_event
set status = 'PUBLISHING'
where event_id = ?
  and status in ('NEW', 'FAILED');
```

只有更新行数为 1 的实例才有权发送。

### MQ 消费端幂等实现

消费端要处理重复消息。

消费流程：

```text
1. 收到消息。
2. 按 messageId 插入消费记录。
3. 如果插入冲突，说明处理过，直接 ack。
4. 执行业务更新。
5. 标记消费成功。
6. 失败时抛异常，让 MQ 重试或进入死信。
```

消费记录表：

```sql
create table consumed_message (
    id bigint primary key,
    consumer_name varchar(128) not null,
    message_id varchar(128) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    unique key uk_consumer_message (consumer_name, message_id)
);
```

常见坑：

- 消费端没有去重。
- 先执行业务再插消费记录，失败后无法判断是否处理过。
- 消息没有版本，后续升级难兼容。
- 消费失败没有死信或人工处理入口。

### 补偿任务手写实现

补偿任务扫描失败状态并重试。

订单补偿示例：

```sql
select *
from orders
where status = 'PENDING_RETRY'
  and next_retry_at <= current_timestamp
order by created_at
limit ?;
```

处理流程：

```text
1. 查询 PENDING_RETRY 订单。
2. 根据失败阶段判断补偿动作。
3. 重试库存预占、库存确认或库存释放。
4. 成功后推进订单状态。
5. 失败后增加 retry_count。
6. 超过阈值后等待人工处理。
```

伪代码：

```java
public void retryPendingOrders(int limit) {
    List<Order> orders = orderRepository.findPendingRetry(limit);
    for (Order order : orders) {
        try {
            if (order.failedAtReserve()) {
                inventoryClient.reserve(order.skuId(), order.quantity(), order.requestId());
                order.markCreated();
            }
            orderRepository.save(order);
        } catch (Exception ex) {
            orderRepository.markRetryFailed(order.orderId(), ex.getMessage());
        }
    }
}
```

补偿任务必须满足：

- 可重复执行。
- 有最大重试次数。
- 有失败原因。
- 有人工触发入口。
- 多实例执行时不能重复处理同一条任务。

### 网关限流手写思路

令牌桶限流逻辑：

```text
1. 每个 key 有一个桶。
2. 桶按固定速率补充令牌。
3. 每个请求消耗一个令牌。
4. 有令牌则通过。
5. 没有令牌则拒绝。
```

限流 key 可以组合：

```text
rate:{userId}:{deviceId}:{skuId}:{clientIp}
```

Redis 实现通常使用 Lua 脚本保证原子性：

```text
读取当前令牌数和上次补充时间
计算应补充令牌
如果令牌数 >= 请求令牌数，则扣减并放行
否则拒绝
```

常见坑：

- 只按 IP 限流，容易误伤 NAT 后的大量正常用户。
- 只按用户限流，无法挡住匿名攻击。
- 限流阈值写死，不能动态调整。
- 限流失败时默认放行，导致保护失效。

### 缓存手写思路

商品详情缓存流程：

```text
1. 先查 Redis。
2. 命中则返回。
3. 未命中查 MySQL。
4. MySQL 查到后写 Redis，设置 TTL。
5. MySQL 查不到时缓存短 TTL 空值，防穿透。
```

伪代码：

```java
public Product getProduct(long skuId) {
    String key = "product:" + skuId;
    Product cached = redis.get(key);
    if (cached != null) {
        return cached;
    }

    Product product = productRepository.findBySkuId(skuId);
    redis.set(key, product, Duration.ofMinutes(10));
    return product;
}
```

缓存问题：

- 穿透：查询不存在的数据打到数据库。
- 击穿：热点 key 过期，大量请求同时打数据库。
- 雪崩：大量 key 同时过期。
- 不一致：数据库已更新，缓存还是旧值。

常见解决：

- 空值短 TTL。
- 热点 key 互斥重建。
- TTL 加随机抖动。
- 更新数据库后删除缓存。
- 重要数据用消息异步刷新。

### 内部运维接口实现

内部运维接口不是给用户用的，而是给运维或后台系统修复异常状态。

典型接口：

```text
POST /internal/operations/orders/retry-pending
POST /internal/operations/outbox/retry-failed
POST /internal/operations/inventory/release-expired-reservations
POST /internal/operations/payments/reconcile-channel-statements
```

必须具备：

- 内部 token。
- 操作人。
- trace ID。
- 参数限制。
- 审计记录。
- 幂等或可重复执行。

Filter 校验思路：

```java
String token = request.getHeader("X-Internal-Token");
if (!expectedToken.equals(token)) {
    response.setStatus(403);
    return;
}
```

审计记录：

```text
operator = X-Operator
traceId = X-Trace-Id
operation = retry-pending-orders
target = order
result = SUCCESS / FAILED
```

### 测试怎么写才算有效

单元测试要测业务规则：

```java
@Test
void paidOrderCannotBeCancelled() {
    Order order = Order.created(...);
    order.markPaid();

    assertThatThrownBy(order::cancel)
        .isInstanceOf(BusinessException.class);
}
```

Repository 集成测试要测真实 SQL：

```java
@Test
void duplicateRequestIdIsRejected() {
    repository.insertIdempotency("create-order", "req-1");

    assertThatThrownBy(() -> repository.insertIdempotency("create-order", "req-1"))
        .isInstanceOf(DuplicateKeyException.class);
}
```

E2E 测试要测核心链路：

```text
创建用户
创建商品
初始化库存
创建订单
支付
查询订单状态为 PAID
库存 reserved 减少，sold 增加
```

测试重点：

- 正常路径。
- 重复请求。
- 库存不足。
- 下游失败。
- 补偿恢复。
- 支付重复回调。
- MQ 重复消费。

### 一条核心链路的最小实现顺序

如果你要从 0 手写，不要一开始就写 40 个模块。推荐顺序：

1. `common`：统一响应、异常、错误码、ID。
2. `user`：创建用户和查询用户。
3. `product`：创建商品和查询商品。
4. `inventory`：初始化库存、预占、确认、释放。
5. `pricing`：返回价格快照。
6. `marketing`：返回优惠快照，可以先实现无优惠。
7. `order`：幂等下单、保存订单、取消订单。
8. `payment`：支付单、回调、确认订单。
9. `outbox`：订单和支付事件可靠发布。
10. `smoke`：跑通下单和支付链路。

每一步都要补测试。没有测试的功能，只能算“写过”，不能算“掌握”。
