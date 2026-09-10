# 文档索引

[项目首页](../README.md) | [项目代码导读](learning/README.md) | [运维配置索引](../ops/README.md)

本文是项目文档入口。建议先从架构、深度设计、模块清单和生产检查清单读起，再看运维、测试、数据和安全文档。

## 建议先读

- [架构设计](architecture.md)：系统目标、分层、服务边界、交易链路和一致性策略。
- [设计深度说明](design-deep-dive.md)：设计思路、数据流图、关键技术细节、竞品对比和面试讲法。
- [项目代码导读](learning/README.md)：真实源码中的下单恢复、持久化消息和运行验证，不重复基础教学。
- [按分类学习 Java 技术栈](interview/README.md)：Java、JVM、Spring 和中间件接入原理，独立于项目代码导读。
- [国内互联网技术栈适配](domestic-stack.md)：Sentinel、MyBatis-Plus、Nacos、Dubbo、Elasticsearch、ClickHouse 和 ELK 的接入说明。
- [持久层规范](persistence-conventions.md)：MyBatis-Plus、强类型 Mapper、审计字段、乐观锁和 SQL 约定。
- [Web/App 下单 API 契约](api/web-app-checkout.openapi.yml)：浏览器和手机 App 统一下单接口、请求头、请求体和响应结构。
- [容量验证说明](capacity-verification.md)：分布式 Java 压测、背压、流式直方图、证据门禁和单 Cell 容量模型。
- [国内生产扩展说明](domestic-stack.md#生产扩展基线)：分库分表、多级缓存、Redis Cluster 和 Helm 部署基线。
- [模块清单](modules.md)：所有 Maven 模块、职责分组和构建 profile。
- [生产检查清单](production-checklist.md)：上线前需要检查的核心事项。

## 构建和验证

- [集成测试](integration-testing.md)：生产级集成测试清单、执行方式和环境开关。

## 运维

- [SLO 和故障手册](slo-and-runbooks.md)：SLO、告警、故障处理流程和容量策略。
- [可观测性](observability.md)：指标、日志、链路追踪和 trace 验证。
- [安全加固](security-hardening.md)：边缘安全、服务间安全、数据保护、风控和密钥轮换。
- [混沌工程](chaos-engineering.md)：混沌演练目录、执行流程和生产边界。
- [成本治理](cost-governance.md)：成本风险信号、预算和 FinOps 操作。

## 数据和规模

- [数据平台](data-platform.md)：分库分表、归档、冷热分层、Outbox 和异步同步。
- [多区域策略](multi-region-strategy.md)：区域归属、路由规则和故障切换边界。

## 架构决策

- [ADR 001：核心交易一致性方案](adr/001-core-transaction-consistency.md)：为什么不用强 2PC。
- [ADR 002：Outbox 和消息消费幂等](adr/002-outbox-and-message-idempotency.md)：事件可靠发布和幂等消费取舍。
- [ADR 003：Sentinel 动态治理和平滑恢复](adr/003-sentinel-and-adaptive-recovery.md)：限流、熔断、降级和恢复策略。

## 部署资产

- [生产部署 Chart](../ops/helm/emall/README.md)：38 个在线服务的唯一 Kubernetes 部署事实源。
- [Kubernetes 辅助资产](../ops/k8s/README.md)：ExternalSecret、迁移过渡入口和混沌演练边界。
- [运维配置索引](../ops/README.md)：本地运行和部署配置地图。
- `../docker-compose.yml`：本地运行拓扑。
- `../ops/**`：Kubernetes、可观测、发布、混沌、MySQL 和压测基线。

## 历史记录

- [工程审查历史记录](review-history.md)：历次问题、修复决策及当时的验证证据，仅用于追溯，不作为当前实现指南。
  尚缺外部验收证据的事项汇总在[生产检查清单](production-checklist.md#待补齐的验收证据)，不因归档而视为完成。
