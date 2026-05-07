# 稳定运行就绪度

本文说明如果目标不是“做完所有大厂能力”，而是“当前工程可以稳定运行”，需要关注哪些范围。

## 范围

稳定运行的最低目标是：

- 核心服务可以编译、测试、打包。
- 本地依赖可以启动，核心链路可以 smoke 验证。
- 下单、库存、支付的失败状态有补偿或人工恢复入口。
- 关键接口有幂等、限流、超时和基础降级。
- 日志、指标、trace 能帮助定位问题。
- 部署清单有健康检查、资源限制和优雅关闭。

## 已完成标记

- P0：核心交易闭环已完成。
- P1：高并发和可靠性基础已完成。
- P2：生产运维基线已完成。
- P3：规模、多区域和商业成熟度基础已完成。
- P4：身份、风控、运营和开放平台基础已完成。
- P5：商品、营销、搜索、推荐和实验平台基础已完成。
- P6：供应链、财务、客服和预测基础已完成。
- P7：数据、AI 和分析平台基础已完成。
- P8：流量、可靠性、发布和平台运维控制面基础已完成。
- P9-lite：稳定运行控制和护栏已完成。
- P10-lite：稳定数据一致性和任务协调已完成。
- P14-lite：稳定运行验证已完成。
- 生产级集成测试 PIT-00 到 PIT-17 已完成标记。

## 当前可以证明的事情

- `mvn test` 可以执行全部单元测试。
- `mvn verify -DskipITs=false` 可以执行完整构建和集成测试阶段。
- `mvn -DskipITs=false test-compile failsafe:integration-test failsafe:verify` 可以单独执行 Failsafe 集成测试阶段。
- 所有 Maven 模块都有基础测试。
- Docker Desktop 可用时，Testcontainers 可以拉起 MySQL、Redis、Kafka 等真实依赖执行集成测试。
- 没有 Docker 时，Testcontainers 测试会按设计跳过；此时构建通过不等于真实中间件路径已经验证。
- 没有设置 smoke 环境变量时，真实端到端测试会按设计跳过。
- 配置、部署和清单类测试可以在本地执行。

2026-05-07 最近一次本地验证结果：

- 完整构建：`mvn verify -DskipITs=false`，43 个模块全部通过，130 个测试，0 failures，0 errors，7 skipped。
- Surefire 阶段：`mvn -DskipITs test`，113 个测试，0 failures，0 errors，0 skipped。
- Failsafe 阶段：`mvn -DskipITs=false test-compile failsafe:integration-test failsafe:verify`，17 个测试，
  0 failures，0 errors，7 skipped。
- 7 个 skipped 来自 `smoke` 模块真实服务端到端测试，因为未设置 `EMALL_RUN_*_IT`。

## 真实生产前仍需要

- 启动 Docker 或真实基础设施，执行 Testcontainers 和 smoke E2E。
- 对核心链路做持续压测，得到容量基线。
- 为 MySQL、Redis、Kafka、OpenSearch 准备真实集群和备份恢复方案。
- 接入真实配置中心、服务发现、密钥管理和日志平台。
- 建立发布审批、灰度、回滚和故障演练流程。
- 补齐前端、后台、权限、审计和合规流程。

## 学习建议

如果只是为了稳定学习和展示，优先掌握：

1. 订单、库存、支付的状态流转。
2. 幂等、Outbox、补偿和对账。
3. 网关限流、熔断、降级和平滑恢复。
4. 单元测试、集成测试和 smoke 测试的区别。
5. Docker Compose 和 Kubernetes 清单分别解决什么问题。
