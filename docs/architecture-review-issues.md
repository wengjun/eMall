# 工程架构设计审查问题清单

[文档索引](README.md) | [架构设计](architecture.md) | [生产检查清单](production-checklist.md) |
[历史生产就绪审查](project-review-issues.md)

本文记录 2026 年 7 月 15 日对当前工程进行生产架构审查时发现的问题。审查基线为提交
`a5fabebd4a0a2e254c32df1d34686d9a4c527bd2`，目标是判断当前设计是否能够支撑 10 亿注册用户、
1 亿日活和 100 万峰值并发，而不是判断代码能否在单机开发环境启动。

本次结论来自静态代码、配置和部署资产审查。没有执行生产规模压测、真实 Kubernetes 故障演练、
多区域切换、安全渗透或长期稳定性测试，因此问题完成后仍需通过独立的生产验证门禁。

## 审查结论

工程已经实现幂等、Outbox、Saga、分库分表、限流、熔断、降级、服务发现、可观测性等基础机制，
但当前仍属于生产架构能力实现和演示，不能据此认定已经具备十亿用户和百万并发承载能力。

| 级别 | 数量 | 当前状态 | 含义 |
| --- | ---: | --- | --- |
| P0 | 8 | 6 项已完成，2 项代码完成并等待外部验收 | 上线阻断，可能造成库存错误、查询结果错误、数据不一致、数据库连接耗尽或扩容失败 |
| P1 | 5 | 5 项代码与自动化修复已完成 | 高风险，容量目标仍必须由预生产证据验证，不能仅凭代码宣称达标 |

## P0 上线阻断问题

### P0-01 库存主表和库存桶形成两套事实来源

- 状态：`[~] 代码修复完成，待 MySQL 容器复验`
- 修复记录：已引入 `SINGLE_ROW/BUCKETED` 模式与乐观版本，模式切换时把主表变为历史基础条带，库存查询统一
  聚合基础条带和桶条带；补货改为请求 ID 幂等和数据库原子累加；补货、模式切换、预占、确认和释放均追加同事务
  库存流水，V10 为存量数据建立迁移基线。12 项库存单元/并发测试已通过，MySQL 8.4 集成测试已加入
  `InventoryRepositoryIT`；当前执行环境无 Docker CLI，容器测试未执行，最终强制集成验收后再改为 `[x]`。
- 问题：`addStock` 和库存查询只操作 `inventory_item`；初始化库存桶时把主表可用库存复制到
  `inventory_bucket`，却没有清零主表、记录库存模式或执行原子所有权切换。只要存在库存桶，预占、确认和释放
  就只操作桶表。`saveItem` 还是先读后写的绝对值覆盖，并发补库存可能丢失更新；补库存接口也没有业务幂等键。
- 影响：热点 SKU 开启分桶后，新补充的库存不会进入可售桶；主库存查询不会反映桶内预占和销售；并发补货可能
  少记或重复记账。库存可售量、已售量和运营查询会长期分叉。
- 证据：[InventoryService.java](../inventory/src/main/java/com/emall/inventory/service/InventoryService.java#L64)、
  [InventoryService.java](../inventory/src/main/java/com/emall/inventory/service/InventoryService.java#L70)、
  [InventoryService.java](../inventory/src/main/java/com/emall/inventory/service/InventoryService.java#L85)、
  [InventoryService.java](../inventory/src/main/java/com/emall/inventory/service/InventoryService.java#L207)、
  [MybatisPlusInventoryRepository.java](../inventory/src/main/java/com/emall/inventory/repository/MybatisPlusInventoryRepository.java#L35)。
- 修复方向：为 SKU 建立明确的 `SINGLE_ROW/BUCKETED` 库存模式和版本；以库存流水作为审计依据；初始化分桶、
  模式切换和补货必须在单事务中原子执行；分桶后所有库存读写统一聚合或只访问桶表；补货使用请求 ID 幂等和
  `total = total + quantity` 原子更新。
- 验收标准：覆盖分桶前后补货、重复补货、并发补货、预占、确认、释放、扩桶和模式切换；任何时刻主账、桶账和
  库存流水满足总量守恒，API 查询结果与实际可售量一致。

### P0-02 默认 Elasticsearch 搜索实现会遗漏正确结果

- 状态：`[~] 代码修复完成，待 Elasticsearch 容器与百万文档复验`
- 问题：生产默认搜索引擎是 Elasticsearch，但实现只读取索引第一页的 `limit * 5` 条文档，再在 JVM 内完成
  关键词过滤、可售过滤、排序和截断。读取第一页时没有稳定排序，也没有真正构造 Elasticsearch 查询。
- 影响：相关商品不在任意第一页时永远无法被搜索到；商品规模增长后结果既不完整也不稳定，同时浪费应用内存和
  网络带宽，无法提供分词、相关度、聚合、纠错和深分页能力。
- 证据：[application.yml](../search/src/main/resources/application.yml#L64)、
  [ElasticsearchSearchRepository.java](../search/src/main/java/com/emall/search/repository/ElasticsearchSearchRepository.java#L35)、
  [ElasticsearchSearchRepositoryTest.java](../search/src/test/java/com/emall/search/repository/ElasticsearchSearchRepositoryTest.java#L33)。
- 修复方向：使用 Elasticsearch 原生 Query DSL 或 Spring Data 查询构造器，在 ES 内执行分词、布尔过滤、相关度
  排序、字段权重和聚合；使用 `search_after` 和 PIT 实现稳定深分页；明确索引 mapping、中文分析器、别名切换、
  重建和回滚方案。
- 验收标准：在百万级以上商品索引中验证召回率、排序稳定性、P95/P99、分页无重无漏、索引重建和事件乱序；
  自动化测试不得再通过 mock `findAll(PageRequest)` 固化错误实现。
- 修复记录：已将关键词检索、可售过滤、字段权重和稳定排序下推到 Elasticsearch；深分页使用带 HMAC 签名的
  PIT + `search_after` 游标，并在终页和异常路径关闭 PIT；已增加版本化索引、限速重建、数量校验、读写别名
  原子切换和回滚接口。文档写入使用 Elasticsearch `external` 版本控制，乱序或重复事件无法覆盖新版本。
  单元测试覆盖查询 DSL、游标篡改、索引生命周期和乱序拒绝；Testcontainers 集成测试覆盖万级默认数据，并可通过
  `emall.search.it.document-count=1000000` 扩展到百万文档。当前机器无可用 Docker，真实 Elasticsearch、百万数据
  性能和中文 IK 分词验收尚未执行，因此暂不标记为完全完成。

### P0-03 请求和后台任务会同步扇出到全部物理分片

- 状态：`[x] 已完成`
- 问题：`executeAll` 在调用线程中逐个遍历全部物理库表。默认 16 个数据库分片和 64 个表分片会形成 1024 个
  物理目标。商品搜索在缓存未命中时直接执行全分片查询；订单 Saga 恢复任务每 10 秒执行一次全分片扫描。
  Saga 任务锁租期固定为 30 秒，锁实现没有续租能力。
- 影响：一次用户请求可能放大为 1024 次数据库访问；后台任务会持续制造周期性扫描峰值。任务运行超过锁租期时，
  其他 Pod 可以再次获得锁并与旧任务重叠执行，放大数据库压力和补偿竞争。
- 证据：[DefaultShardRoutingOperations.java](../common/src/main/java/com/emall/common/sharding/DefaultShardRoutingOperations.java#L36)、
  [DefaultShardRoutingOperations.java](../common/src/main/java/com/emall/common/sharding/DefaultShardRoutingOperations.java#L69)、
  [ProductService.java](../product/src/main/java/com/emall/product/service/ProductService.java#L94)、
  [OrderSagaRecoveryJob.java](../order/src/main/java/com/emall/order/saga/OrderSagaRecoveryJob.java#L12)、
  [MybatisPlusDistributedTaskLock.java](../common/src/main/java/com/emall/common/task/MybatisPlusDistributedTaskLock.java#L23)。
- 修复方向：在线搜索只走搜索索引，禁止请求级全分片扫描；后台任务按物理分片建立可续租的分区租约、游标和检查点，
  由多个 Worker 有界并行消费；每轮限制扫描分片数和记录数，并设置超时、背压、公平性和积压指标。
- 验收标准：在线 API 的数据库扇出数有明确上限；后台任务可在 1024 个物理分片上水平扩展且不会重复持有同一分区；
  注入慢分片、锁过期、Worker 崩溃和重启后，任务不会重入执行副作用并最终处理完积压。
- 修复记录：已删除 `ShardRoutingOperations.executeAll` 及全工程全部同步全分片调用。商品服务不再提供数据库搜索，
  生产搜索强制使用 Elasticsearch；本地 JDBC 搜索仅执行单库查询。新增 `PartitionedShardWorkCoordinator`，按任务维护
  轮转游标，每轮最多访问 8 个物理分片，每个分片使用独立数据库租约并由后台心跳续租，失租后在下一条副作用前
  立即终止。库存过期、订单补偿、Saga 恢复、优惠券过期和支付补偿均使用该执行器；Outbox 使用记录级 claim 租约和
  有界 8 分片轮转。已增加分片获得、繁忙、失租和批量大小指标。自动化测试验证 1024 分片每轮扇出不超过 8、
  128 轮无遗漏覆盖全部分片、慢任务续租、续租失败中止，以及各业务任务遵守全局批量上限。

### P0-04 每个服务实例创建过多数据库连接池

- 状态：`[x] 已完成`
- 问题：分片数据源为每个数据库分片创建独立 Hikari 连接池。默认 16 个数据库分片、每池最大 32 个连接，意味着
  单个 Pod 的连接上限为 512。配置没有设置全局连接预算，也没有按流量动态调整各分片池的最小空闲和最大连接数。
- 影响：服务副本扩容会线性放大数据库连接。仅订单服务 HPA 最大 200 个副本时，理论连接上限就达到 102400，
  MySQL 会在应用达到目标吞吐前因连接、线程和内存耗尽而失效。
- 证据：[ShardRoutingAutoConfiguration.java](../common/src/main/java/com/emall/common/sharding/ShardRoutingAutoConfiguration.java#L54)、
  [ShardRoutingAutoConfiguration.java](../common/src/main/java/com/emall/common/sharding/ShardRoutingAutoConfiguration.java#L86)、
  [ShardDataSourceProperties.java](../common/src/main/java/com/emall/common/sharding/ShardDataSourceProperties.java#L22)、
  [hpa.yaml](../ops/helm/emall/templates/hpa.yaml)。
- 修复方向：建立服务、Pod、数据库实例和全局四级连接预算；显式设置较小的 `minimumIdle` 和按分片负载计算的
  `maximumPoolSize`；评估懒加载/可回收分片连接池、数据库代理或按 Cell 部署只连接本 Cell 分片；HPA 同时受
  数据库连接和下游容量约束。
- 验收标准：在最大计划副本数下，所有 Pod 的连接总量不超过数据库预算；冷分片不会维持大量空闲连接；扩缩容、
  数据库故障和连接泄漏测试能够证明连接数收敛且不会形成重连风暴。
- 修复记录：分片连接池默认改为 `maximumPoolSize=4`、`minimumIdle=0` 并按首次访问延迟创建；新增 Pod、数据库实例、
  计划副本和全局四级预算以及 20% 容量余量，任何超预算配置都会在启动阶段失败。所有启用分片数据源的原生 HPA
  上限已从 150 至 300 收敛为 10，并与 Helm 规划副本一致；连接池暴露活跃连接、利用率和已初始化冷分片数指标。
  自动化测试覆盖预算拒绝、32 路并发首次访问只创建一个池、冷池关闭不初始化、已初始化池幂等关闭和真实 H2 分片
  路由，相关 8 项测试已通过。

### P0-05 本地数据库事务跨越远程调用且 Saga 状态存在覆盖竞争

- 状态：`[x] 已完成`
- 问题：订单创建、支付和取消方法在本地 `@Transactional` 事务中同步调用定价、营销、库存等远程服务。
  下单过程中 Saga 状态又通过 `REQUIRES_NEW` 反复打开独立事务。Saga 持久化更新只按 `saga_id` 覆盖，没有版本号、
  期望阶段或期望状态条件。
- 影响：下游延迟和重试会长期占用数据库连接，产生连接池饥饿和事务超时；外层事务和 Saga 独立事务同时占用连接。
  在线请求、超时重试和恢复任务并发时，旧状态可能覆盖新状态，导致已经完成的 Saga 被重新补偿或状态回退。
- 证据：[OrderService.java](../order/src/main/java/com/emall/order/service/OrderService.java#L124)、
  [OrderService.java](../order/src/main/java/com/emall/order/service/OrderService.java#L185)、
  [OrderCreateWorkflow.java](../order/src/main/java/com/emall/order/workflow/OrderCreateWorkflow.java#L62)、
  [OrderSagaStateService.java](../order/src/main/java/com/emall/order/saga/OrderSagaStateService.java#L18)、
  [MybatisPlusOrderSagaRepository.java](../order/src/main/java/com/emall/order/saga/MybatisPlusOrderSagaRepository.java#L24)。
- 修复方向：把编排拆成短事务：先持久化意图和 Saga 状态，事务外调用下游，再用短事务提交订单与 Outbox；每个状态
  迁移使用版本号或 `WHERE status = expected AND stage = expected` 的 CAS；恢复 Worker 先领取 Saga 租约再执行；
  对未知远程结果使用查询确认而不是盲目补偿。
- 验收标准：数据库事务中不包含网络调用；连接占用时间有指标和预算；并发在线请求、恢复任务、重复回调和服务崩溃
  不会造成状态回退，所有 Saga 最终进入完成、已补偿或人工处理状态。
- 修复记录：订单创建、支付、取消、重试及支付确认编排已移除外层数据库事务；订单、路由、支付确认和 Outbox 仅在
  3 秒本地短事务中提交，并暴露 `emall_order_local_transaction_duration` 时长指标。Saga 表新增单调 `version`，所有
  更新按前一版本执行 CAS，重复创建返回胜出记录，旧 Worker 无法覆盖新状态；恢复任务在分区租约之外还会领取单
  Saga 租约。库存与优惠券补偿新增只读状态查询，未知结果先确认，已消费或查询失败不会盲目释放。自动化测试覆盖
  编排入口无事务、短事务边界与指标、数据库和内存 CAS 冲突、重复开始、恢复租约及查询失败转人工处理，相关模块
  共 24 项定向测试和 171 项完整回归测试通过。

### P0-06 Redis 被用作永久且唯一的全局分片路由目录

- 状态：`[x] 已完成（2026-07-15）`
- 问题：订单、请求、支付、退款、库存预占等全局查找键通过无过期时间的 Redis 键保存。分片模式下缺少 Redis 路由
  就直接返回不存在。订单和支付虽然写入数据库路由表，但相应查询方法未接入回退链路，而且路由表自身位于业务分片，
  在不知道分片时无法直接定位。
- 影响：Redis 淘汰、集群数据丢失、错误清理或迁移不完整会使仍存在的历史业务数据无法访问。每笔交易产生多个永久
  路由键，随着订单量增长会形成数十亿级高成本 Redis 数据集，并使 Redis 成为核心写事务的强依赖和单独故障域。
- 证据：[ShardRouteIndex.java](../common/src/main/java/com/emall/common/sharding/ShardRouteIndex.java#L66)、
  [ShardRouteIndex.java](../common/src/main/java/com/emall/common/sharding/ShardRouteIndex.java#L89)、
  [ShardRouteIndex.java](../common/src/main/java/com/emall/common/sharding/ShardRouteIndex.java#L110)、
  [OrderCreateWorkflow.java](../order/src/main/java/com/emall/order/workflow/OrderCreateWorkflow.java#L112)、
  [OrderService.java](../order/src/main/java/com/emall/order/service/OrderService.java#L359)、
  [MybatisPlusOrderRepository.java](../order/src/main/java/com/emall/order/repository/MybatisPlusOrderRepository.java#L78)。
- 修复方向：建立持久化、可复制、可重建并带版本的全局路由目录，Redis 只作为缓存；或者在业务 ID 中编码稳定虚拟分片。
  请求幂等路由键按业务保留期设置 TTL，长期实体路由写入专用 KV/数据库；提供全量重建、增量校验、双写迁移和
  Redis 丢失恢复工具。
- 验收标准：清空 Redis 后仍可查询全部历史订单和支付；路由缓存可从权威存储重建；路由目录容量、延迟、可用性和
  灾备有明确预算；迁移期间新旧路由版本均可正确读取且不存在静默错分片。
- 修复记录：新增独立 `routing` 服务和 `global_shard_route` 持久化目录，路由记录带版本、所有权冲突检查、过期时间与
  CAS 删除栅栏；`ShardRouteIndex` 改为 Redis 有限 TTL 缓存，未命中、损坏或 Redis 故障时回源持久目录并自动回填。
  订单、支付和库存请求类路由分别设置有限业务保留期，生产守卫强制所有分片服务配置持久目录端点。新增带游标的
  分批缓存重建接口，支持清空 Redis 后从目录恢复；Compose、Helm、Kubernetes、数据库初始化和迁移资产均已接入
  `routing`。部署采用 3 个基础副本、PDB 至少 2 个可用实例和最多 10 个副本，告警预算为可用性 99.9%、P99 50 ms。
  HTTP 404、409、错误请求和真实下游故障具有独立语义，MyBatis-Plus/H2 持久化、CAS、游标分页、Redis 故障回源、
  有限保留期和缓存重建测试均通过；MySQL Testcontainers 用例已加入，当前环境没有可用 Docker CLI，因此自动跳过。

### P0-07 固定取模分片无法在线扩容

- 状态：`[x] 已完成（2026-07-15）`
- 问题：数据库和表路由直接根据当前分片数量执行取模和整除。调整数据库分片数、表分片数、区域列表或 Cell 列表时，
  大量已有业务键会映射到新的物理位置。工程中未形成路由版本、迁移状态机、双读双写、数据回填、校验、切流和回滚闭环。
- 影响：不能通过简单修改配置增加分片；错误扩容会导致历史数据查询不到、同一实体在新旧分片重复创建、消息和补偿任务
  访问错误位置。区域或 Cell 调整也可能瞬间改变写入所有权。
- 原证据：旧 `HashModShardRouter`、
  [DefaultShardRoutingOperations.java](../common/src/main/java/com/emall/common/sharding/DefaultShardRoutingOperations.java)、
  [OwnershipGuard.java](../common/src/main/java/com/emall/common/region/OwnershipGuard.java)。
- 修复方向：使用数量固定且足够多的虚拟分片，把虚拟分片到物理数据库、表、区域和 Cell 的映射放入版本化控制面；
  实现准备、复制、校验、双读、切写、观察、清理和回滚状态机；使用 epoch/fencing token 阻止旧所有者继续写入。
- 验收标准：能够在持续读写流量下把指定虚拟分片迁移到新节点；迁移前后无数据丢失、重复或错路由；失败时可回滚，
  并通过校验和、行数、业务不变量和增量日志证明数据一致。
- 修复记录：删除直接取模物理拓扑的 `HashModShardRouter`，统一使用 4096 个固定虚拟分片；虚拟分片到数据库、表、Region
  和 Cell 的映射由 `routing` 服务持久化管理，包含 `mappingVersion`、`epoch`、当前主位置、迁移目标和迁移状态。迁移状态机
  严格限制为准备、复制、CDC 追赶、校验、延迟切写、观察、清理和稳定状态，每步采用数据库 CAS 和不可变审计记录。
  行数、校验和、复制游标和 CDC 延迟不满足时禁止切流；切流与跨 Region 回滚前至少等待两个映射缓存周期并停止写入，
  旧映射写请求在缓存过期或控制面异常时故障关闭，只读请求才允许在限定时间内使用陈旧映射。Region/Cell 所有权已改为
  读取同一份虚拟分片放置记录，不再独立取模。核心业务查询已显式切换到 `executeRead`，写操作继续走带栅栏的 `execute`。
  Flyway/H2/MyBatis 集成测试覆盖完整切流、CAS 冲突、证据门禁、epoch 递增、切流前回滚和审计链路；HTTP 测试覆盖控制面
  故障时的读降级、写关闭和切流栅栏，14 个相关 Reactor 模块的生产及测试代码已通过编译。

### P0-08 事件契约缺少安全演进和聚合顺序保证

- 状态：`[x] 已完成（2026-07-15）`
- 问题：核心事件使用 `Map<String, Object>` 作为载荷，没有 schema 版本、生产者版本和聚合版本。消费者通过字符串
  字段名和运行时类型转换解析消息。Outbox 批次会并行发送，多 Pod 也可以同时领取同一分片中的不同事件；失败旧事件
  可能在新事件之后重试。
- 影响：字段重命名、类型变化或删除只能在运行时暴露；旧消费者可能直接失败。相同聚合的消息乱序会使搜索索引、订单
  状态和分析数据回退。仅按 `eventId` 去重不能判断事件新旧，也不能阻止旧事件覆盖新状态。
- 证据：[DomainEvent.java](../common/src/main/java/com/emall/common/event/DomainEvent.java#L6)、
  [OutboxEvent.java](../common/src/main/java/com/emall/common/event/OutboxEvent.java#L6)、
  [OutboxPublisherSupport.java](../common/src/main/java/com/emall/common/outbox/OutboxPublisherSupport.java#L68)、
  [ProductEventConsumer.java](../search/src/main/java/com/emall/search/messaging/ProductEventConsumer.java#L50)。
- 修复方向：定义稳定的事件信封和强类型事件 DTO，至少包含 `eventType`、`schemaVersion`、`aggregateId`、
  `aggregateVersion`、`occurredAt`、生产者和追踪信息；使用 Schema Registry、Avro/Protobuf/JSON Schema 之一执行兼容性
  门禁；按聚合键分区并在消费者端使用版本 CAS，提供 upcaster 或多版本处理器。
- 验收标准：CI 能拒绝不兼容契约变更；生产者和前后两个版本消费者可以滚动升级；乱序、重复、延迟和旧版本消息不会
  回退读模型；每种核心事件都有契约测试和重放测试。

**修复记录（2026-07-15）**

- 已定义稳定事件信封，统一包含事件类型、契约版本、聚合版本、发生时间、生产者版本和追踪信息；10 种核心事件均使用
  强类型载荷，并提供 7 份 JSON Schema、版本注册表、兼容读取和严格类型校验。
- Outbox 在同一事务内先按 `eventId` 幂等占位，再原子分配聚合序号；发布查询通过单条 `NOT EXISTS` SQL 仅选择每个聚合
  的队首事件，失败或租约中的旧事件会阻塞后继事件，Kafka 固定使用聚合 ID 作为消息键。
- 六个核心消费者共享数据库版本 CAS；重复和旧版本事件返回 stale，版本缺口触发重试，处理失败回滚版本声明。契约错误
  直接进入 DLT，搜索索引同时使用外部版本防止旧消息覆盖新状态。
- 已增加 5 组生产者迁移、6 组消费者版本表，以及契约兼容、乱序重放、并发去重、队首公平性和版本 CAS 测试。12 个相关
  Reactor 模块的完整单元测试通过；MySQL/Kafka Testcontainers 用例已纳入构建，但本机缺少 Docker CLI，因此本次本地
  验证中按预设条件显式跳过。

## P1 高风险问题

### P1-01 生产控制面只保存操作记录而不执行操作

- 状态：`[x] 已完成（2026-07-15）`
- 问题：流量隔离、发布放量、消息重放、容量演练、混沌、备份、数据库操作和 FinOps 等接口主要修改数据库状态，
  未形成对 Nacos、Sentinel、Kafka、Prometheus、Kubernetes、云数据库或云负载均衡的执行适配器和协调循环。
  多区域路由 ConfigMap 也未找到实际挂载和消费方。
- 影响：API 返回成功或数据库状态变为已执行时，真实运行环境可能没有发生任何变化；运营人员会获得错误反馈，无法依靠
  这些模块完成隔离、回滚、备份、重放、扩容和故障切换。
- 证据：[TrafficService.java](../traffic/src/main/java/com/emall/traffic/TrafficService.java#L27)、
  [ReleaseService.java](../release/src/main/java/com/emall/release/ReleaseService.java#L35)、
  [ReliabilityService.java](../reliability/src/main/java/com/emall/reliability/ReliabilityService.java#L25)、
  [PlatformOpsService.java](../platform-ops/src/main/java/com/emall/platformops/PlatformOpsService.java#L23)。
- 修复方向：如果只作为治理台账，应修改模块名称和文档，避免宣称为控制面；如果作为真实控制面，应采用期望状态与
  观测状态模型，增加幂等执行器、协调器、租约、审批、审计、超时、回滚和外部系统适配器，并持续回读实际状态。
- 验收标准：每个操作都能在测试环境改变真实基础设施并回读验证；重复请求不会重复产生副作用；执行失败、控制器重启、
  外部系统超时和部分成功时状态能够正确收敛或回滚。

修复记录：

- 在 `common` 中增加持久化期望状态控制面：操作使用全局幂等键和期望状态摘要去重，协调器通过 CAS 租约领取任务，区分
  应用、回读、重试、回滚和终态；控制器失联后会先回读外部状态再决定是否重放，避免重复副作用。
- 实现 Nacos 配置、Kubernetes Server-Side Apply、Kafka Consumer Group Offset 和基础设施 Operator 四类真实适配器；
  所有适配器均先保存回滚快照、执行变更并持续回读，超出重试预算后执行可恢复回滚。
- `traffic`、`release`、`reliability`、`platform-ops` 已接入控制命令，业务记录只有在对应外部操作回读为
  `SUCCEEDED` 后才能进入 `COMPLETED/PASSED`；删除无人消费的静态多区域路由 ConfigMap，改为发布完整动态路由快照。
- 四个模块新增独立 `control_plane_operation` 迁移表。生产启动会拒绝内存存储、缺失适配器、本机端点、缺失 Nacos/
  基础设施凭据和缺失 Kubernetes ServiceAccount Token；ExternalSecret、ConfigMap、生产 Profile、最小权限 RBAC 已接通。
- 14 个控制面单元与适配器测试、8 个模块服务测试及 6 个 Kubernetes 清单集成测试通过；MySQL 持久化幂等集成测试已纳入
  Failsafe，本机未发现 Docker 环境时按 `disabledWithoutDocker` 明确跳过，需由容器化 CI 继续执行。

### P1-02 原生 Kubernetes 和 Helm 是两套冲突的部署事实来源

- 状态：`[x] 已完成（2026-07-16）`
- 问题：原生 Kubernetes 清单和 Helm Chart 分别维护副本、资源、HPA 和服务列表。订单原生清单配置 3 个副本、
  HPA 3 到 200，并包含 PDB；Helm 默认资源和 HPA 明显不同，只列出 17 个服务，模板中也没有同等的 PDB 和拓扑分散策略。
- 影响：不同团队或环境使用不同入口会部署出完全不同的容量和可用性拓扑；一套配置的修复不会自动进入另一套，容易造成
  生产漂移、服务缺失和错误扩容。
- 证据：历史原生清单已删除；统一实现见 [values.yaml](../ops/helm/emall/values.yaml)、
  [rollout.yaml](../ops/helm/emall/templates/rollout.yaml)、
  [poddisruptionbudget.yaml](../ops/helm/emall/templates/poddisruptionbudget.yaml) 和
  [hpa.yaml](../ops/helm/emall/templates/hpa.yaml)。
- 修复方向：选择 Helm 或 Kustomize 作为唯一生产源；另一套资产必须由唯一源自动渲染生成，不能手工维护；在 CI 中执行
  schema、策略和渲染差异检查，统一资源、PDB、反亲和、拓扑分散、HPA、探针、安全上下文和服务完整性。
- 验收标准：同一版本只能生成一套确定的生产清单；全部运行服务均被覆盖；渲染结果通过 kubeconform、策略测试和预生产
  Server-Side Dry Run；修改副本或资源后不存在另一套未同步配置。

修复记录：

- 选择 `ops/helm/emall` 作为唯一生产部署事实源，删除 38 份手工在线服务清单及重复的 ConfigMap、ServiceAccount、
  NetworkPolicy、Gateway、Rollout 和 Istio 文件；`ops/k8s` 仅保留 ExternalSecret、迁移过渡入口和非生产混沌资产。
- `values.yaml` 完整覆盖 38 个 Spring Boot 在线服务，并通过容量等级统一副本、CPU/内存、HPA；模板统一生成 Argo
  Rollout、Service、PDB、拓扑分散、Pod 反亲和、探针、优雅关闭、安全上下文、最小权限 RBAC 和动态运行配置。
- 增加 `values.schema.json`，强制服务数量、端口和容量等级结构，拒绝 `latest` 镜像；集成测试同时校验模块覆盖、名称/
  端口唯一、无重复原生清单、资源策略完整以及渲染后资源键唯一。
- CI 固定 Helm 4.2.0、kubeconform 0.7.0 和 Argo Rollouts 1.8.3，依次执行严格 lint、确定性渲染、checksum 校验、
  kubeconform 和 Kind API Server 的 Server-Side Dry Run，任何 schema、策略或 API 校验失败都会阻断合并。
- 本地实际渲染出 246 个资源；kubeconform 验证 200 个标准资源全部有效、46 个 CRD 资源按显式规则交由 API Server 校验；
  8 项 Helm/部署集成测试通过。本机没有 Docker，Kind Dry Run 由 CI 的不可跳过作业执行。

### P1-03 集中式 Migration Runner 耦合所有服务和数据库权限

- 状态：`[x] 已完成`
- 修复记录：Migration Runner 已收敛为单服务执行器，不再聚合任何业务 SQL；`Dockerfile.migration` 为 37 个数据库
  服务分别构建只包含自身脚本的不可变镜像，Helm 为每个服务生成独立 Job、ServiceAccount 和
  `emall/migrations/<service>` ExternalSecret。共享运行时 Secret 已删除跨库迁移凭据，JDBC URL 会强制校验只能落在
  当前服务数据库；本地 MySQL 初始化和 Testcontainers 集成测试验证迁移账号不能访问其他业务库。
- 分片迁移先执行金丝雀，再按有界并发分批执行，批次设置截止时间和观测暂停；失败 Job 可基于 Flyway 历史安全重建，
  已完成分片会幂等跳过。每个应用 Rollout 只等待自身迁移 Job，并通过 `resourceNames` 限定的只读 RBAC 查询状态，单服务
  失败不会阻塞其他服务。默认 `EXPAND` 阶段拒绝删除、重命名和截断，`CONTRACT` 必须同时提供兼容版本、显式开关和审批单。
- 13 项迁移单元测试、8 项 Kubernetes/Helm 集成测试和 Helm 严格检查通过；实际渲染 431 个资源，kubeconform 验证
  348 个标准资源全部有效、83 个 CRD 资源按显式规则交由 API Server 校验。MySQL 权限隔离用例在本机无 Docker 时跳过，
  CI 通过 `emall.integration.require-docker=true` 将其设为不可跳过门禁。
- 问题：一个 Migration Runner 构建物打包所有模块的迁移脚本，使用同一组用户名和密码，串行遍历全部服务、区域和
  数据库分片。任何服务的迁移错误都会中断整个批次。
- 影响：独立服务不能独立发布数据库变更；迁移账号需要跨越大量数据库的高权限，凭证泄漏和错误脚本的故障半径过大；
  大量分片串行迁移也会显著延长发布窗口。
- 证据：[pom.xml](../migration-runner/pom.xml#L56)、
  [MigrationRunnerProperties.java](../migration-runner/src/main/java/com/emall/migration/MigrationRunnerProperties.java#L17)、
  [MigrationRunnerProperties.java](../migration-runner/src/main/java/com/emall/migration/MigrationRunnerProperties.java#L58)、
  [MigrationRunnerApplication.java](../migration-runner/src/main/java/com/emall/migration/MigrationRunnerApplication.java#L29)、
  [migration-runner.yml](../ops/k8s/migration-runner.yml#L27)。
- 修复方向：每个服务发布版本化、不可变的迁移制品和独立 Job，使用该服务的最小权限凭证；平台编排层只负责依赖顺序、
  审批和汇总状态；按分片批次或金丝雀执行，采用 expand/contract 兼容滚动发布，并设置超时、暂停和回滚门禁。
- 验收标准：一个服务的迁移失败不会阻塞无关服务；迁移账号无法访问其他业务库；能够在少量分片验证后分批放量；应用
  新旧版本在迁移窗口内均可运行，失败批次可以安全停止和恢复。

### P1-04 身份账户和用户档案缺少统一生命周期编排

- 状态：`[x] 已完成`
- 修复记录：`identity` 现在是客户账户生命周期的唯一权威服务，外部只保留
  `POST /api/identity/registrations` 注册入口并强制 `Idempotency-Key`；`user` 已删除独立注册、状态变更和隐私删除接口。
  注册事务会原子写入待激活账户、密码凭据、注册幂等记录和版本化 Outbox，只有用户档案返回可靠 ACK 后账户才进入
  `ACTIVE`，因此客户端无法登录孤儿身份。
- 身份服务通过 `AccountRegistered/Activated/Suspended/Restored/Closed/DeletionRequested/Deleted` 事件驱动用户档案；用户
  消费事务按账户 ID 预先选择分片，并原子提交消费幂等记录、聚合版本水位、档案投影和 ACK Outbox。双方均使用事务
  Outbox、按聚合版本有序消费、重复消息去重和绑定 hash 校验；账户行与生命周期行采用固定加锁顺序，避免 ACK、人工状态
  变更和定时对账互相覆盖。
- 停用、关闭和删除会在账户锁内撤销数据库会话，并把仍可能有效的 JWT 会话写入 Redis 撤销存储；登录和刷新同样锁定
  账户，消除了停用并发创建新会话的窗口。删除先等待用户档案清除手机号和昵称，再擦除身份主体及凭据；恢复只允许从
  `SUSPENDED` 返回 `ACTIVE`，旧会话保持失效。
- 256 个逻辑对账分区通过分布式租约有界轮询，持续重发未确认状态；长期未收敛记录输出
  `emall_identity_lifecycle_due` 并触发 Prometheus 告警。Compose、Helm、Kafka 主题配置和 Java Smoke 均已切换到单入口
  异步注册流程。
- 事件契约、状态机、重复消费、绑定冲突、会话撤销、恢复、隐私删除、逻辑分区和 API 收口测试通过；MySQL/Flyway
  集成测试覆盖并发注册重试后单身份/单档案、事务 Outbox 和重复投递。本机没有 Docker，4 项容器用例按规则跳过，CI 通过
  `emall.integration.require-docker=true` 强制执行。
- 问题：客户端需要先创建身份账户并登录，再单独调用用户服务创建档案。身份服务创建账户后没有发布可靠事件或调用
  用户生命周期工作流；用户停用、身份停用、注销和隐私删除也没有明确的双向传播与对账机制。
- 影响：两次调用之间失败会产生可登录但没有用户档案的孤儿身份；重试时可能发生手机号、主体和用户 ID 绑定冲突；
  停用或删除只在一个服务生效时可能继续访问业务数据或违反隐私删除要求。
- 证据：[CheckoutSmokeApplication.java](../smoke/src/main/java/com/emall/smoke/CheckoutSmokeApplication.java#L41)、
  [IdentityService.java](../identity/src/main/java/com/emall/identity/IdentityService.java#L55)、
  [UserController.java](../user/src/main/java/com/emall/user/api/UserController.java#L42)。
- 修复方向：定义账户生命周期的权威服务和状态机；由身份服务通过 Outbox 发布版本化 `AccountCreated`、
  `AccountSuspended`、`AccountDeleted` 事件，用户服务幂等创建和同步档案，或者建立专用注册 Saga；增加孤儿扫描、
  绑定校验、补偿和隐私删除编排。
- 验收标准：客户端只调用一个注册入口；在任一步骤超时、重复、崩溃后最终只有一个身份和一个用户档案；停用、注销、
  恢复和删除能够跨服务收敛，并有自动对账发现长期不一致。

### P1-05 当前容量工具和记录不能证明目标容量

- 状态：`[x] 工具与证据门禁已完成（目标容量须由预生产报告验证）`
- 修复记录：`loadtest` 已拆分为 `standalone/worker/coordinator` 三种角色。worker 按索引公平拆分全局 QPS 和
  全局请求序号，可在不同机器或 Kubernetes Indexed Job 并行运行；独立 Helm Chart 使用 RWX 报告卷、主机/可用区
  拓扑分散、只读根文件系统、资源上下限和 Secret 注入，coordinator 在所有 worker 与监控采集结束后单独聚合。
- 请求执行改为 `Semaphore + Phaser` 有界异步流水线，许可获取超时会显式记为背压拒绝；不再保留全量 Future、结果和
  延迟集合。延迟由 HdrHistogram `Recorder` 流式记录，并将压缩直方图跨 worker 合并，因此内存不随总请求数和浸泡
  时长线性增长，P99 也不是错误地平均节点百分位。
- 已实现生产混合流量、用户/SKU 数据基数、热点比例，以及 `constant/step/spike/soak/fault-recovery/breakpoint` 模式。
  多用户写流量按 worker 流式读取 `userId,token` 分片，支付回调使用预置确定性支付数据并生成时间戳、nonce 和
  HMAC-SHA256 签名；共享 JWT、缺失回调密钥或缺失故障实验 ID 均不能形成有效生产证据。
- worker JSON 和聚合 Markdown/JSON 会记录环境、完整 Git SHA、资源、数据规模、目标/实测 QPS、在途量、
  P50/P95/P99、错误率、429/5xx、生成器 CPU/堆/线程/网络及各层饱和指标。报告状态严格区分 `INVALID`、
  `BASELINE_ONLY`、`PREPRODUCTION_RUN_ELIGIBLE` 和 `VERIFIED`；worker 缺失、压测端饱和、指标不全或非预生产运行均
  无法通过门禁。
- 单 Cell 安全 QPS 使用实测值和余量计算，再结合 Cell 数、实测扩展效率与 Little's Law 推导在线并发；证据套件要求
  六种模式在同一 Git 和环境中各重复三轮，固定负载 QPS 变异系数不超过 10%。21 项单元测试和 2 项真实 HTTP/Helm
  集成测试通过；胖 JAR 可直接运行，Helm strict lint 与 kubeconform 对 worker/coordinator 共 4 个资源全部通过。
- 当前工作区没有预生产集群和真实百万级数据，因此没有伪造容量实测值，也不声明 100 万并发已经达标；此限制已由
  `EMALL_LOAD_REQUIRE_VERIFIED_EVIDENCE=true` 变成不可绕过的自动门禁，而不再依赖人工文档约定。
- 问题：Java 压测工具只在单 JVM 中运行，使用固定平台线程池，并把整个测试期间的所有 `CompletableFuture`、结果和
  延迟值保存在内存中。线程池只限制执行线程，不限制待处理 Future 和 HTTP 请求数量；容量基线模板仍没有实测数据。
- 影响：长时间或高 QPS 压测会先耗尽压测机内存，压测端自身成为瓶颈；单机结果不能证明百万并发、多 AZ、分片扩容、
  MQ 积压、数据库故障和下游恢复能力，也无法支持当前容量声明。
- 证据：[CheckoutLoadTestApplication.java](../loadtest/src/main/java/com/emall/loadtest/CheckoutLoadTestApplication.java#L34)、
  [CheckoutLoadTestApplication.java](../loadtest/src/main/java/com/emall/loadtest/CheckoutLoadTestApplication.java#L50)、
  [CheckoutLoadTestApplication.java](../loadtest/src/main/java/com/emall/loadtest/CheckoutLoadTestApplication.java#L216)、
  [capacity-verification.md](capacity-verification.md#容量结论写法)、
  [p2-capacity-baseline-template.md](../ops/loadtest/p2-capacity-baseline-template.md#场景)。
- 修复方向：使用分布式压测执行器；设置明确的在途请求信号量和背压；通过 HdrHistogram 等流式统计避免保留全部结果；
  建立符合真实流量比例、数据基数、热点分布和用户行为的模型；执行阶梯、尖峰、浸泡、故障、恢复和极限测试。
- 验收标准：提交包含环境、Git 版本、资源、数据规模、QPS、并发、P50/P95/P99、错误率和各层饱和指标的容量报告；
  压测端资源不成为瓶颈；目标容量能够通过单 Cell 基线和水平扩展模型推导，并在预生产集群重复验证。

## 2026-07-17 最终代码验收

- `[x]` 45 个 Maven 模块完成完整 `verify` 生命周期，全部构建成功；Checkstyle、JaCoCo 和 Java 17/Maven Enforcer
  门禁均通过。
- `[x]` 148 份 Surefire 报告包含 451 个单元测试，失败 0、错误 0、跳过 0。
- `[x]` 36 份 Failsafe 报告包含 48 个集成测试，20 个在当前环境实际通过，失败 0、错误 0；其中 Helm 渲染、
  Kubernetes 策略、事件契约、迁移资产和分布式压测 HTTP 链路均已执行。
- `[x]` 修复全量验收发现的配置回归：`routing` 已补齐 Nacos、Sentinel、结构化日志、Tracing、健康探针和
  MyBatis-Plus 生产配置；渐进发布和服务网格测试已迁移到唯一权威 Helm Chart，不再引用已删除的原生清单。
- `[x]` 主系统与压测 Chart 均通过 Helm strict lint；主系统、Worker 和 Coordinator 共渲染 435 个 Kubernetes
  资源，kubeconform 校验结果为 352 个有效、0 个无效、0 个错误，83 个 CRD 资源按规则交给 API Server 门禁。
- `[~]` 当前执行环境没有可用 Docker 守护进程，17 个 MySQL、Redis、Kafka 和 Elasticsearch 容器测试按本地规则
  跳过；CI 使用 `emall.integration.require-docker=true` 将其设为不可跳过，因此 P0-01 和 P0-02 保留外部验收状态。
- `[~]` 11 个端到端 Smoke 测试需要已部署的完整服务栈；百万文档搜索、真实中文分词、长期浸泡、多可用区故障恢复
  和百万并发容量报告必须在预生产执行，不能用本地构建结果替代。

## 建议修复顺序

1. 先修复库存双账和 Elasticsearch 错误搜索，消除直接业务正确性问题。
2. 重构事务边界、Saga CAS、事件版本和权威路由目录，保证核心交易最终一致。
3. 改造分片扇出、稳定虚拟分片、后台任务分区租约和数据库连接预算，解决水平扩展瓶颈。
4. 统一部署事实来源和服务级迁移流程，将控制台账改造成真实协调控制面。
5. 完成账户生命周期编排，并使用分布式压测、故障演练和容量报告验证最终架构。

## 完成判定

问题只有在代码或配置完成、自动化测试覆盖、相应故障场景验证且验收标准有证据时才能标记完成。单元测试通过、
Maven 构建成功、存在接口或存在配置文件，均不能单独作为生产架构问题完成的依据。
