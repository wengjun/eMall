# 集成测试

本文记录 eMall 的生产级集成测试分层、当前覆盖、环境开关和本地限制。

## 执行分层

- Surefire 测试：`mvn test`，执行 Maven 单元测试阶段，默认匹配 `*Test.java`。
- 完整验证：`mvn verify -DskipITs=false`，执行编译、Checkstyle、Surefire、打包和 Failsafe 集成测试。
- 只跑 Failsafe 集成测试：`mvn -DskipITs=false test-compile failsafe:integration-test failsafe:verify`。
- Testcontainers 测试：需要 Docker，用真实 MySQL、Redis、Kafka 等组件验证行为。
- Smoke 测试：需要真实运行的服务，并通过 `EMALL_RUN_*_IT` 环境变量显式开启。
- 配置和清单测试：验证 Kubernetes、Docker Compose、Prometheus、Grafana、混沌清单等文件。

不要用 `mvn -DskipTests -DskipITs=false verify` 作为“只跑集成测试”的命令；`skipTests` 会让 Failsafe 也跳过测试。

## 当前覆盖

- `common`：MySQL 事务、Kafka Outbox/DLT、Redis 原语、Snowflake worker 租约、分片路由与路由索引。
- `smoke`：带真实身份令牌的下单支付闭环、补偿恢复、对账、可观测、网关路由、幂等和内部操作安全。
- `chaos`：混沌清单、Kubernetes 清单、Docker Compose 拓扑、可观测配置、安全和灰度清单。
- 订单、支付、身份、库存、营销等核心模块使用行为测试验证权限边界、资金安全、Saga、资源回收和并发限制；
  已删除只读取源码并断言字符串的伪集成测试。

## 环境开关

Smoke 集成测试默认不会访问真实环境，必须显式设置环境变量：

```powershell
$env:EMALL_RUN_CHECKOUT_IT="true"
$env:EMALL_RUN_COMPENSATION_IT="true"
$env:EMALL_RUN_RECONCILIATION_IT="true"
$env:EMALL_RUN_OBSERVABILITY_IT="true"
$env:EMALL_RUN_GATEWAY_CONTRACT_IT="true"
$env:EMALL_RUN_IDEMPOTENCY_IT="true"
$env:EMALL_RUN_INTERNAL_SECURITY_IT="true"
$env:EMALL_RUN_FLASH_SALE_IT="true"
$env:EMALL_RUN_DATA_PLATFORM_IT="true"
$env:EMALL_RUN_COST_GOVERNANCE_IT="true"
$env:EMALL_RUN_RELEASE_TRAFFIC_IT="true"
$env:EMALL_SMOKE_SETUP_ACCESS_TOKEN="<具备 pricing、inventory、fulfillment 服务权限的访问令牌>"
$env:EMALL_PAYMENT_CALLBACK_SECRET="<与 payment 服务 mock 渠道一致的回调密钥>"
mvn -pl smoke -DskipITs=false verify
```

`EMALL_RUN_*_IT` 未设置时，对应真实环境测试会被跳过。开启结账测试时，两个密钥变量必须与目标环境配置一致；
测试客户端会自行注册顾客账号并登录，业务请求不再依赖可伪造的用户请求头。

## 执行要求

- 常规单元测试应尽量不依赖 Docker。
- Testcontainers 测试需要 Docker daemon 正常运行。
- 真实 E2E 测试需要先启动 MySQL、Redis、Kafka、Elasticsearch 和相关服务。
- 快速 CI 可以先跑 `mvn validate` 和 `mvn test`。
- CI 使用 `-Demall.integration.require-docker=true` 执行完整验证；Docker 不可用时必须失败，不能静默跳过基础设施测试。

## 本地限制

当前机器如果 Docker Desktop 未启动，Testcontainers 测试会自动跳过。此时 `mvn verify -DskipITs=false`
仍然可以通过，但不能说明真实 MySQL、Redis、Kafka 集成路径已经执行；发布门禁必须以具备 Docker 的 CI 结果为准。
