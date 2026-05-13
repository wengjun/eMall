# 文档索引

[项目首页](../README.md) | [学习手册](learning/README.md) | [运维配置索引](../ops/README.md)

本文是项目文档入口。建议先从架构、深度设计、模块清单和路线图读起，再看运维、测试、数据和安全文档。

## 建议先读

- [架构设计](architecture.md)：系统目标、分层、服务边界、交易链路和一致性策略。
- [设计深度说明](design-deep-dive.md)：设计思路、数据流图、关键技术细节、竞品对比和面试讲法。
- [技术能力地图](technical-skill-map.md)：手动实现本工程需要掌握的 Java 后端技术点导航。
- [学习手册](learning/README.md)：按主题拆分后的深入学习文档，适合系统学习。
- [国内互联网技术栈适配](domestic-stack.md)：Sentinel、MyBatis-Plus、Nacos、Dubbo、Elasticsearch、ClickHouse 和 ELK 的接入说明。
- [持久层规范](persistence-conventions.md)：MyBatis-Plus、强类型 Mapper、审计字段、乐观锁和 SQL 约定。
- [国内生产扩展说明](domestic-stack.md#生产扩展基线)：分库分表、多级缓存、Redis Cluster 和 Helm 部署基线。
- [模块清单](modules.md)：所有 Maven 模块、职责分组和构建 profile。
- [路线图](roadmap.md)：P0 到 P14 的阶段规划和完成标记。
- [生产检查清单](production-checklist.md)：上线前需要检查的核心事项。
- [仓库整理说明](repository-cleanup.md)：文档、配置和本地文件的整理原则。

## 构建和验证

- [集成测试](integration-testing.md)：生产级集成测试清单、执行方式和环境开关。
- [稳定运行就绪度](stable-runtime-readiness.md)：稳定运行所需范围、完成标记和上线前缺口。

## 运维

- [SLO 和故障手册](slo-and-runbooks.md)：SLO、告警、故障处理流程和容量策略。
- [可观测性](observability.md)：指标、日志、链路追踪和 trace 验证。
- [安全加固](security-hardening.md)：边缘安全、服务间安全、数据保护、风控和密钥轮换。
- [混沌工程](chaos-engineering.md)：混沌演练目录、执行流程和生产边界。
- [成本治理](cost-governance.md)：成本风险信号、预算和 FinOps 操作。

## 数据和规模

- [数据平台](data-platform.md)：分库分表、归档、冷热分层、Outbox 和异步同步。
- [多区域策略](multi-region-strategy.md)：区域归属、路由规则和故障切换边界。

## 阶段说明

- [未来路线 Backlog](future-roadmap.md)：非必需的远期能力清单。
- [阶段说明](phase-notes.md)：P4 到 P8 的模块说明和生产扩展边界。

## 部署资产

- [Kubernetes 基线](../ops/k8s/README.md)：Kubernetes 清单使用方式和假设。
- [Helm 部署基线](../ops/helm/emall)：面向国内云原生部署的 Helm Chart 示例。
- [运维配置索引](../ops/README.md)：本地运行和部署配置地图。
- `../docker-compose.yml`：本地运行拓扑。
- `../ops/**`：Kubernetes、可观测、发布、混沌、MySQL 和压测基线。
