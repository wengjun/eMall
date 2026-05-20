# eMall Platform

eMall 是一个基于 Java 17 的微服务电商平台，用来学习和展示大型电商系统的工程设计。目标场景参考
10 亿注册用户、1 亿日活、100 万峰值并发，但当前工程不宣称已经真实承载同等生产流量。

这个仓库不是简单 CRUD 商城，而是一个生产导向的起点。它覆盖网关、用户、商品、类目、价格、库存、订单、
购物车、支付、营销、促销、搜索、履约、评价、售后、商家、秒杀、推荐、实验、广告、供应链、财务、客服、
预测、事件平台、数仓、智能、分析、混沌、成本、身份、风控、流量、可靠性、发布、平台运维、运营、开放平台、
通用库和治理模块。

## 快速导航

- [文档总目录](docs/README.md)：所有架构、测试、运维和学习文档入口。
- [学习手册](docs/learning/README.md)：按主题系统学习 Java 17 电商系统实现。
- [按分类学习面试题](docs/interview/README.md)：唯一的面试学习入口，按分类顺序学习全部题目。
- [运维配置索引](ops/README.md)：Docker Compose、Kubernetes、可观测和压测配置入口。
- [Kubernetes 基线](ops/k8s/README.md)：Kubernetes 部署清单说明。
- [Web/App 下单 API 契约](docs/api/web-app-checkout.openapi.yml)：浏览器和手机 App 共用的后端下单接口契约。

系统重点实现和表达以下能力：

- 微服务分布式部署。
- 网关限流、熔断、降级、重试、隔离。
- Nacos 服务注册发现和 Dubbo 核心交易链内部 RPC。
- 下游自动平滑恢复。
- 幂等下单、库存预占、库存确认和释放。
- Outbox 事件、Kafka 发布、补偿任务。
- 支付回调幂等、资金流水、渠道对账。
- 可观测性、SLO、Runbook、Kubernetes 部署基线。
- 单元测试、集成测试、smoke 测试和压测工具。

## 目录结构

根目录保留 Maven 多模块平铺结构，不再把业务模块物理移动到 `services`、`tools`、`common` 等多级目录。这样可以保持
`mvn -pl order -am verify`、Dockerfile、Docker Compose、CI 和 IDE 导入路径稳定。

主要入口按职责理解：

| 入口 | 内容 |
| --- | --- |
| `README.md` | 项目总入口和本地启动说明 |
| `pom.xml` | Maven 聚合工程、依赖版本和 profile 分组 |
| `common`、`governance`、`gateway` | 平台基础能力 |
| `user`、`product`、`inventory`、`order`、`payment` 等平铺目录 | 独立业务或平台服务模块 |
| `smoke`、`loadtest` | Java 验证工具 |
| `docs` | 架构、学习、测试、运维和面试资料 |
| `ops` | Docker Compose、Kubernetes、Helm、观测、MySQL、压测和混沌配置 |

如果只想按分类理解模块，不需要从根目录逐个找，优先看 [模块清单](docs/modules.md)。

## 模块

| 分组 | 模块 |
| --- | --- |
| 平台基础 | `common`、`governance`、`gateway` |
| 核心交易 | `user`、`product`、`inventory`、`order`、`cart`、`payment` |
| 交易扩展 | `pricing`、`marketing`、`search`、`fulfillment`、`review`、`after-sales` |
| 商家和增长 | `merchant`、`flash-sale`、`recommendation`、`catalog`、`promotion`、`experiment`、`advertising` |
| 供应、财务和客服 | `supply-chain`、`finance`、`customer-service`、`forecasting` |
| 数据、AI 和分析 | `event-platform`、`data-warehouse`、`intelligence`、`analytics` |
| 信任、风险和治理 | `identity`、`risk`、`operations`、`openapi`、`chaos`、`cost` |
| 生产控制面 | `traffic`、`reliability`、`release`、`platform-ops` |
| 验证工具 | `smoke`、`loadtest` |

完整模块说明见 [模块清单](docs/modules.md)。

## 核心交易流程

1. `POST /api/orders` 使用 `requestId` 幂等创建订单，并通过 `clientType=WEB|APP`、`deviceId`、`channel` 区分 Web 和手机 App 下单来源。
2. 订单服务读取价格和优惠快照。
3. 订单服务调用库存服务预占库存。
4. 预占成功后订单进入 `CREATED`。
5. `POST /api/orders/{orderId}/pay` 确认支付并确认库存。
6. `POST /api/orders/{orderId}/cancel` 释放库存并取消订单。
7. 如果库存、支付或 MQ 出现异常，补偿任务和内部运维接口负责恢复。

下单请求示例：

```json
{
  "requestId": "web-order-001",
  "userId": 70001,
  "skuId": 30001,
  "quantity": 1,
  "clientType": "WEB",
  "deviceId": "web-device-001",
  "channel": "pc-web"
}
```

手机 App 下单时将 `clientType` 改为 `APP`，并将 `channel` 设置为 `ios-app`、`android-app` 等渠道值。
`deviceId` 和 `channel` 可以放在请求体，也可以通过 `X-Device-Id`、`X-Client-Channel` 请求头传入；请求体优先。
老客户端不传 `clientType`、`deviceId`、`channel` 时，订单服务分别默认按 `WEB`、`unknown-device`、`direct` 处理。

外部接入采用 Kubernetes Gateway API + 云厂商 ALB 终止 HTTPS/TLS，再转发到 Java `gateway`。
Web 和手机 App 生产请求只暴露 `https://api.emall.example.com/api/orders` 这类 HTTPS 地址，内部服务继续走
HTTP/Dubbo + Nacos。`gateway` 会追加安全响应头作为后端兜底，并把用户、客户端类型、渠道、设备、SKU、IP 组合进限流键。

## 关键生产能力

- 核心交易链路默认使用 MyBatis-Plus、Flyway 和 MySQL；`emall.storage=memory` 仅用于本地实验。
- 每个服务拥有独立数据库 schema，避免 Flyway 历史和表所有权混乱。
- 商品和价格读取使用 Redis-backed Spring Cache 降低数据库压力。
- 订单保存下单时价格、优惠、币种、价格版本和优惠券快照。
- 库存支持热点 SKU 库存桶，减少单行库存竞争。
- 网关按用户、客户端类型、渠道、设备、SKU、客户端 IP 等组合键限流。
- 订单、库存、支付、商品通过 Outbox 可靠发布事件。
- 搜索服务异步消费商品事件，保证搜索投影最终一致。
- 内部运维接口支持补偿、Outbox 重放、库存释放和支付对账。
- 用户手机号使用 AES-GCM 加密，并使用 HMAC hash 做精确查询。
- 支付服务记录追加式流水，并对渠道账单做对账。
- 秒杀服务通过活动窗口、用户令牌、库存预分配和队列隔离尖峰流量。
- 流量、可靠性、发布和平台运维模块提供生产控制面模型。
- Prometheus、Grafana、结构化日志和 OpenTelemetry 提供观测基础。
- Kubernetes 清单包含探针、资源配置、安全上下文、NetworkPolicy 和优雅关闭。

## 本地启动

要求：

- Java 17。
- Maven 3.9+。
- Docker Desktop，只有在需要本地基础设施或 Testcontainers 时才必须启动。

启动基础设施：

```powershell
docker compose up -d mysql redis kafka nacos elasticsearch clickhouse logstash kibana prometheus grafana
```

编译打包：

```powershell
mvn clean package
```

启动核心服务示例：

```powershell
mvn -pl gateway spring-boot:run
mvn -pl user spring-boot:run
mvn -pl product spring-boot:run
mvn -pl inventory spring-boot:run
mvn -pl order spring-boot:run
mvn -pl payment spring-boot:run
```

也可以在打包后使用 Docker Compose `app` profile 启动核心服务：

```powershell
mvn clean package -DskipTests
docker compose --profile app up -d --build
mvn -pl smoke exec:java
```

根目录 `Dockerfile` 可以从已打包模块构建服务镜像：

```powershell
docker build --build-arg MODULE=order -t emall/order:local .
```

运行时连接配置可以通过环境变量覆盖。本机直接运行少量 Spring Boot 模块时，参考 `.env.example`；
Docker Compose 的公共默认值集中在
`docker-compose.yml` 顶部的 `x-app-runtime-env` 锚点，本地可覆盖值见
`ops/env/local.env`。生产环境不要使用这个本地文件，应改用 Kubernetes Secret、ConfigMap、Nacos Config
或云厂商密钥管理。

## Windows 11 本地启动步骤

Windows 11 下不需要单独安装 MySQL、Redis、Kafka、Nacos、Elasticsearch 或 ClickHouse，推荐全部通过
Docker Desktop 和根目录 `docker-compose.yml` 启动。这样版本更一致，也便于删除和重建本地环境。

1. 检查 Java、Maven 和 Docker：

```powershell
java -version
mvn -version
docker version
docker compose version
```

`docker version` 必须能看到 `Server` 信息。如果只有 `Client`，说明 Docker Desktop daemon 没有正常启动。

2. 启动 Docker Desktop：

- 使用 Linux containers。
- 推荐启用 WSL2 backend。
- 如果 Docker Desktop 刚启动，等待 1 到 2 分钟再执行 Maven 集成测试或 `docker compose`。

3. 启动本地基础设施：

```powershell
docker compose up -d mysql redis kafka nacos elasticsearch clickhouse logstash kibana prometheus grafana
docker compose ps
```

如果需要覆盖本地默认连接配置，优先修改 `ops/env/local.env`，然后在命令中显式加载：

```powershell
docker compose --env-file ops/env/local.env up -d mysql redis kafka nacos
```

4. 编译工程：

```powershell
mvn clean package
```

5. 分别启动核心服务。建议每个服务使用一个独立 PowerShell 窗口：

```powershell
mvn -pl gateway spring-boot:run
mvn -pl user spring-boot:run
mvn -pl product spring-boot:run
mvn -pl inventory spring-boot:run
mvn -pl order spring-boot:run
mvn -pl payment spring-boot:run
```

6. 也可以用 Docker Compose 启动应用服务镜像：

```powershell
mvn clean package -DskipTests
docker compose --profile app up -d --build
docker compose ps
```

7. 执行验证：

```powershell
mvn -DskipITs test
mvn -DskipITs=false test-compile failsafe:integration-test failsafe:verify
```

常见问题：

- Docker 命令卡住：先关闭 Docker Desktop，再执行 `wsl --shutdown`，然后重新打开 Docker Desktop。
- 端口占用：用 `netstat -ano | findstr :端口号` 查占用进程，再决定是否关闭对应程序。
- Testcontainers 被跳过：先确认 `docker version` 有 `Server` 信息，再重新执行集成测试。
- Testcontainers 在 Windows 11 下找不到 Docker：确认 Docker Desktop 使用 Linux Engine，并在当前 PowerShell 中设置
  `$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'` 后重试。
- 中文显示乱码：PowerShell 建议使用 UTF-8 终端，或在 Windows Terminal 中打开 PowerShell。
- Maven 依赖下载慢：确认 Maven `settings.xml` 已配置国内镜像，并使用本地仓库 `C:\maven-repository`。

## 代码格式

项目使用 `.editorconfig`、`checkstyle.xml` 和 `eclipse-formatter.xml` 统一代码格式：

- Java 使用 4 个空格缩进，单行不超过 120 个字符。
- Java 注解参数、方法参数和构造器参数使用紧凑格式；不超过 120 字符时不强制换行，超过 120 字符再自动换行。
- Java 短方法体、普通代码块、`if`/循环代码块和 lambda block 不保留单行，统一展开成多行。
- YAML 使用 2 个空格缩进。
- Markdown 保留行尾空格，避免破坏手写换行。

推荐使用 Maven formatter 做全项目格式化，IDE 只作为辅助，不应引入不同格式规则。IntelliJ IDEA 建议启用 EditorConfig
支持，并读取仓库中的 `.editorconfig` 约束。命令行使用：

```powershell
mvn formatter:format
mvn formatter:validate
mvn validate
git diff --check
```

说明：

- `mvn formatter:format` 一键格式化所有模块的 `src/main/java` 和 `src/test/java`。
- `mvn formatter:validate` 校验当前代码是否符合 `eclipse-formatter.xml`。
- `mvn validate` 执行全模块 Checkstyle 校验。
- `git diff --check` 检查行尾空格、空白错误等 Git 级格式问题。
- Formatter 使用 `eclipse-formatter.xml`，保持 Java 17、4 空格缩进、LF 换行、120 行宽、参数紧凑换行和短 block
  自动展开约束。

## 验证

常用命令：

```powershell
mvn validate
mvn test
mvn verify -DskipITs=false
mvn -DskipITs=false test-compile failsafe:integration-test failsafe:verify
mvn -Pcore-services verify
mvn -Pbusiness-platforms verify
mvn -Pdata-platforms verify
mvn -Ptrust-governance verify
mvn -Pproduction-control verify
mvn -Pstable-runtime verify
```

说明：

- `mvn validate` 执行 Checkstyle。
- `mvn test` 执行 Surefire 测试，即常规单元测试阶段。
- `mvn verify -DskipITs=false` 执行完整构建、Surefire 测试、打包和 Failsafe 集成测试。
- `mvn -DskipITs=false test-compile failsafe:integration-test failsafe:verify` 只执行 Failsafe 集成测试阶段。
- 不要用 `mvn -DskipTests -DskipITs=false verify` 作为集成测试命令；`skipTests` 会让 Failsafe 也跳过测试。
- Testcontainers 测试需要 Docker daemon 正常运行。
- Windows 11 + Docker Desktop 场景下，如果 Maven/Testcontainers 没有自动识别 Docker Linux Engine，可先执行
  `$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'`。
- Smoke 真实环境测试需要设置 `EMALL_RUN_*_IT` 环境变量。

最近一次本地验证结果：

- `mvn -DskipITs=false verify`：44 个模块全部通过。
- Testcontainers 已实际运行 MySQL、Kafka 和 Redis 相关集成测试。
- `smoke` 真实服务端到端测试默认跳过，需要启动对应服务并设置 `EMALL_RUN_*_IT` 开关后执行。

## 压测

Java 17 压测工具位于 `loadtest` 模块。默认场景是 `checkout`：

```powershell
mvn -pl loadtest exec:java
```

可指定参数：

```powershell
mvn -pl loadtest exec:java -Dexec.args="http://localhost:8080 100 60 200 checkout"
```

参数顺序为：`baseUrl`、`ratePerSecond`、`durationSeconds`、`maxConcurrency`、`scenario`。

支持场景：

- `checkout`
- `read-heavy`
- `hot-sku`
- `payment-callbacks`
- `mq-backlog`

大促或热点 SKU 前，建议初始化库存桶：

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/inventory/10001/buckets `
  -ContentType application/json -Body '{"bucketCount":64}'
```

## 内部运维接口

内部运维接口用于补偿、重放、对账和恢复，不应暴露到公网。请求需要 `X-Internal-Token`，并建议携带
`X-Operator` 和 `X-Trace-Id`。

```powershell
$headers = @{
  "X-Internal-Token" = $env:EMALL_INTERNAL_OPERATIONS_TOKEN
  "X-Operator" = "ops-user"
  "X-Trace-Id" = [guid]::NewGuid().ToString("N")
}

Invoke-RestMethod -Method Post -Headers $headers `
  -Uri "http://localhost:8084/internal/operations/orders/retry-pending?limit=100"

Invoke-RestMethod -Method Post -Headers $headers `
  -Uri "http://localhost:8086/internal/operations/payments/reconcile-channel-statements?limit=100"
```

典型用途：

- 重试 `PENDING_RETRY` 订单。
- 重放失败 Outbox。
- 释放过期库存预占。
- 重试支付后的订单确认。
- 导入渠道账单并执行对账。

## 文档

- [文档索引](docs/README.md)
- [架构设计](docs/architecture.md)
- [设计深度说明](docs/design-deep-dive.md)
- [技术能力地图](docs/technical-skill-map.md)
- [学习手册](docs/learning/README.md)
- [模块清单](docs/modules.md)
- [数据平台](docs/data-platform.md)
- [集成测试](docs/integration-testing.md)
- [生产检查清单](docs/production-checklist.md)

## Kubernetes 基线

`ops/k8s` 目录包含在线服务的 Kubernetes 部署基线，包括 Deployment、Service、PDB、HPA、健康检查、
资源配置和优雅关闭。真实集群使用前必须修改镜像仓库、Secret、域名、证书、资源规格和安全策略。

更多说明见：

- [运维配置索引](ops/README.md)
- [Kubernetes 基线](ops/k8s/README.md)
