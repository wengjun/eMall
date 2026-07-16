# 工程生产就绪审查问题清单

[文档索引](README.md) | [当前架构审查](architecture-review-issues.md) | [生产检查清单](production-checklist.md) |
[架构设计](architecture.md)

本文记录 2026 年 7 月 11 日对当前工程进行生产级代码审查时发现的问题。审查基线为提交
`dcb59accdcf81661839283abeb0ff8a861510a77`，并记录截至 2026 年 7 月 14 日的修复和验证结果。

## 结论

清单中的 15 个代码和部署配置问题已经完成修复，并由单元测试、行为集成测试、静态清单测试及全量 Maven
构建验证。修复完成不等于已经证明 10 亿用户、1 亿日活和 100 万最高并发；真实 Kubernetes 连通性、
基础设施故障演练、容量压测、安全渗透和长期稳定性测试仍是投产前独立门禁。

| 级别 | 数量 | 含义 |
| --- | ---: | --- |
| P0 | 8 | 上线阻断，可能造成越权、资金错误、重复主键、数据不一致或服务不可用 |
| P1 | 7 | 高风险，可能造成消息丢失、扩展失败、内存耗尽、限流绕过或质量门禁失效 |

## P0 上线阻断问题

### P0-01 身份认证可以被绕过

- 状态：`[x] 已完成`
- 修复记录：实现 BCrypt 凭证哈希、失败锁定、签名访问令牌、刷新令牌轮换与撤销；管理接口统一要求
  平台管理员或精确服务权限，并覆盖错误凭证、暴力破解、匿名管理和权限提升测试。
- 问题：登录接口只接收账号标识和设备号，没有密码、短信验证码、扫码凭证或其他身份因子。权限授予、
  会话撤销、服务客户端注册和商家子账号创建接口也没有管理员认证。
- 影响：知道账号 `subject` 的调用者可以为该账号创建有效会话；匿名调用者还可以修改权限和注册内部客户端。
- 证据：[IdentityService.java](../identity/src/main/java/com/emall/identity/IdentityService.java#L39)、
  [IdentityController.java](../identity/src/main/java/com/emall/identity/IdentityController.java#L31)、
  [IdentityController.java](../identity/src/main/java/com/emall/identity/IdentityController.java#L47)。
- 修复方向：实现密码或验证码认证、凭证哈希、失败锁定、会话轮换和管理员 RBAC；管理接口必须只允许经过
  强认证和审计的管理员调用。
- 验收标准：错误凭证无法登录；匿名用户无法调用管理接口；越权、暴力破解、会话吊销和权限边界均有自动化测试。

### P0-02 业务接口缺少统一认证和资源级授权

- 状态：`[x] 已完成`
- 修复记录：网关与各服务同时校验 Bearer 令牌，清理客户端伪造身份头；公共 API 使用显式白名单，用户资源执行
  owner 校验，运营接口执行角色和权限校验，并覆盖绕过网关、IDOR、伪造请求头和服务权限边界。
- 问题：网关和服务没有完整的 Spring Security 认证链。订单查询、支付、取消以及用户、购物车、售后等接口
  直接信任路径参数或请求头中的用户标识。订单和支付服务默认关闭身份与风控校验。
- 影响：调用者可以读取或修改其他用户的数据，触发其他用户订单支付、取消、售后审批或退款。
- 证据：[OrderController.java](../order/src/main/java/com/emall/order/api/OrderController.java#L56)、
  [CartController.java](../cart/src/main/java/com/emall/cart/api/CartController.java#L28)、
  [AfterSalesController.java](../after-sales/src/main/java/com/emall/aftersales/api/AfterSalesController.java#L43)、
  [order/application.yml](../order/src/main/resources/application.yml#L77)。
- 修复方向：在网关校验访问令牌，在服务端再次验证签名身份；禁止直接信任客户端提供的 `X-Account-Id`；
  对用户资源执行 owner 校验，对运营接口执行角色、权限和审批校验。
- 验收标准：所有非公开 API 都有明确权限策略；服务绕过网关直连时仍不能越权；覆盖 IDOR、角色提升和伪造请求头测试。

### P0-03 支付单和支付回调可以被伪造

- 状态：`[x] 已完成`
- 修复记录：支付单改为读取服务端订单快照并核对归属、金额、币种和状态；回调按渠道验签、校验时间戳和 nonce、
  查询渠道结果，并将订单、支付单、金额、币种、渠道流水精确绑定；生产默认密钥会被运行保护器拒绝。
- 问题：支付创建接口接受客户端提交的订单 ID、用户 ID 和金额，但不向订单服务核对订单归属、状态、币种和
  应付金额。回调 HMAC 使用源码中公开的默认密钥，Kubernetes 运行 Secret 没有覆盖该配置。回调成功后会直接
  调用订单支付，而订单接口也不接收和校验本次实付金额。
- 影响：使用默认配置时，攻击者可以为高金额订单创建低金额支付单，伪造成功回调并将原订单推进到已支付状态。
- 证据：[PaymentService.java](../payment/src/main/java/com/emall/payment/service/PaymentService.java#L320)、
  [PaymentService.java](../payment/src/main/java/com/emall/payment/service/PaymentService.java#L192)、
  [payment/application.yml](../payment/src/main/resources/application.yml#L89)、
  [values.yaml](../ops/helm/emall/values.yaml)。
- 修复方向：支付单只能根据服务端订单快照创建；校验订单、用户、金额、币种和状态；为每个支付渠道使用独立密钥
  或证书并由密钥管理系统下发；回调增加 nonce 防重放、渠道订单查询和来源校验。
- 验收标准：客户端不能覆盖服务端金额；错误金额、币种、渠道、订单归属和签名全部拒绝；默认密钥不能启动生产实例。

### P0-04 退款流程没有调用真实支付渠道

- 状态：`[x] 已完成`
- 修复记录：增加异步退款渠道接口和 `CREATED/PROCESSING/SUCCEEDED/FAILED` 状态机，支持回调、主动查询、
  回调丢失恢复、超时重试、失败回滚和补偿扫描；本地清结算只在渠道确认成功后完成。
- 问题：退款代码创建退款单后直接生成 `local-refund-*` 渠道流水并标记成功，没有调用支付渠道退款接口，也没有
  等待渠道回调或主动查询结果。
- 影响：系统账务可能显示退款成功，但用户没有实际收到资金，随后对账和清结算都会产生错误。
- 证据：[PaymentService.java](../payment/src/main/java/com/emall/payment/service/PaymentService.java#L241)。
- 修复方向：建立独立退款状态机，异步调用渠道、处理回调、主动查询、超时重试和人工介入；本地账务只能在确认
  渠道结果后记账，并通过对账发现长时间不一致。
- 验收标准：成功、失败、处理中、重复退款、回调丢失、渠道超时和对账修复场景均有集成测试。

### P0-05 多副本 Snowflake ID 会重复

- 状态：`[x] 已完成`
- 修复记录：所有服务统一使用自动配置的 Snowflake 生成器，通过 Redis TTL 租约分配 worker ID，并实现续期、
  冲突检测、租约失效停发和时钟回拨保护；覆盖 20 实例并发唯一性、冲突、过期和回拨测试。
- 问题：每个服务把 Snowflake worker ID 写死在代码中，同一服务的所有 Pod 因而使用相同 worker ID。生成器只在
  单个 JVM 内同步，无法协调不同 Pod 的时间戳和序列号。
- 影响：多个副本在同一毫秒生成相同序列时会产生重复订单 ID、支付 ID、事件 ID或其他业务主键。
- 证据：[SnowflakeIdGenerator.java](../common/src/main/java/com/emall/common/id/SnowflakeIdGenerator.java#L23)、
  [OrderConfig.java](../order/src/main/java/com/emall/order/config/OrderConfig.java#L16)、
  [rollout.yaml](../ops/helm/emall/templates/rollout.yaml)。
- 修复方向：通过可靠的 worker 租约服务、StatefulSet ordinal 或统一发号服务分配实例 ID；增加租约续期、冲突检测、
  时钟回拨处理和实例重启隔离。
- 验收标准：至少 20 个并行实例持续生成 ID 时无重复；重复 worker、租约过期、时钟回拨和滚动发布均能安全处理。

### P0-06 分库分表链路不可用

- 状态：`[x] 已完成`
- 修复记录：使用延迟取连接的数据源保证事务内首次访问前完成路由；增加 Redis 全局路由索引及提交/回滚语义，
  为核心实体建立索引；Outbox、补偿和后台任务支持有界跨分片执行，生产清单启用核心服务分片。
- 问题：Kubernetes 配置没有启用分片数据源。即使启用，`@Transactional` 会先建立事务，业务方法随后才设置
  `ShardContext`，数据源路由可能已经固定；用户注册按手机号分片而查询按用户 ID 分片；订单和支付的路由索引
  保存在业务分片中，却在确定目标分片前查询；Outbox、过期任务和补偿任务也没有跨分片扫描能力。
- 影响：当前部署仍是单库；开启分片后会出现数据写入错误库、按 ID 查询不到、事件不发布和补偿任务漏处理。
- 证据：[ShardRoutingAutoConfiguration.java](../common/src/main/java/com/emall/common/sharding/ShardRoutingAutoConfiguration.java#L39)、
  [UserService.java](../user/src/main/java/com/emall/user/service/UserService.java#L38)、
  [OrderService.java](../order/src/main/java/com/emall/order/service/OrderService.java#L332)、
  [OutboxPublisherSupport.java](../common/src/main/java/com/emall/common/outbox/OutboxPublisherSupport.java#L53)。
- 修复方向：在事务开始前确定分片；统一实体的主分片键；把全局路由索引放入独立路由存储；Outbox 和后台任务按
  数据库分片并行执行；明确跨分片查询和迁移策略。
- 验收标准：真实多数据源集成测试能够验证创建、按 ID 查询、更新、Outbox、补偿、扩容和迁移，且不会访问默认错误分片。

### P0-07 下单 Saga 和资源补偿不完整

- 状态：`[x] 已完成`
- 修复记录：增加持久化下单 Saga 和独立事务状态更新，远程调用前记录进行中状态，进程恢复后按幂等键继续或补偿；
  优惠券和库存预占增加租约、过期回收和兜底扫描，并覆盖超时、崩溃、重复请求和不确定结果。
- 问题：下单先预占优惠券，再执行风控和库存，最后才保存订单。风控拒绝、库存调用结果不确定、订单数据库写入失败
  或 Outbox 写入失败时，没有持久化 Saga 状态保证释放优惠券和库存。优惠券预占没有独立超时时间和自动回收任务。
- 影响：优惠券可能永久被锁定，库存可能泄漏；重试后还可能出现优惠券、库存和订单状态不一致。
- 证据：[OrderCreateWorkflow.java](../order/src/main/java/com/emall/order/workflow/OrderCreateWorkflow.java#L54)、
  [Coupon.java](../marketing/src/main/java/com/emall/marketing/domain/Coupon.java#L27)、
  [MarketingService.java](../marketing/src/main/java/com/emall/marketing/service/MarketingService.java#L70)。
- 修复方向：先完成身份、风控和价格检查，再建立可持久化的订单/Saga 记录；为每个远程步骤记录状态、幂等键、
  补偿动作和重试时间；优惠券和库存预占都必须有租约和兜底扫描。
- 验收标准：在每个步骤注入超时、进程崩溃、数据库异常和重复请求后，系统最终收敛且没有资源泄漏。

### P0-08 Kubernetes 服务间网络和 Istio 授权配置错误

- 状态：`[x] 已完成`
- 修复记录：命名空间保持严格 mTLS，网关外部入口端口单独允许明文接入，AuthorizationPolicy 改为按目标工作负载
  最小授权；补齐 ServiceAccount、NetworkPolicy、监控抓取和固定 Dubbo `20880` 端口的一致配置及静态清单测试。
- 问题：名为“允许网关访问公共 API”的 Istio 策略实际选择了网关工作负载，并只允许网关身份入站到网关自身；
  另一个无 selector 的 ALLOW 策略会作用于命名空间内全部工作负载。生产配置同时选择 Dubbo 并使用随机端口 `-1`，
  NetworkPolicy 却只开放固定 HTTP 端口。
- 影响：启用 Istio 和 NetworkPolicy 后，公网请求或网关到服务的请求会被拒绝，Dubbo 服务发现成功后也无法建立连接。
- 证据：[service-mesh.yaml](../ops/helm/emall/templates/service-mesh.yaml)、
  [values.yaml](../ops/helm/emall/values.yaml)、
  [network-policy.yaml](../ops/helm/emall/templates/network-policy.yaml)、
  [inventory/application.yml](../inventory/src/main/resources/application.yml#L69)。
- 修复方向：按目标工作负载编写入站 AuthorizationPolicy；增加显式默认拒绝和最小允许矩阵；为 Dubbo 分配固定端口，
  在容器、Service、NetworkPolicy 和 Istio 中一致声明。
- 验收标准：在真实 Kubernetes 测试环境验证 ALB 到网关、网关到服务、订单到库存/价格/营销以及支付到订单的连通性，
  同时证明未授权服务间调用被拒绝。

## P1 高风险问题

### P1-01 消息失败和死信状态会被事务回滚

- 状态：`[x] 已完成`
- 修复记录：业务消费和失败计数使用独立事务，`FAILED/DEAD` 更新原子化；达到阈值后交给 Kafka DLT，重复投递
  由数据库幂等记录阻止副作用，并增加真实 MySQL 事务集成测试。
- 问题：消息模板捕获业务异常后写入 `FAILED/DEAD`，随后重新抛出异常；多个 Kafka 监听方法带有
  `@Transactional`，因此失败次数和死信状态会随监听事务一起回滚，模板配置的最大重试次数无法可靠生效。
- 影响：毒消息可能被反复消费，也可能由 Kafka 默认错误处理器提交后丢失，但数据库中没有可追踪的死信记录。
- 证据：[MessageConsumerTemplate.java](../common/src/main/java/com/emall/common/messaging/MessageConsumerTemplate.java#L43)、
  [OrderEventConsumer.java](../fulfillment/src/main/java/com/emall/fulfillment/messaging/OrderEventConsumer.java#L33)、
  [PaymentEventConsumer.java](../order/src/main/java/com/emall/order/messaging/PaymentEventConsumer.java#L30)。
- 修复方向：明确由 Kafka DLT 还是数据库状态负责重试；失败记录使用独立事务，或交给配置明确的
  `DefaultErrorHandler` 和 DLT 发布器；业务更新与幂等记录必须保持原子性。
- 验收标准：连续失败达到阈值后只进入一次 DLT，数据库状态为 DEAD；服务重启和重复投递不会重复执行业务副作用。

### P1-02 Migration Runner 无法覆盖生产数据库

- 状态：`[x] 已完成`
- 修复记录：Migration Runner 不再聚合业务 SQL；37 个数据库服务分别构建只包含自身脚本的不可变迁移镜像，Helm
  为每个服务生成独立 Job、ExternalSecret 和启动门禁。缺目录、零脚本、越权数据库 URL 和不安全 DDL 均立即失败。
- 问题：通用 Dockerfile 只复制应用 JAR，Migration Runner 却从 `/migrations/{service}` 读取脚本，清单没有挂载该目录。
  Job 只列出 9 个服务，而生产配置为更多服务声明了独立数据库。
- 影响：迁移任务可能失败或零迁移成功退出，大量服务启动后缺少数据表；数据库版本与应用版本无法可靠对应。
- 证据：[Dockerfile.migration](../Dockerfile.migration)、
  [migration-job.yaml](../ops/helm/emall/templates/migration-job.yaml)、
  [migration-runner/application.yml](../migration-runner/src/main/resources/application.yml)。
- 修复方向：把全部迁移脚本作为不可变镜像资产打包，或显式挂载版本化制品；缺少目录和零迁移必须失败；维护完整服务清单，
  并在应用发布前执行和验证迁移 Job。
- 验收标准：空数据库可由生产镜像一次性初始化全部 schema；重复执行幂等；漏脚本、校验和变化和部分失败会阻断发布。

### P1-03 大量接口执行无分页全表查询

- 状态：`[x] 已完成`
- 修复记录：删除生产代码中的 `selectList(null)`，统一使用 `BoundedQuery` 和 MyBatis-Plus 最大页大小；汇总计数下推
  数据库，搜索和跨分片扫描均设置游标、页大小或扇出上限。
- 问题：事件、分析、数据仓库、客服、流量、智能、可靠性等仓储大量使用 `selectList(null)`。事件平台汇总接口还会
  两次加载全部事件对象后在 JVM 中计数。
- 影响：数据增长后会造成慢查询、数据库带宽放大、长时间 GC 和 OOM，无法支撑亿级数据。
- 证据：[MybatisPlusEventPlatformRepository.java](../event-platform/src/main/java/com/emall/eventplatform/MybatisPlusEventPlatformRepository.java#L88)、
  [EventPlatformService.java](../event-platform/src/main/java/com/emall/eventplatform/EventPlatformService.java#L82)、
  [MybatisPlusAnalyticsRepository.java](../analytics/src/main/java/com/emall/analytics/MybatisPlusAnalyticsRepository.java#L42)。
- 修复方向：列表接口统一使用游标或主键翻页并限制最大页大小；汇总使用 SQL 聚合、预聚合表或 OLAP；增加归档和冷热分层。
- 验收标准：业务 API 不允许无界返回集合；千万级基准数据下查询计划、响应时间和内存占用满足预算。

### P1-04 订单提交限流是单机且内存无界

- 状态：`[x] 已完成`
- 修复记录：订单提交保护改为使用 Redis Lua、Redis TIME 和 TTL 的跨实例窗口；本地降级采用有界缓存，生产环境
  Redis 异常时 fail-closed，并覆盖跨实例共享限额和高基数键回收测试。
- 问题：每个订单 Pod 使用本地 `ConcurrentHashMap` 保存用户计数，条目不会主动过期或删除。
- 影响：请求分散到不同 Pod 后可以绕过限制；用户键持续增长最终会占满堆内存。
- 证据：[OrderSubmissionGuard.java](../order/src/main/java/com/emall/order/service/OrderSubmissionGuard.java#L15)。
- 修复方向：使用 Redis Cluster 和 Lua 实现分布式令牌桶或滑动窗口；所有 key 必须有 TTL；本地层只做有界快速保护。
- 验收标准：跨 Pod 并发测试不能超过全局限额；高基数用户压测后 Redis 和 JVM 内存能够自动回收。

### P1-05 网关限流键可以被客户端绕过

- 状态：`[x] 已完成`
- 修复记录：限流键只使用已验证 Principal、可信代理解析出的源 IP、规范化路由和受限热点维度；实现全站、主体/IP
  与热点资源分层限流，拒绝非法 IP，覆盖伪造 XFF 和可变业务头绕过测试。
- 问题：当前没有认证 Principal，限流用户通常为 `anonymous`；限流键又包含客户端可控制的设备、渠道、区域、SKU、
  campaign 和 `X-Forwarded-For`。攻击者可以不断更换这些字段生成新 key。
- 影响：恶意流量可以绕过单 key 限流，并制造大量 Redis key，反向形成缓存和网关压力。
- 证据：[RateLimitConfig.java](../gateway/src/main/java/com/emall/gateway/config/RateLimitConfig.java#L27)。
- 修复方向：由可信认证结果生成用户 ID；入口代理清理外部转发头并写入可信源 IP；限流采用 IP、账号、设备和业务热点的
  分层规则，限制可变维度和 key 数量。
- 验收标准：伪造转发头和业务头不能绕过限流；匿名、登录用户、热点 SKU 和全站保护均有独立压测。

### P1-06 生产 Secret 和运行保护器不安全

- 状态：`[x] 已完成`
- 修复记录：删除可部署静态 Secret 和仓库内 Vault 占位端点，ExternalSecret 只引用平台预置的
  `ClusterSecretStore/emall-platform-vault`；生产保护器默认校验凭证强度、密钥、鉴权、分片、信任、支付 HTTPS、
  Redis 和固定 RPC 端口，任一不安全配置均启动失败。
- 问题：可部署清单中包含 `root/root`、`replace-in-production` 等占位凭证。生产保护器默认不启用，并且只拒绝空值
  或以 `local-dev-` 开头的值，所以这些占位值会被接受。静态 Secret 与 External Secrets 还可能形成双重来源。
- 影响：误用清单时会以弱凭证和公开密钥启动生产服务，造成数据库、字段加密和内部运维接口失守。
- 证据：[values.yaml](../ops/helm/emall/values.yaml)、
  [ProductionRuntimeGuard.java](../common/src/main/java/com/emall/common/runtime/ProductionRuntimeGuard.java#L23)、
  [runtime-secret.yml](../ops/k8s/external-secrets/runtime-secret.yml#L18)。
- 修复方向：删除仓库中的可部署静态 Secret，只保留 External Secrets 或云 KMS 引用；生产模式默认开启保护器；
  拒绝已知占位值、默认账号和不足强度的密钥；为关键密钥建立版本和轮换机制。
- 验收标准：缺少任意生产 Secret、使用默认值或弱密钥时 Pod 必须启动失败；仓库和镜像扫描不包含生产凭证。

### P1-07 CI 和测试提供了错误的生产信心

- 状态：`[x] 已完成`
- 修复记录：CI 同时监听 `master` 的 push 和 pull request，显式运行格式、静态检查、单元测试及
  `-DskipITs=false -Demall.integration.require-docker=true verify`，并增加迁移资产和 Trivy 门禁；删除 41 个源码字符串
  伪集成测试，补充 MySQL、Redis、Kafka、分片、ID、Saga、支付和清单行为测试。
- 问题：仓库当前使用 `master`，GitHub Actions 只监听 `main` 的 push。根 POM 默认 `skipITs=true`，因此 CI 的
  `mvn verify` 不执行 Failsafe 集成测试。多个 `ModuleIntegrationTest` 只是读取源码并断言字符串存在，不验证运行行为。
- 影响：代码推送可能不触发 CI；数据库、Kafka、Redis、分片、多副本和 Kubernetes 错误可以在流水线全绿时进入主分支。
- 证据：[ci.yml](../.github/workflows/ci.yml#L3)、[pom.xml](../pom.xml#L25)、
  [ModuleIntegrationTest.java](../order/src/test/java/com/emall/order/service/ModuleIntegrationTest.java#L20)。
- 修复方向：统一默认分支；CI 显式执行单元测试和 `-DskipITs=false verify`；增加真实 MySQL、Redis、Kafka、服务间调用、
  分片、多副本 ID 和失败补偿测试；增加覆盖率、安全依赖和镜像扫描门禁。
- 验收标准：主分支每次提交都触发完整流水线；关键交易和基础设施测试不能被默认跳过；故意引入已知缺陷时流水线会失败。

## 修复结果

1. P0-01 至 P0-04：身份、授权、支付和退款资金入口已收紧。
2. P0-05 至 P0-07：多副本 ID、分片路由、Saga 和资源回收已实现并有自动化测试。
3. P0-08、P1-01 至 P1-07：服务网络、消息、迁移、查询边界、限流、密钥和 CI 门禁已修复。
4. 真实集群连通性、容量、安全和灾备验证继续作为发布条件管理，不以本清单的代码完成标记替代。

## 本次验证记录

- `mvn clean verify -DskipITs=false`：44 个 Reactor 项目全部成功，编译、Checkstyle、单元测试、Failsafe 和 JaCoCo
  门禁通过；真实环境 Smoke 用例因未设置显式开关而跳过。
- `mvn formatter:validate`：44 个模块全部通过。
- Migration Runner 构建产物不包含业务 SQL；37 个服务迁移目录、独立镜像构建输入和 Helm Job/Secret 映射的完整性测试通过。
- 当前 PowerShell 无法使用 Docker，因此本地 Testcontainers 用例按设计跳过；CI 已设置
  `-Demall.integration.require-docker=true`，Docker 不可用时会直接失败而不是产生假绿。
- 当前未连接真实 Kubernetes 集群，ALB、mTLS、NetworkPolicy、Dubbo 和跨服务连通性仅完成清单静态测试；
  上线前仍必须在预生产集群执行连通性、故障注入、渗透、容量和恢复演练。
