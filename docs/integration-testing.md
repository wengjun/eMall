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

- `common`：Flyway schema、Kafka Outbox、Redis 原语、JDBC 分布式任务锁。
- `smoke`：下单端到端、补偿恢复、对账、可观测、网关路由、幂等、内部操作安全。
- `chaos`：混沌清单、Kubernetes 清单、Docker Compose 拓扑、可观测配置、安全和灰度清单。
- 所有业务模块：至少有基础模块集成测试，用于验证模块可编译、可加载和核心服务可用。

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
mvn -pl smoke -DskipITs=false verify
```

如果这些变量未设置，测试会被跳过，而不是失败。

## 执行要求

- 常规单元测试应尽量不依赖 Docker。
- 当前 `user` 模块的 `UserRepositoryIntegrationTest` 会被 Surefire 单元测试阶段捕获，因此执行 `mvn test`
  时也可能启动 Testcontainers/MySQL。后续如果要严格区分阶段，可以把它重命名为 `UserRepositoryIT`。
- Testcontainers 测试需要 Docker daemon 正常运行。
- 真实 E2E 测试需要先启动 MySQL、Redis、Kafka、Elasticsearch 和相关服务。
- 快速 CI 可以先跑 `mvn validate` 和 `mvn test`。
- 具备 Docker 的 CI 环境再跑 `mvn verify -DskipITs=false` 或单独的 Failsafe 集成测试命令。

## 本地限制

当前机器如果 Docker Desktop 未启动，Testcontainers 测试会自动跳过。此时 `mvn verify -DskipITs=false`
仍然可以通过，但不能说明真实 MySQL、Redis、Kafka 集成路径已经执行。
