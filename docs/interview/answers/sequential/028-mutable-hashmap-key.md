# 028 为什么可变对象不适合作为 `HashMap` 的 key？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

为什么可变对象不适合作为 `HashMap` 的 key？

## 先给面试官的短答案

`HashMap` 根据 key 的 `hashCode` 定位桶，再用 `equals` 找具体 entry。
如果 key 放入 Map 后，参与 `hashCode` 或 `equals` 的字段被修改，后续查找会计算出不同位置，
导致这个 key 明明在 Map 里却找不到。

所以 Map key 应该稳定，最好使用不可变对象，例如 `String`、`Long`、`record` 值对象。

## 从零基础理解

假设有一个可变 key：

```java
class UserKey {
    private String type;
    private String value;
}
```

放入 Map：

```java
Map<UserKey, String> map = new HashMap<>();
UserKey key = new UserKey("mobile", "15500000000");
map.put(key, "user-1");
```

如果之后改了 key：

```java
key.setValue("16600000000");
```

再查：

```java
map.get(key)
```

可能拿不到，因为 hash 位置变了。

## 为什么这在生产中危险？

在电商系统里，key 很多：

- 缓存 key。
- 幂等 key。
- 限流 key。
- 库存桶 key。
- 用户会话 key。
- MQ 去重 key。

如果 key 不稳定，会导致：

- 缓存命中率异常下降。
- 幂等失效，重复下单。
- 限流绕过。
- 去重失败。
- 内存泄漏，因为旧 key 找不到也删不掉。

## 推荐写法

使用不可变 record：

```java
public record InventoryBucketKey(long skuId, int bucketNo) {
}
```

或使用字符串 key：

```java
String key = "inventory:" + skuId + ":" + bucketNo;
```

关键是 key 创建后不要再变化。

## 如果必须使用对象作为 key？

确保：

- 字段 final。
- 参与 equals/hashCode 的字段不可变。
- 不暴露 setter。
- 集合字段做防御性拷贝。

```java
public final class IdempotencyKey {
    private final String service;
    private final String requestId;
}
```

## 深度增强：工程化理解图

![Java 工程能力从语法到生产设计](../../assets/java-engineering-model.svg)

这类题不能只停留在语法解释。生产系统更关心它如何改善建模、降低误用、保护兼容性、提升可测试性，
以及能否让团队在多人协作中保持稳定边界。回答时要从语言特性落到业务约束和工程治理。

## 深度增强：Java 17 落地示例

```java
import java.util.Objects;

record StableApiField(String name, String type, boolean required) {

    StableApiField {
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        if (name.isBlank() || type.isBlank()) {
            throw new IllegalArgumentException("API field metadata must be explicit");
        }
    }
}

final class ApiCompatibilityPolicy {

    boolean canAddField(StableApiField field) {
        return !field.required();
    }
}
```

这段代码体现 Java 17 在工程建模中的价值：用 `record` 表达不可变数据，用构造校验保护边界，
用小的策略类表达兼容规则。面试中要把语法能力和 API 演进、错误预防、团队协作联系起来。

## 深度增强：生产边界

语言特性不是越新越好。核心原则是可读、可测、可维护、可兼容。任何语法选择都要能让代码意图更清晰，
而不是为了炫技。公共 API、金额、时间、状态、异常和 DTO 都要有稳定约束，避免线上数据被随意破坏。

## 深度增强：面试高分表达

我会先回答概念，再说明它在电商系统中的真实作用。例如金额要避免精度错误，状态要可兼容扩展，
DTO 和领域对象要隔离外部契约和内部模型。这样能体现我不是只会写 Java 语法，而是能做工程设计。

## 专家级完整回答

```text
可变对象不适合作为 HashMap key，因为 HashMap 依赖 key 的 hashCode 定位桶。
如果 key 放入后参与 hashCode 或 equals 的字段发生变化，查找时会去错误的桶，
导致 entry 找不到。

在分布式服务里，缓存 key、幂等 key、限流 key、消息去重 key 都必须稳定。
我会使用 String、Long 或不可变 record 作为 key，避免 setter 和可变集合参与 hash。
```

## 回答评分点

高分答案应该覆盖：

- 能解释 HashMap 查找依赖 hashCode 和 equals。
- 能说明字段变化导致查找失败。
- 能联系缓存、幂等、限流、去重。
- 能提出不可变 key 和 record。
- 能指出内存泄漏风险。

## 深度完善：面向 L6 的回答框架

围绕「为什么可变对象不适合作为 `HashMap` 的 key？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
面试官真正关注的是：你是否知道它解决什么问题、什么时候会失效、如何在生产系统中验证。

### 1. 先界定边界

- 本题属于「Java 语言和工程基础」，先说明它影响的是正确性、稳定性、性能、安全还是协作效率。
- 不要直接背结论，要先说清业务约束、数据规模、调用链位置和失败后果。
- 如果存在多种方案，要说明默认选择、替代方案、迁移成本和放弃条件。

### 2. 结合 eMall 落地

- 可以从 `common、order、inventory、payment 的 DTO、值对象、异常和公共 API` 切入，说明它在真实电商链路中的入口、状态、数据和依赖。
- 回答时至少补一个失败路径，例如超时、重复请求、状态不一致、热点流量或配置误发。
- 再说明如何通过代码规范、测试、灰度、回滚、监控或补偿把风险收敛。

### 3. 生产级验证

- 关键指标：代码评审问题数、缺陷逃逸率、兼容性测试结果、静态检查违规数。
- 验证证据：代码规范、单元测试、契约测试、兼容性用例和重构前后缺陷数据。
- 如果没有这些证据，只能说明方案在理论上成立，不能证明它能长期稳定运行。

### 4. 追问防守

- 被问“为什么不用更简单方案”时，回答当前规模、团队能力和风险收益是否匹配。
- 被问“为什么不用更复杂方案”时，回答复杂方案的运维成本、故障面和迁移成本。
- 最后用一句话收束：先用简单可靠方案闭环，再用指标驱动演进，而不是提前复杂化。

## 补强索引

重复补强内容已合并到 [面试补强共享框架](../shared/deepening-framework.md)。

整理标记：重复内容已合并

本题复习重点：为什么可变对象不适合作为 `HashMap` 的 key？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

