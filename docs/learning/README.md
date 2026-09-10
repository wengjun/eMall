# 项目代码导读

[项目首页](../../README.md) | [文档索引](../README.md) | [Java 技术栈题库](../interview/README.md)

本目录面向已有服务端开发经验、希望熟悉本工程 Java 实现的读者。只解释实际代码中的调用关系、
事务边界和框架接入，不重复教授通用服务端设计、零基础语法或面试表达。

## 阅读顺序

| 顺序 | 导读 | 阅读目标 |
| --- | --- | --- |
| 1 | [下单与恢复](checkout-and-recovery.md) | 从 HTTP 入口跟到幂等、Saga、短事务及失败恢复 |
| 2 | [持久化与消息](persistence-and-messaging.md) | 跟踪 MyBatis-Plus 映射、条件更新、Outbox 发布和消费事务 |
| 3 | [运行配置与验证](runtime-and-verification.md) | 找到 Spring 装配、HTTP/Dubbo 切换、恢复控制和测试入口 |

每篇按“源码入口、关键行为、验证方式”组织。建议在 IDEA 中打开链接对应的类，沿调用链调试；
代码片段只摘录局部机制，不是另一套可直接部署的实现。

## 文档分工

- Java、JVM、Spring 和客户端 API 的原理：查[分类题库](../interview/README.md)。
- 服务边界与整体架构图：查[架构设计](../architecture.md)；全部模块职责查[模块清单](../modules.md)。
- 业务数据流图和设计取舍：查[设计深度说明](../design-deep-dive.md)及[架构决策](../README.md#架构决策)。
- 构建、格式化和 Windows 启动：查[项目首页](../../README.md)，本目录不重复维护命令。
- 配置值、部署、测试开关和验收要求：使用各篇链接到的专门文档，不以教学示例代替运行配置。

本导读解释的是代码行为，不是生产规模验收报告；目标容量是否达标仍以
[容量验证](../capacity-verification.md)和[生产检查清单](../production-checklist.md)要求的证据为准。
