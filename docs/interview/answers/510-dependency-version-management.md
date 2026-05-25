# 510 如何管理依赖版本？

[返回按分类学习面试题](../README.md)

## 题目

如何管理依赖版本？

## 先给面试官的短答案

依赖版本要集中管理、可追踪、可升级、可回滚。Maven 多模块项目应在父 POM 的 `dependencyManagement` 和
`pluginManagement` 中统一版本，业务模块只声明依赖，不分散写版本。升级时通过 CI、测试和兼容性验证控制风险。

## 为什么要集中管理

如果每个模块自己写版本，项目会出现同一个库多个版本、传递依赖冲突、插件行为不一致和安全漏洞难以修复。
集中管理可以让版本升级和安全治理更可控。

父 POM 负责统一版本，模块 POM 只负责声明“我需要这个依赖”。

## 管理内容

需要管理的不只是业务依赖，还包括 Maven 插件、测试库、编码插件、打包插件、静态检查插件和 Docker 构建插件。

还要关注传递依赖。某些库会间接引入旧版本组件，需要通过 exclusions 或 dependencyManagement 覆盖。

## 升级策略

依赖升级要分层处理。补丁版本可以更频繁，次版本需要回归测试，主版本要评估 API 兼容和行为变化。

关键依赖如 Spring Boot、MyBatis Plus、数据库驱动、Kafka 客户端和 Netty，要阅读 release notes，
并在核心模块跑集成测试。

## 在 eMall 项目中怎么讲？

eMall 已经通过外层 POM 提取公共依赖和插件版本。后续可以引入版本更新检查、依赖树审计和安全扫描，
确保 43 个模块不会出现版本漂移。

## 深度增强：现场编码工程化图

![现场编码题的工程化解法](../assets/coding-patterns.svg)

现场编码题不只是写出算法，还要说明输入输出、边界条件、复杂度、线程安全和可测试性。
面试官通常更看重思考过程、代码结构和验证意识，而不是只看最终代码。

## 深度增强：Java 17 编码模板示例

```java
import java.util.LinkedHashMap;
import java.util.Map;

final class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    LruCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

这段代码展示现场编码的表达方式：先选合适数据结构，再说明复杂度和边界。
若用于生产，还要考虑并发、监控、容量和淘汰策略。

## 深度增强：生产边界

面试中的简化实现通常不是生产实现。生产需要线程安全、容量限制、指标、异常处理、单元测试和压测验证。
如果题目涉及分布式场景，还要说明单机实现和多实例实现的差异。

## 深度增强：面试高分表达

我会先澄清需求和边界，再写最小正确实现，最后补充复杂度、测试用例和生产化改造。
这样即使代码题不复杂，也能体现工程成熟度。

## 专家级完整回答

```text
依赖版本管理的目标是避免版本漂移和不可控升级。

在 Maven 多模块项目中，我会把依赖和插件版本放在父 POM 的 dependencyManagement 和 pluginManagement。
业务模块只声明依赖，不重复写版本。

升级依赖时不能只改版本号。要看兼容性、传递依赖、安全公告和测试结果。
对核心框架和中间件客户端，要通过集成测试和灰度验证再进入生产。
```

## 回答评分点

高分答案应该覆盖：

- 说明依赖版本要集中管理。
- 知道 `dependencyManagement` 和 `pluginManagement` 的作用。
- 能说明传递依赖和冲突风险。
- 知道升级要结合测试、兼容和 release notes。
- 能结合 Maven 多模块项目说明。
