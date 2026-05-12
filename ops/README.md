# 运维配置索引

[项目首页](../README.md) | [文档索引](../docs/README.md) | [Kubernetes 基线](k8s/README.md) | [Helm 基线](helm/emall)

`ops` 目录保存本地运行、部署、可观测、混沌、MySQL 和压测相关配置。这里是配置入口说明。

## 本地运行

- `../docker-compose.yml`：本地 MySQL、Redis、Kafka、Nacos、Elasticsearch、ClickHouse、ELK、Prometheus、Grafana 和应用服务拓扑。
- `../.env.example`：本地环境变量默认值示例。
- `mysql/init`：本地 MySQL 初始化脚本，为各服务创建独立 schema。

## 可观测

- `prometheus`：Prometheus 规则和抓取配置。
- `grafana`：Grafana 看板基线。
- `elk/logstash.conf`：ELK 日志管道基线，接收应用 JSON 日志并写入 Elasticsearch。
- 服务运行时支持结构化日志、ELK 日志检索和 OpenTelemetry OTLP 导出。

## Kubernetes

- `k8s`：Kubernetes 部署基线。
- `helm/emall`：Helm Chart 部署基线，适合把稳定运行模块统一发布到 Kubernetes。
- 包含 Deployment、Service、PDB、HPA、探针、资源配置、安全上下文和网络策略。
- 真实集群使用前需要修改镜像仓库、域名、证书、Secret 和资源规格。

## 压测

- `loadtest/p2-capacity-baseline-template.md`：容量基线记录模板。
- Java 压测工具位于 `loadtest` Maven 模块。

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
