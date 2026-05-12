# 国内互联网技术栈适配

[文档索引](README.md) | [架构设计](architecture.md) | [运维配置索引](../ops/README.md)

本文按当前确定的范围记录国内互联网公司面试和落地时更贴近的技术栈取舍。Kafka 保留不替换，当前落地以下能力：

1. Sentinel：替换 Resilience4j/自研限流降级的运行时流量治理能力。
2. MyBatis-Plus：替换简单 CRUD 场景的手写 JDBC。
3. Elasticsearch + ClickHouse：替换搜索和分析场景中仅依赖通用数据库或泛化搜索组件表达的方案。
4. ELK：作为日志采集、检索和排障平台。
5. Nacos + Dubbo：Nacos 承载服务注册发现和配置中心入口，Dubbo 承载核心交易链内部 RPC。
6. 分库分表、多级缓存、Redis Cluster 和 Helm：补齐国内生产部署和大规模容量表达。

## 保留的基础栈

- `Java 17 + Spring Boot 3` 继续作为服务运行基础。
- `Kafka` 继续作为事件流、Outbox 发布、搜索索引同步和异步解耦中间件。
- `MySQL + Redis` 继续承载交易数据和缓存。
- `Prometheus + Grafana + OpenTelemetry` 继续承载指标和 Trace 基线。

## Nacos + Dubbo

所有在线业务服务已接入 `spring-cloud-starter-alibaba-nacos-discovery` 和
`spring-cloud-starter-alibaba-nacos-config`。默认本地开发关闭 Nacos，避免没有中间件时影响单模块启动；Docker Compose、
Kubernetes 和 Helm 环境会通过环境变量启用注册发现：

```yaml
spring:
  cloud:
    nacos:
      server-addr: ${EMALL_NACOS_SERVER_ADDR:localhost:8848}
      discovery:
        enabled: ${EMALL_NACOS_DISCOVERY_ENABLED:false}
      config:
        enabled: ${EMALL_NACOS_CONFIG_ENABLED:false}
```

网关路由默认从固定地址切换为 `lb://service-name`，由 Spring Cloud LoadBalancer 基于 Nacos 服务发现解析实例。环境变量仍可覆盖为
HTTP 地址，方便本地临时直连和故障排查。

核心交易链内部同步调用已增加 Dubbo RPC：

- `order -> inventory`：库存预占、确认和释放。
- `order -> pricing`：价格快照查询。
- `order -> marketing`：优惠快照查询。
- `payment -> order`：支付成功后通知订单状态流转。

RPC 契约统一放在 `common/src/main/java/com/emall/common/rpc`，服务提供方使用 `@DubboService` 暴露，调用方使用
`@DubboReference` 注入。`EMALL_RPC_PROTOCOL=http` 时继续走 HTTP 兜底；`EMALL_RPC_PROTOCOL=dubbo` 且
`EMALL_DUBBO_REGISTRY_ADDRESS=nacos://nacos:8848` 时走 Dubbo + Nacos。

面试时可以这样解释取舍：Nacos 解决“服务在哪里”和“配置如何动态下发”，Dubbo 解决高频内部 RPC 的接口治理、连接复用和服务治理。
HTTP Controller 不删除，因为它仍然是网关外部入口、调试入口和 RPC 故障时的兜底路径。

## Sentinel

所有 Spring Boot 服务已接入 `spring-cloud-starter-alibaba-sentinel`，默认关闭：

```yaml
spring:
  cloud:
    sentinel:
      enabled: ${EMALL_SENTINEL_ENABLED:false}
      transport:
        dashboard: ${EMALL_SENTINEL_DASHBOARD:localhost:8858}
        port: ${EMALL_SENTINEL_API_PORT:8719}
```

启用方式：

```powershell
$env:EMALL_SENTINEL_ENABLED = "true"
$env:EMALL_SENTINEL_DASHBOARD = "localhost:8858"
$env:EMALL_SENTINEL_API_PORT = "8719"
```

面试时重点讲清楚：

- Sentinel 解决运行时流量治理，覆盖限流、熔断、降级、热点参数和系统保护。
- 限流保护入口，熔断保护调用方，降级保护用户体验。
- Sentinel 不替代业务幂等、事务一致性、Outbox 补偿和数据对账。
- 生产规则应由控制台或配置中心动态推送，不能只靠本地 YAML。

当前 `order` 和 `payment` 的下游保护已经从 Resilience4j 收敛到 Sentinel：

- `order.inventory.reserve`、`order.inventory.confirm`、`order.inventory.release`：库存预占、确认和释放。
- `order.pricing.quote`：价格报价。
- `order.marketing.quote`：促销报价，降级为无优惠。
- `payment.order.pay`：支付成功后通知订单。

这些资源使用 `@SentinelResource` 声明保护点，并提供 `blockHandler` 和 `fallback`。`blockHandler` 处理限流、
熔断打开等 Sentinel 阻断，`fallback` 处理运行时异常。没有控制台时，模块内的 Sentinel 规则配置会加载基础
QPS 和异常比例熔断规则；有控制台或配置中心时，生产规则应动态下发覆盖本地基线。

## MyBatis-Plus

所有 JDBC 服务已接入 `mybatis-plus-spring-boot3-starter`，公共模块提供分页插件：

```java
@Bean
@ConditionalOnMissingBean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    return interceptor;
}
```

使用边界：

- 简单 CRUD、后台分页、条件查询优先使用 MyBatis-Plus。
- 订单创建、库存扣减、支付回调、Outbox 扫描、任务锁等核心链路可以继续保留手写 SQL。
- 复杂 SQL 仍然要看执行计划、索引、锁等待和慢查询，不能因为用了 MyBatis-Plus 就忽略数据库基本功。

这种取舍在国内面试里更容易解释：效率型代码用 MyBatis-Plus，关键交易链路保留显式 SQL，保证锁、事务和幂等边界可控。

当前工程已经落地两层能力：

- 公共模块自动注册 `MybatisPlusInterceptor`，所有 JDBC 服务可以直接使用 MyBatis-Plus Mapper、分页和条件构造器。
- 业务模块 POM 已统一接入 MyBatis-Plus starter，后续新增简单表的 CRUD 不再需要写 `JdbcTemplate` 样板代码。

没有强行替换的 MySQL 操作：

- Outbox 扫描、发布状态更新和失败重试。
- 分布式任务锁。
- 支付流水、对账、库存扣减和订单状态流转。
- 需要显式控制 SQL、索引命中、行锁范围和幂等约束的核心交易写入。

这些场景保留手写 SQL 不是“没替换完”，而是为了让交易链路的锁、事务和故障恢复边界保持可审计、可解释。

## Elasticsearch + ClickHouse

搜索链路使用 Elasticsearch 作为商品搜索读模型：

```yaml
emall:
  search:
    engine: elasticsearch
    url: ${EMALL_ELASTICSEARCH_URL:http://localhost:9200}
```

`search` 模块已经增加 Elasticsearch Repository 适配：

- `ElasticsearchSearchDocument`：Elasticsearch 索引文档模型。
- `ElasticsearchDocumentRepository`：Spring Data Elasticsearch Repository。
- `ElasticsearchSearchRepository`：实现业务侧 `SearchRepository` 接口。

分析链路使用 ClickHouse 作为 OLAP 候选存储：

```yaml
emall:
  olap:
    engine: clickhouse
    clickhouse-url: ${EMALL_CLICKHOUSE_URL:jdbc:clickhouse://localhost:8123/emall}
```

公共模块已经提供 `ClickHouseAutoConfiguration`：

- `clickHouseDataSource`：独立 ClickHouse 数据源。
- `clickHouseJdbcTemplate`：独立 ClickHouse 查询入口，不污染主 MySQL 数据源。

职责边界：

- Elasticsearch 负责商品搜索、倒排索引、相关性排序、筛选聚合和搜索降级。
- ClickHouse 负责埋点、报表、漏斗、经营分析和大宽表聚合。
- MySQL 不承载全文搜索和高维分析压力，避免核心交易库被查询拖垮。
- Kafka 继续作为商品变更、交易事件和分析事件进入搜索/分析系统的异步通道。

## ELK

日志平台采用 ELK：

- Elasticsearch：保存应用结构化日志索引。
- Logstash：接收 JSON 日志、补充字段、写入 Elasticsearch。
- Kibana：按 traceId、requestId、orderId、userId、错误码和服务名检索。

本地 Logstash 管道位于：

```text
ops/elk/logstash.conf
```

应用侧继续输出 JSON 日志，交易链路不直接依赖日志平台。如果 ELK 故障，服务应降级为本地标准输出，不影响下单、支付和库存扣减。

## 本地运行中间件

`docker-compose.yml` 已包含以下基础设施：

- MySQL
- Redis
- Kafka
- Nacos
- Elasticsearch
- ClickHouse
- Logstash
- Kibana
- Prometheus
- Grafana
- OpenTelemetry Collector

## 生产扩展基线

### 分库分表

当前工程新增了通用分片路由示例：

- `HashModShardRouter`：按用户 ID、订单 ID 等分片键做 hash/mod 路由。
- `ShardRoute`：返回物理库名、物理表名、库索引和表索引。

面试时不要只说“用了分库分表”，要讲清楚三件事：

- 分片键优先选择高频查询主键，例如订单按 `userId` 或 `orderId` 路由。
- 单分片事务保留本地 ACID，跨分片操作走异步事件、补偿和对账。
- 扩容要考虑双写、数据迁移、校验、灰度读切换和回滚。

### 多级缓存和 Redis Cluster

当前工程新增了 `TwoLevelCache` 和 `ExpiringMapCacheStore` 作为多级缓存代码示例，表达本地 L1 缓存加 Redis
L2 缓存的读路径：

1. 先查本地缓存，命中直接返回。
2. 本地未命中查 Redis，命中后回填本地缓存。
3. Redis 未命中再加载数据库，随后同时写入 Redis 和本地缓存。
4. 写操作先更新数据库，再删除或更新缓存，避免脏数据长期停留。

`gateway`、`product`、`pricing` 增加了 `redis-cluster` profile，可以通过
`EMALL_REDIS_CLUSTER_NODES` 接入 Redis Cluster。默认本地仍然使用单节点 Redis，避免开发环境复杂化。

### Helm 部署

`ops/helm/emall` 提供 Helm Chart 基线，覆盖稳定运行核心服务的 Deployment、Service、HPA、探针、资源规格和
统一运行环境变量。它不是替代 `ops/k8s`，而是给国内云原生面试和真实集群部署提供更接近生产的发布入口。

可以这样说明取舍：`ops/k8s` 适合逐个看清 Kubernetes 对象，`ops/helm/emall` 适合批量参数化部署和环境差异化
管理。真正生产还需要接入镜像仓库、Secret 管理、Ingress、证书、灰度发布和云上托管中间件。

## 面试讲法

可以这样总结：

> 这个项目没有为了贴近国内面试而盲目替换所有技术栈。Kafka 保留，因为它适合事件流、Outbox 和高吞吐日志型数据。国内化改造重点放在 Sentinel、MyBatis-Plus、Nacos、Dubbo、Elasticsearch、ClickHouse、ELK、分库分表、多级缓存、Redis Cluster 和 Helm：Sentinel 解决运行时流量治理，MyBatis-Plus 提升 CRUD 效率，Nacos 承载服务发现和配置中心入口，Dubbo 承载核心交易链内部 RPC，Elasticsearch 承载搜索读模型，ClickHouse 承载分析型查询，ELK 承载结构化日志检索，分库分表和多级缓存表达大规模读写扩展，Helm 表达生产部署能力。核心交易链路仍然保留显式 SQL、幂等、补偿、对账和可观测性，避免为了换技术而降低稳定性。
