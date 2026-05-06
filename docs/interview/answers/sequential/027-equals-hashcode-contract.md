# 027 `equals` 和 `hashCode` 的契约是什么？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

`equals` 和 `hashCode` 的契约是什么？

## 先给面试官的短答案

核心契约是：如果两个对象 `equals` 返回 true，它们的 `hashCode` 必须相同。
反过来不要求成立，也就是说 hash 相同不代表对象一定相等。

此外 `equals` 要满足自反性、对称性、传递性、一致性，以及和 null 比较返回 false。
这个契约对 `HashMap`、`HashSet`、缓存 key、幂等 key 都非常重要。

## 从零基础理解

`equals` 判断两个对象是否业务上相等。
`hashCode` 用于哈希容器快速定位对象。

例如：

```java
Set<SkuKey> set = new HashSet<>();
set.add(new SkuKey(10001L, 1));
```

当你查找：

```java
set.contains(new SkuKey(10001L, 1));
```

HashSet 会先用 `hashCode` 找桶，再用 `equals` 判断是否相等。

如果 `equals` 和 `hashCode` 不一致，就会出现放进去找不到的问题。

## equals 的规则

### 自反性

```java
a.equals(a) == true
```

### 对称性

```java
a.equals(b) == b.equals(a)
```

### 传递性

如果：

```text
a equals b
b equals c
```

那么：

```text
a equals c
```

### 一致性

对象状态不变时，多次调用结果一致。

### null

```java
a.equals(null) == false
```

## hashCode 的规则

如果：

```java
a.equals(b)
```

那么必须：

```java
a.hashCode() == b.hashCode()
```

但 hash 相同不代表 equals 一定 true，因为不同对象可能 hash 冲突。

## 业务系统里的坑

### 可变字段参与 hash

如果一个对象作为 HashMap key 后，参与 hash 的字段被修改，就可能找不到。

### Entity 用 ID 判断相等

数据库实体常用 ID 判断相等，但 ID 生成前怎么办？

如果新对象 ID 为 null，两个新对象是否相等？这要谨慎设计。

### Lombok 自动生成

`@Data` 会自动生成 equals/hashCode，但可能把不该参与比较的字段放进去。
例如订单对象如果把 updatedAt 放进去，更新时间变化会影响相等性。

## 推荐实践

值对象可以使用所有不可变字段：

```java
public record SkuBucketKey(long skuId, int bucketNo) {
}
```

实体对象要谨慎：

- 有稳定 ID 后，用 ID。
- ID 未生成前，避免放入 HashSet/HashMap。
- 不要让可变业务字段参与 hash key。

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
equals 和 hashCode 的核心契约是 equals 为 true 的对象 hashCode 必须相同。
equals 还要满足自反、对称、传递、一致和 null 返回 false。

这对 HashMap、HashSet、缓存 key、幂等 key 都很重要。
在业务系统里，我会让值对象使用不可变字段生成 equals/hashCode；
实体对象如果用数据库 ID，要注意 ID 生成前不要放入哈希集合。
也不会无脑用 Lombok @Data 生成领域对象的 equals/hashCode。
```

## 回答评分点

高分答案应该覆盖：

- equals true 必须 hashCode 相同。
- 能说出 equals 五个规则。
- 能解释 HashMap/HashSet 依赖。
- 能指出可变字段和实体 ID 的坑。
- 能提到 Lombok 自动生成风险。

## 深度完善：面向 L6 的回答框架

围绕「`equals` 和 `hashCode` 的契约是什么？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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

本题复习重点：`equals` 和 `hashCode` 的契约是什么？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。

