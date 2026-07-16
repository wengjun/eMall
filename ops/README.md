# 运维配置索引

[项目首页](../README.md) | [文档索引](../docs/README.md) | [Kubernetes 辅助资产](k8s/README.md) | [生产部署 Chart](helm/emall/README.md)

`ops` 目录保存本地运行、部署、可观测、混沌、MySQL 和压测相关配置。这里是配置入口说明。

## 本地运行

- `../docker-compose.yml`：本地 MySQL、Redis、Kafka、Nacos、Elasticsearch、ClickHouse、ELK、Prometheus、Grafana 和应用服务拓扑。
- `../.env.example`：本机直接运行 Spring Boot 时的 `localhost` 环境变量示例。
- `env/local.env`：Docker Compose 容器网络下的本地覆盖值，使用 `mysql`、`redis`、`nacos` 等容器服务名。
- `mysql/init`：本地 MySQL 初始化脚本，为各服务创建独立 schema。

## 可观测

- `prometheus`：Prometheus 规则和抓取配置。
- `grafana`：Grafana 看板基线。
- `elk/logstash.conf`：ELK 日志管道基线，接收应用 JSON 日志并写入 Elasticsearch。
- 服务运行时支持结构化日志、ELK 日志检索和 OpenTelemetry OTLP 导出。

## Kubernetes

- `helm/emall`：38 个在线服务的唯一生产部署事实源，统一生成 Rollout、Service、PDB、HPA、探针、资源、RBAC、
  NetworkPolicy 和 Gateway API。
- `k8s`：只保存 ExternalSecret、迁移过渡入口和非生产混沌演练，不再保存第二套在线服务清单。
- 真实集群使用前需要修改镜像仓库、域名、证书、Secret 和资源规格。

## HTTPS/TLS 接入

- Web 和手机 App 只通过 HTTPS 调用公网域名，例如 `https://api.emall.example.com/api/orders`。
- TLS 证书在 Kubernetes Gateway API + 云厂商 ALB 层终止，再转发到 Java `gateway` 服务。
- `gateway` 默认追加 Spring Cloud Gateway `SecureHeaders`，对 HSTS、X-Frame-Options、X-Content-Type-Options 等安全响应头做后端兜底。
- 内部服务之间继续使用 HTTP/Dubbo + Nacos，后续需要更高安全级别时再引入服务网格 mTLS。

## 压测

- [loadtest](loadtest/README.md)：分布式 Java 17 压测 Helm Chart、部署步骤和容量人工复核模板。
- Java 压测工具位于 `loadtest` Maven 模块，支持 Indexed Job worker、HdrHistogram 聚合和预生产证据门禁。

## 验证

常用命令：

```powershell
mvn validate
mvn test
mvn verify -DskipITs=false
mvn -Pstable-runtime verify
```

如果要执行 Testcontainers 集成测试，需要先启动 Docker Desktop，并确认：

```powershell
docker version
docker ps
```

Windows 11 + Docker Desktop 场景下，如果 Maven/Testcontainers 没有自动识别 Docker Linux Engine，在当前 PowerShell 中先设置：

```powershell
$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'
mvn -DskipITs=false verify
```
