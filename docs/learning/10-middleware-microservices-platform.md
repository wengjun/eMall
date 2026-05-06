# 中间件、微服务和部署平台

[返回学习手册首页](README.md) | [返回技术能力地图](../technical-skill-map.md)

## 中间件

### Redis

常见用途：

- 商品缓存。
- 价格缓存。
- 网关限流。
- 热点数据。
- 分布式锁基础。

需要掌握：

- key 设计。
- TTL。
- 缓存穿透。
- 缓存击穿。
- 缓存雪崩。
- 热 key。
- 原子操作。

注意：

- Redis 不是数据库的完全替代。
- 缓存和数据库会短暂不一致。
- 删除缓存和更新数据库需要设计顺序。

### Kafka

常见用途：

- 订单事件。
- 支付事件。
- 商品变更事件。
- 搜索索引同步。
- 履约事件。

需要掌握：

- Topic。
- Partition。
- Producer。
- Consumer。
- Consumer Group。
- Offset。
- 至少一次投递。
- 重试。
- 死信。

关键认知：

- Kafka 不能保证业务只处理一次。
- 消费端必须幂等。
- 消费延迟需要监控。
- 消息顺序通常只在同一个 partition 内保证。

### OpenSearch

用于搜索读模型。

需要掌握：

- 文档。
- 索引。
- 倒排索引。
- 查询。
- 分词。
- 商品上下架同步。
- 最终一致。

注意：

- 搜索不是交易数据源。
- 商品写入不能强依赖搜索成功。
- 搜索索引可以重建。

### Docker Compose

用于本地启动依赖：

- MySQL。
- Redis。
- Kafka。
- OpenSearch。
- Prometheus。
- Grafana。

需要掌握：

- service。
- network。
- volume。
- environment。
- profile。
- healthcheck。

### Testcontainers

Testcontainers 用于测试中启动真实中间件。

优点：

- 比 mock 更接近真实环境。
- 能验证 Flyway、SQL、Redis、Kafka 行为。

限制：

- 需要 Docker。
- 执行速度慢。
- CI 需要支持容器。


## 微服务和网关

### 服务拆分原则

按业务域拆分，而不是按技术层拆分。

正确拆法：

- 订单服务。
- 库存服务。
- 支付服务。
- 商品服务。

错误拆法：

- Controller 服务。
- Service 服务。
- DAO 服务。

判断服务边界：

- 是否拥有独立业务数据？
- 是否可以独立部署？
- 是否有独立扩缩容需求？
- 是否由不同业务规则驱动？

### 网关

网关负责：

- 路由。
- 限流。
- Trace ID。
- 安全响应头。
- 统一入口控制。

网关不应该负责：

- 复杂业务逻辑。
- 订单状态变化。
- 库存扣减。
- 支付处理。

### 内部调用

内部调用要注意：

- 超时。
- 重试。
- 熔断。
- trace ID 透传。
- 错误码转换。
- 幂等键传递。

本工程建议使用 Java 现代 HTTP 客户端，例如 Spring `RestClient`。


## 部署和运维

### Dockerfile

Dockerfile 解决如何构建服务镜像。

需要关注：

- 基础镜像。
- Java 版本。
- JAR 路径。
- 非 root 用户。
- 启动命令。
- 时区。

### Docker Compose

Docker Compose 适合本地开发。

它可以启动：

- MySQL。
- Redis。
- Kafka。
- OpenSearch。
- Prometheus。
- Grafana。
- 应用服务。

### Kubernetes

Kubernetes 适合生产部署。

需要掌握：

- Deployment：部署无状态服务。
- Service：服务发现。
- Ingress：入口流量。
- ConfigMap：普通配置。
- Secret：敏感配置。
- Probe：健康检查。
- HPA：自动扩缩容。
- PDB：保护滚动升级可用性。
- NetworkPolicy：网络访问控制。
- resources：CPU 和内存限制。

### 灰度和回滚

灰度发布步骤：

1. 小比例流量。
2. 观察错误率和延迟。
3. 观察业务指标。
4. 逐步放量。
5. 异常时回滚。

回滚前要确认：

- 数据结构是否兼容。
- 消息格式是否兼容。
- 配置是否能恢复。
- 是否需要补偿任务。
