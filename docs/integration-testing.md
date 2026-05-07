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

## 生产集成测试 Backlog

以下 PIT 项均已在文档和测试中建立完成标记：

- PIT-00：集成测试总入口和执行规范。
- PIT-01：数据库迁移和 schema 验证。
- PIT-02：Redis 运行时原语。
- PIT-03：Kafka Outbox 发布。
- PIT-04：核心交易 E2E。
- PIT-05：补偿恢复。
- PIT-06：支付对账。
- PIT-07：可观测性。
- PIT-08：网关路由契约。
- PIT-09：幂等契约。
- PIT-10：内部操作安全。
- PIT-11：混沌清单覆盖。
- PIT-12：Kubernetes 运行时清单。
- PIT-13：Docker Compose 拓扑。
- PIT-14：Prometheus 和 Grafana 配置。
- PIT-15：密钥和服务网格安全。
- PIT-16：灰度发布清单。
- PIT-17：生产集成测试完成标记。

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
- 真实 E2E 测试需要先启动 MySQL、Redis、Kafka、OpenSearch 和相关服务。
- 快速 CI 可以先跑 `mvn validate` 和 `mvn test`。
- 具备 Docker 的 CI 环境再跑 `mvn verify -DskipITs=false` 或单独的 Failsafe 集成测试命令。

## 本地限制

当前机器如果 Docker Desktop 未启动，Testcontainers 测试会自动跳过。此时 `mvn verify -DskipITs=false`
仍然可以通过，但不能说明真实 MySQL、Redis、Kafka 集成路径已经执行。

## 最近一次本地结果

2026-05-07 在 Docker Desktop 可用的本地环境中验证：

- 完整构建：`mvn verify -DskipITs=false`，43 个模块全部通过，130 个测试，0 failures，0 errors，7 skipped。
- Surefire 阶段：`mvn -DskipITs test`，113 个测试，0 failures，0 errors，0 skipped。
- Failsafe 阶段：`mvn -DskipITs=false test-compile failsafe:integration-test failsafe:verify`，17 个测试，
  0 failures，0 errors，7 skipped。
- 7 个 skipped 都来自 `smoke` 模块真实服务端到端测试；未设置 `EMALL_RUN_*_IT` 时按设计跳过。
