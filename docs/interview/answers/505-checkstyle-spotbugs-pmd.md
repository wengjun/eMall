# 505 Checkstyle、SpotBugs、PMD 分别解决什么问题？

[返回按分类学习面试题](../README.md)

## 题目

Checkstyle、SpotBugs、PMD 分别解决什么问题？

## 先给面试官的短答案

Checkstyle 主要检查代码风格和格式规范，SpotBugs 通过字节码分析发现潜在 bug，PMD 通过源码规则发现坏味道、
复杂度和可维护性问题。三者关注点不同，可以组合成 Java 项目的静态质量门禁。

## Checkstyle

Checkstyle 关注编码规范，例如缩进、行长、命名、导入顺序、Javadoc、空格和大括号风格。
它的价值是让团队代码风格统一，降低评审时的格式争论。

Checkstyle 不擅长发现复杂运行时 bug，它更像“代码格式和规范裁判”。

## SpotBugs

SpotBugs 分析编译后的字节码，能发现空指针风险、资源未关闭、错误的 equals/hashCode、并发可见性问题、
日期格式线程安全问题和可疑的异常处理。

它更偏 bug 检测，适合发现代码审查中容易遗漏的缺陷。

## PMD

PMD 分析源代码，关注重复代码、复杂方法、过长类、未使用变量、空 catch、过多分支和设计坏味道。
它更偏可维护性和代码质量趋势。

PMD 的规则要谨慎配置，过多低价值规则会制造噪音。

## 在 eMall 项目中怎么讲？

eMall 已使用 Checkstyle 约束 Java17 代码风格。后续可以按模块引入 SpotBugs 和 PMD，把空指针、资源泄漏、
复杂度和坏味道纳入 CI 门禁，但要先调优规则，避免误报阻塞开发。

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
这三个工具解决的是不同层次的静态质量问题。

Checkstyle 解决风格一致性，SpotBugs 解决潜在缺陷，PMD 解决源码层面的坏味道和复杂度。
它们不能替代测试，但能在代码进入运行前发现一部分低成本问题。

我会把它们放入 CI，并根据团队实际调优规则。静态检查的目标是提高质量，而不是制造大量无意义噪音。
```

## 回答评分点

高分答案应该覆盖：

- 能分别说明三者关注点。
- 知道 Checkstyle 偏格式规范。
- 知道 SpotBugs 偏字节码 bug 检测。
- 知道 PMD 偏源码坏味道和复杂度。
- 能说明规则需要调优，避免噪音。
