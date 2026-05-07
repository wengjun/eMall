# eMall 学习手册

本目录由原 `docs/technical-skill-map.md` 拆分而来，用于按主题学习大型 Java 电商系统。
建议先按顺序阅读，再回到具体模块代码中手写和验证。

## 阅读顺序

- [系统总览和学习方法](01-system-overview.md)
- [Amazon L6 面试导向指南](02-amazon-l6-interview-guide.md)
- [零基础概念详解](03-foundation-concepts.md)
- [代码驱动实现讲解](04-code-walkthrough.md)
- [Java、Spring Boot 和 Maven](05-java-spring-maven.md)
- [数据库和持久化](06-database-persistence.md)
- [电商核心业务建模](07-ecommerce-domain-model.md)
- [分布式一致性](08-distributed-consistency.md)
- [高并发和稳定性](09-high-concurrency-resilience.md)
- [中间件、微服务和部署平台](10-middleware-microservices-platform.md)
- [安全和可观测性](11-security-observability.md)
- [测试、工程治理和生产就绪](12-testing-governance-readiness.md)
- [从概念到实现的深度补强](13-concept-to-implementation.md)
- [学习路径和面试表达](14-learning-path-interview.md)

## 面试专项

- [面试专题入口](../interview/README.md)
- [Java 专家级分布式服务开发面试题库](../interview/question-bank.md)
- [Java 分布式服务专家级面试答案手册](../interview/answers/README.md)

## 学习建议

- 第一遍看整体，不纠结每个实现细节。
- 第二遍结合 `order`、`inventory`、`payment`、`common`、`gateway` 等模块代码阅读。
- 第三遍按文档中的关键链路手写一遍，再用单元测试、集成测试和 smoke 测试验证。
- 面试准备时重点训练容量估算、取舍分析、失败恢复、观测排障和行为面试表达。
