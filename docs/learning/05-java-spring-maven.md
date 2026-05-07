# Java、Spring Boot 和 Maven

[返回学习手册首页](README.md) | [返回技术能力地图](../technical-skill-map.md)

## Java 基础到工程级 Java

### 你需要掌握什么

Java 语法只是起点。工程级 Java 需要能写出可维护、可测试、可并发运行的业务代码。

必须掌握：

- 类、接口、抽象类、枚举。
- 封装、组合、继承、多态。
- 泛型和集合。
- 异常体系。
- 注解和反射基础。
- Java 17 常用能力。
- 时间 API。
- 并发基础。
- JVM 基础。

### 面向对象设计

面向对象不是为了“多写几个类”，而是为了让业务规则有清晰归属。

示例：

- 订单状态变化应该由订单服务控制。
- 库存数量变化应该由库存服务控制。
- 支付流水应该由支付服务控制。

常见错误：

- 把所有逻辑写在 Controller。
- 业务对象只有 getter/setter，没有行为。
- 多个服务都能随便改订单状态。
- 用 `String` 到处表示状态，导致状态不可控。

推荐做法：

- 状态使用枚举。
- 关键状态变更集中在领域服务或应用服务。
- 参数对象和返回对象要表达业务含义。
- 对外 DTO 和内部领域对象分开。

### 集合和泛型

电商系统大量使用集合：

- 购物车商品列表。
- SKU 库存桶。
- 优惠券列表。
- 订单明细。
- 风控规则集合。

需要理解：

- `ArrayList` 适合按下标读取和追加。
- `HashMap` 适合按 key 快速查找。
- `HashSet` 适合去重。
- `ConcurrentHashMap` 适合并发读写。
- `Queue` 适合排队和缓冲。

常见坑：

- 遍历时修改普通 `ArrayList`。
- 用可变对象作为 `HashMap` key。
- 不理解 `equals` 和 `hashCode`。
- 并发场景使用非线程安全集合。

### 异常体系

工程中异常要分层：

- 参数错误：用户输入不合法。
- 业务错误：库存不足、订单状态不允许取消。
- 下游错误：库存服务不可用、支付渠道超时。
- 系统错误：数据库连接失败、序列化失败。

推荐做法：

- 定义统一错误码。
- 业务异常使用明确的错误码。
- Controller 层统一转换响应。
- 日志记录系统错误，避免把内部堆栈直接暴露给用户。

### Java 17 常用能力

需要熟悉：

- `record`：适合不可变参数对象或简单返回对象。
- `var`：局部变量推断，避免类型过长。
- `switch` 表达式：适合状态转换。
- `Optional`：适合表达可能为空的返回值。
- `Stream`：适合集合转换、过滤、分组。
- `Instant`、`Duration`：适合记录事件时间和超时。

注意：

- `Optional` 不建议作为实体字段。
- Stream 不要写得过度复杂。
- 金额不要用 `double`，要用 `BigDecimal`。
- 时间要明确时区和存储格式。

### 并发基础

电商系统中常见并发场景：

- 多用户同时抢同一个 SKU。
- 支付渠道重复回调。
- 多个实例同时执行补偿任务。
- MQ 多消费者并发消费。

需要掌握：

- 线程池。
- 锁。
- 原子类。
- 并发集合。
- 可见性。
- 幂等设计。

关键认知：

- 单机锁不能解决多实例并发。
- Java 锁不能替代数据库唯一键。
- 并发安全最终要靠数据库约束、幂等表、分布式锁或消息去重兜底。


## Spring Boot 和 Web 开发

### 请求处理流程

一次 HTTP 请求大致经过：

1. 网关路由。
2. Filter。
3. Interceptor。
4. Controller。
5. Service。
6. Repository。
7. 数据库或下游服务。
8. 统一响应返回。

你需要理解每层职责：

- Controller：参数接收、校验、调用业务服务。
- Service：业务规则、事务边界、状态变化。
- Repository：数据库访问。
- Filter：跨请求通用处理，例如 trace ID。
- Exception Handler：统一异常响应。

### 常用注解

必须掌握：

- `@SpringBootApplication`
- `@RestController`
- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@PatchMapping`
- `@DeleteMapping`
- `@RequestBody`
- `@PathVariable`
- `@RequestParam`
- `@Validated`
- `@Service`
- `@Repository`
- `@Configuration`
- `@Bean`
- `@ConfigurationProperties`
- `@Transactional`

学习重点不是背注解，而是知道注解背后的生命周期和职责。

### REST API 设计

好的 API 需要：

- 路径表达资源。
- 方法表达动作。
- 请求体表达业务参数。
- 响应体结构统一。
- 错误码稳定。
- 幂等接口明确幂等键。

示例：

```text
POST /api/orders
GET /api/orders/{orderId}
POST /api/orders/{orderId}/pay
POST /api/orders/{orderId}/cancel
```

常见错误：

- 所有接口都用 `POST /doSomething`。
- 返回结构不统一。
- 业务错误只返回字符串。
- 幂等接口没有 `requestId`。

### 参数校验

需要掌握 Bean Validation：

- `@NotNull`
- `@NotBlank`
- `@Min`
- `@Max`
- `@Positive`
- `@Size`
- `@Valid`

参数校验负责挡住明显非法请求，但不能替代业务校验。

示例：

- 参数校验：购买数量必须大于 0。
- 业务校验：库存是否足够。
- 状态校验：订单是否允许取消。

### 配置管理

Spring Boot 配置来源：

- `application.yml`
- profile 配置。
- 环境变量。
- 命令行参数。
- 配置中心。

生产建议：

- 密码和密钥不要写死在普通配置。
- 本地默认值放 `.env.example`。
- 生产用环境变量或 Secret 覆盖。
- 配置变更需要审计和回滚。


## Maven 多模块工程

### 为什么需要多模块

多模块用于把不同服务和公共能力拆开：

- 每个服务可以独立编译。
- 公共能力放到 `common`。
- 根 POM 统一依赖和插件版本。
- Maven profile 可以按业务组验证。

根工程负责聚合，不直接写业务代码。子模块负责各自服务。

### POM 核心概念

必须掌握：

- `groupId`
- `artifactId`
- `version`
- `packaging`
- `modules`
- `parent`
- `dependencies`
- `dependencyManagement`
- `build`
- `plugins`
- `pluginManagement`
- `profiles`

重点区别：

- `dependencies`：真正引入依赖。
- `dependencyManagement`：只管理版本，不自动引入。
- `plugins`：真正启用插件。
- `pluginManagement`：只管理插件版本和默认配置。

### 生命周期

常用命令：

```powershell
mvn validate
mvn test
mvn package
mvn verify
mvn clean package -DskipTests
mvn verify -DskipITs=false
```

含义：

- `validate`：校验工程和代码规范。
- `test`：编译并执行单元测试。
- `package`：打包。
- `verify`：执行完整验证，通常包括集成测试。
- `-DskipTests`：跳过测试执行，但仍可能编译测试代码。
- `-DskipITs=false`：打开集成测试。

### Surefire 和 Failsafe

约定：

- `*Test.java`：单元测试，由 Surefire 执行。
- `*IT.java`：集成测试，由 Failsafe 执行。

这样做的好处：

- 普通开发可以快速跑单元测试。
- 需要真实环境时再跑集成测试。
- CI 可以分层执行。
