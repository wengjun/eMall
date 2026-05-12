# 零基础概念详解

[返回学习手册首页](README.md) | [返回技术能力地图](../technical-skill-map.md)

## 零基础知识点详解总纲

这一章把后面所有知识点用更基础的方式讲一遍。读这一章时，不需要先会大型项目，只需要理解：

- 程序接收请求。
- 程序执行业务逻辑。
- 程序读写数据库。
- 程序调用其它服务。
- 程序失败后要能恢复。

### Java 是什么

Java 是写后端服务的编程语言。你写的 Java 代码会被编译成 `.class` 字节码，然后运行在 JVM 上。

你需要先理解三个层次：

- 语法：变量、判断、循环、方法、类。
- 业务建模：用类表达用户、订单、库存、支付等业务概念。
- 运行机制：对象如何创建、内存如何使用、异常如何传播、线程如何执行。

一个最简单的类：

```java
public class Order {
    private Long orderId;
    private String status;

    public void markPaid() {
        this.status = "PAID";
    }
}
```

这段代码表达：

- `Order` 是订单类。
- `orderId` 是订单 ID。
- `status` 是订单状态。
- `markPaid` 是把订单改成已支付的方法。

零基础学习重点：

- 类是对业务对象的抽象。
- 字段保存对象状态。
- 方法表达对象行为。
- 业务规则应该尽量写进方法，而不是到处直接改字段。

### 什么是后端服务

后端服务是运行在服务器上的程序，负责处理客户端请求。

一次典型请求：

```text
用户点击下单
-> 浏览器或 App 发送 HTTP 请求
-> 网关接收请求
-> 订单服务处理请求
-> 订单服务调用库存服务
-> 服务写入数据库
-> 返回结果给用户
```

后端服务必须解决：

- 请求是否合法。
- 用户有没有权限。
- 业务规则是否允许。
- 数据库写入是否成功。
- 下游服务是否可用。
- 失败后如何返回错误。

在 eMall 中，`order`、`inventory`、`payment` 等模块都是后端服务。

### HTTP 和 REST API

HTTP 是客户端和服务端通信的协议。REST API 是一种常见接口设计风格。

常见 HTTP 方法：

- `GET`：查询数据。
- `POST`：创建资源或提交动作。
- `PUT`：整体更新。
- `PATCH`：局部更新。
- `DELETE`：删除资源。

示例：

```text
GET /api/products/10001
POST /api/orders
POST /api/orders/20001/pay
POST /api/orders/20001/cancel
```

零基础要理解：

- URL 表达你要操作什么资源。
- HTTP 方法表达你要做什么动作。
- 请求体传业务参数。
- 响应体返回结果。

常见错误：

- 所有接口都用 `POST /do`。
- 接口名看不出业务含义。
- 没有统一错误码。
- 同一个错误在不同接口返回不同格式。

### Spring Boot 是什么

Spring Boot 是 Java 后端开发框架。它帮你快速启动 Web 服务、管理对象、读取配置、处理请求。

最小结构：

```java
@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

Controller 示例：

```java
@RestController
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders")
    public OrderResponse create(@RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }
}
```

这里要理解：

- `@RestController` 表示这是 HTTP 接口类。
- `@PostMapping` 表示处理 POST 请求。
- `@RequestBody` 表示从请求体读取 JSON。
- `OrderService` 由 Spring 自动注入。

Spring Boot 的核心价值：

- 管理对象。
- 简化配置。
- 快速创建 Web 服务。
- 集成数据库、缓存、MQ、监控等组件。

### 什么是依赖注入

依赖注入是 Spring 的核心思想之一。简单说，就是对象需要其它对象时，不自己 `new`，而是由 Spring 提供。

不推荐：

```java
public class OrderController {
    private OrderService orderService = new OrderService();
}
```

推荐：

```java
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

好处：

- 方便测试。
- 方便替换实现。
- 对象生命周期统一管理。
- 配置更清晰。

在 eMall 中，Controller 依赖 Service，Service 依赖 Repository 和 Client。

### Maven 是什么

Maven 是 Java 项目的构建工具。它负责：

- 下载依赖。
- 编译代码。
- 执行测试。
- 打包 JAR。
- 管理多模块工程。

最常用命令：

```powershell
mvn validate
mvn test
mvn package
mvn verify
```

零基础理解：

- `pom.xml` 是 Maven 项目的配置文件。
- `dependencies` 写项目需要哪些第三方库。
- `modules` 写这个工程有哪些子模块。
- `plugins` 写编译、测试、打包时用什么插件。

为什么 eMall 用多模块：

- 每个服务是一个模块。
- 公共代码放在 `common`。
- 根 POM 统一版本。
- 可以只构建部分服务，也可以构建全部服务。

### 数据库是什么

数据库用于持久化保存数据。程序重启后，内存会丢失，但数据库中的数据还在。

最基础概念：

- 表：一类数据，例如订单表。
- 行：一条数据，例如一个订单。
- 列：一个字段，例如订单状态。
- 主键：唯一标识一行。
- 索引：加速查询的数据结构。
- 事务：保证一组操作一起成功或一起失败。

订单表示例：

```sql
create table orders (
    order_id bigint primary key,
    user_id bigint not null,
    status varchar(32) not null,
    created_at timestamp not null
);
```

插入订单：

```sql
insert into orders(order_id, user_id, status, created_at)
values (10001, 20001, 'CREATED', current_timestamp);
```

查询订单：

```sql
select *
from orders
where order_id = 10001;
```

更新订单：

```sql
update orders
set status = 'PAID'
where order_id = 10001;
```

在 eMall 中，数据库保存用户、商品、库存、订单、支付、流水、Outbox、幂等记录。

### 什么是事务

事务保证一组数据库操作要么全部成功，要么全部失败。

例子：创建订单时，需要同时写订单表和订单明细表。如果订单写成功，但明细写失败，就会产生脏数据。

事务可以这样理解：

```text
开始事务
写订单表
写订单明细表
写 Outbox 表
全部成功 -> 提交事务
任一步失败 -> 回滚事务
```

Spring 中常用：

```java
@Transactional
public void createOrder() {
    orderRepository.save(order);
    orderLineRepository.save(lines);
    outboxRepository.save(event);
}
```

注意：

- 本地事务只能保证当前服务当前数据库。
- 不能保证订单库和库存库一起提交。
- 跨服务一致性要靠幂等、Outbox、补偿和对账。

### 什么是索引

索引用来加快查询。

没有索引时，数据库可能要从第一行扫到最后一行。数据量小时没感觉，数据量大时会很慢。

适合建索引的字段：

- `order_id`
- `user_id`
- `sku_id`
- `request_id`
- `payment_id`
- `channel_trade_no`
- `status + created_at`

例子：

```sql
create index idx_orders_user_id on orders(user_id);
create unique index uk_orders_request_id on orders(request_id);
```

区别：

- 普通索引用于加速查询。
- 唯一索引还能防止重复数据。

在幂等设计里，唯一索引非常重要。

### 什么是缓存

缓存是把热点数据放到更快的存储里，例如 Redis。

为什么需要缓存：

- 商品详情访问量很高。
- 每次都查数据库压力太大。
- Redis 比 MySQL 更适合高频简单读取。

典型流程：

```text
先查 Redis
命中 -> 直接返回
未命中 -> 查 MySQL
查到后写 Redis
返回结果
```

缓存要解决的问题：

- 缓存穿透：查不存在的数据，反复打到数据库。
- 缓存击穿：热点 key 过期，大量请求同时查数据库。
- 缓存雪崩：大量 key 同时过期。
- 缓存不一致：数据库变了，缓存还是旧值。

学习建议：

- 先实现简单缓存。
- 再加 TTL。
- 再处理空值缓存。
- 再处理更新后删除缓存。
- 最后学习热点 key 和缓存重建。

### 什么是消息队列

消息队列用于异步解耦。

不用 MQ 的同步流程：

```text
商品服务更新商品
-> 直接调用搜索服务更新索引
-> 搜索服务失败
-> 商品更新也失败或变慢
```

使用 MQ：

```text
商品服务更新商品
-> 写商品变更事件
-> Kafka 保存事件
-> 搜索服务异步消费事件
-> 更新搜索索引
```

好处：

- 写商品不被搜索服务拖慢。
- 下游可以慢慢消费。
- 失败可以重试。
- 一个事件可以给多个消费者。

代价：

- 数据会短暂不一致。
- 消息可能重复。
- 消费端必须幂等。
- 需要监控消费延迟。

### 什么是微服务

微服务是把一个大系统拆成多个小服务，每个服务负责一个业务域。

单体系统：

```text
用户、商品、订单、库存、支付都在一个应用里
```

微服务系统：

```text
用户服务
商品服务
订单服务
库存服务
支付服务
```

微服务优点：

- 不同服务可以独立开发。
- 不同服务可以独立部署。
- 不同服务可以独立扩容。
- 一个服务故障不一定拖垮全部系统。

微服务缺点：

- 调用链变长。
- 一致性更复杂。
- 部署更复杂。
- 测试更复杂。
- 运维要求更高。

学习重点：

- 不是服务越多越好。
- 服务边界要按业务域划分。
- 每个服务要拥有自己的数据。
- 跨服务不能随便共享数据库表。

### 什么是网关

网关是系统统一入口。

网关负责：

- 路由请求到不同服务。
- 做限流。
- 生成或透传 trace ID。
- 增加安全响应头。
- 隔离外部和内部接口。

网关不负责：

- 订单状态判断。
- 库存扣减。
- 支付记账。
- 复杂业务规则。

简单理解：

```text
用户请求先进网关
网关判断该去哪个服务
网关转发请求
业务服务处理请求
```

### 什么是幂等

幂等是分布式系统里非常重要的概念。它的意思是：同一个请求执行一次和执行多次，最终结果一样。

为什么需要：

- 用户重复点击下单。
- 浏览器超时后重试。
- 支付渠道重复回调。
- MQ 重复投递。
- 运维人员重复触发补偿。

下单幂等例子：

```text
第一次 requestId=req-1 -> 创建订单 10001
第二次 requestId=req-1 -> 直接返回订单 10001
不能再创建订单 10002
```

常见实现：

- 请求必须带 `requestId`。
- 数据库建立唯一索引。
- 保存处理状态。
- 成功后保存结果引用。

关键认知：

- 幂等不是简单“先查一下”。
- 并发下两个请求可能同时查不到。
- 必须依赖数据库唯一约束兜底。

### 什么是 Outbox

Outbox 是可靠事件发布模式。

它解决的问题：

```text
业务数据库写成功了，但 MQ 发送失败了，怎么办？
```

错误做法：

```text
先写数据库
再发 MQ
如果 MQ 失败，业务数据和事件就不一致
```

Outbox 做法：

```text
在同一个数据库事务里：
1. 写业务表
2. 写 Outbox 事件表

后台任务：
1. 扫描 Outbox 表
2. 发送 MQ
3. 成功后标记已发布
4. 失败后下次重试
```

优点：

- 事件不会丢。
- 失败可以重试。
- 可以人工重放。
- 可以审计。

代价：

- 消息可能重复发送。
- 消费端必须幂等。
- Outbox 表需要清理。

### 什么是补偿

补偿是失败后的恢复动作。

例子：

```text
订单创建成功
库存预占失败
订单不能直接丢掉
可以进入 PENDING_RETRY
后台任务稍后重试库存预占
```

补偿适合处理：

- 下游服务临时不可用。
- 网络超时。
- MQ 发送失败。
- 支付成功但订单确认失败。
- 库存预占超时未释放。

补偿设计要求：

- 失败状态要落库。
- 失败原因要记录。
- 重试次数要限制。
- 重试操作必须幂等。
- 多次失败要能人工处理。

### 什么是对账

对账是比较两个系统的数据是否一致。

支付对账例子：

```text
支付渠道账单：订单 10001 支付成功 99 元
本地支付表：订单 10001 仍是待支付
这就是差异
```

为什么需要对账：

- 渠道回调可能丢。
- 本地服务可能处理失败。
- 网络可能超时。
- 人工操作可能出错。

对账流程：

1. 导入渠道账单。
2. 按渠道交易号匹配本地支付单。
3. 比较金额和状态。
4. 生成差异记录。
5. 自动修复或人工审核。

### 什么是限流

限流是限制请求速度，防止系统被打垮。

例子：

```text
一个用户每秒最多下单 2 次
一个 IP 每秒最多请求 100 次
一个 SKU 每秒最多进入下单链路 1000 次
```

为什么需要：

- 防攻击。
- 防误操作。
- 防秒杀流量打爆系统。
- 保护数据库和下游服务。

限流不是为了让用户体验变差，而是为了保护大多数正常用户。

### 什么是熔断

熔断是下游服务故障时，调用方停止继续调用，直接快速失败。

如果不熔断：

```text
库存服务很慢
订单服务大量线程等待库存服务
订单服务线程耗尽
整个下单服务被拖垮
```

熔断后：

```text
发现库存服务失败率高
临时停止调用库存服务
订单快速返回失败或进入补偿
保护订单服务线程池
```

熔断状态：

- 关闭：正常调用。
- 打开：直接拒绝。
- 半开：放少量请求探测恢复。

### 什么是降级

降级是非核心能力失败时，用更简单的结果继续服务。

例子：

- 推荐失败，返回默认推荐。
- 评价失败，商品详情不显示评分。
- 营销失败，下单按无优惠继续。

不能降级的例子：

- 价格服务失败不能随便给 0 元。
- 库存服务失败不能假装有库存。
- 支付失败不能假装已支付。

判断标准：

- 错了会不会造成资金损失？
- 错了会不会造成超卖？
- 错了会不会造成安全风险？

如果会，就不能简单降级。

### 什么是可观测性

可观测性是让你知道系统内部发生了什么。

三件套：

- 日志：记录发生了什么。
- 指标：用数字观察系统状态。
- 链路追踪：看一次请求经过哪些服务。

例子：

```text
用户说下单失败
你根据 X-Trace-Id 查日志
发现订单调用库存超时
再看指标发现库存服务 P99 很高
再看数据库发现库存行锁等待严重
```

没有可观测性时，只能猜。

有可观测性时，可以定位。

### 什么是测试

测试是证明代码行为符合预期。

测试分层：

- 单元测试：测一个类或一个方法。
- 集成测试：测代码和数据库、Redis、Kafka 是否能一起工作。
- Smoke 测试：系统启动后，测核心链路是否可用。
- 压测：测系统在高流量下表现。

学习重点：

- 不是只有写完功能才写测试。
- 复杂业务规则应该先想测试用例。
- 只测正常路径不够，还要测失败和重复请求。

### 什么是 Docker

Docker 用容器运行程序。它让不同机器上的运行环境更一致。

不用 Docker 时：

```text
你的机器 MySQL 版本是 A
别人的机器 MySQL 版本是 B
CI 环境又是 C
问题很难复现
```

使用 Docker Compose：

```text
一条命令启动 MySQL、Redis、Kafka、Elasticsearch
```

学习重点：

- 镜像是什么。
- 容器是什么。
- 端口映射是什么。
- volume 是什么。
- environment 是什么。

### 什么是 Kubernetes

Kubernetes 用于生产环境管理容器。

它解决：

- 服务怎么部署。
- 服务挂了怎么重启。
- 服务怎么扩容。
- 服务怎么滚动发布。
- 配置和密钥怎么管理。
- 服务之间怎么访问。

基础对象：

- Deployment：部署应用。
- Service：暴露服务。
- Ingress：入口路由。
- ConfigMap：普通配置。
- Secret：敏感配置。
- HPA：自动扩缩容。
- Probe：健康检查。

零基础阶段不用一开始深入 Kubernetes，但要知道生产部署离不开它或类似平台。
