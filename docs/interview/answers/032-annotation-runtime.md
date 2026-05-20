# 032 注解是如何在运行时生效的？

[返回按分类学习面试题](../README.md)

## 题目

注解是如何在运行时生效的？

## 先给面试官的短答案

注解本身只是元数据，不会自动产生行为。它能否生效，取决于是否有编译器、框架或运行时代码读取它并处理。

例如 `@Service` 被 Spring 扫描后注册成 Bean；`@Transactional` 被 Spring AOP 创建代理后才有事务；
`@NotNull` 被 Bean Validation 处理器读取后才会校验。

## 从零基础理解

写一个注解：

```java
@Service
public class OrderService {
}
```

`@Service` 本身不会 new 对象，也不会执行业务逻辑。Spring 启动时扫描类路径，
发现这个类有 `@Service`，才把它注册到容器中。

所以注解是“声明”，框架处理器才是“执行者”。

## 注解的关键元注解

### `@Retention`

决定注解保留到什么时候：

- `SOURCE`：只在源码中存在。
- `CLASS`：编译进 class 文件，但运行时不一定可读。
- `RUNTIME`：运行时可通过反射读取。

如果希望运行时框架处理，通常需要 `RUNTIME`。

### `@Target`

决定注解能标在哪里：

- 类。
- 方法。
- 字段。
- 参数。
- 构造函数。

### `@Inherited`

表示类级注解是否可被子类继承，但它有很多限制，不能滥用。

## Spring 注解如何生效？

### `@SpringBootApplication`

组合了：

- `@Configuration`
- `@EnableAutoConfiguration`
- `@ComponentScan`

Spring Boot 启动时会扫描组件、加载自动配置、创建 Bean。

### `@Service`

被组件扫描发现，注册为 Bean。

### `@Transactional`

Spring 创建代理对象。外部调用代理方法时，代理开启事务、调用目标方法、根据异常提交或回滚。

如果同一个类内部 `this.method()` 调用，可能绕过代理，事务不生效。

### `@Validated` 和 `@Valid`

Bean Validation 处理器读取字段上的校验注解，例如 `@NotBlank`、`@Positive`。

## 常见不生效原因

- 注解没有运行时保留。
- 类不在 Spring 扫描路径下。
- 方法不是通过 Spring 代理调用。
- `@Transactional` 标在 private 方法上。
- 同类内部自调用绕过代理。
- 缺少对应依赖或处理器。
- AOP 代理类型和方法可见性不匹配。

## 在 eMall 项目中怎么讲？

eMall 中：

- `@RestController` 让类成为 HTTP Controller。
- `@Service` 注册业务服务。
- `@Transactional` 控制本地事务。
- `@ConfigurationProperties` 绑定配置。
- `@ControllerAdvice` 统一异常处理。
- Bean Validation 注解拦截非法请求。

专家级表达：

```text
注解不是魔法。它必须被框架扫描、解析并转成具体行为。
理解注解背后的处理机制，才能解释为什么事务、校验、配置绑定有时不生效。
```

## 深度增强：工程化理解图

![Java 工程能力从语法到生产设计](../assets/java-engineering-model.svg)

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

## 回答评分点

高分答案应该覆盖：

- 注解只是元数据。
- 行为来自框架处理器或编译器。
- 能说明 Retention 和 Target。
- 能解释 Spring 扫描、代理、校验。
- 能指出事务自调用等不生效原因。

## 深度完善：面向 L6 的回答框架

围绕「注解是如何在运行时生效的？」，高分答案不能停在概念定义，而要把「语言特性、建模边界、兼容性和团队编码规范」讲成一条可验证的工程链路。
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
本题复习重点：注解是如何在运行时生效的？

- 先看本文的题目专属答案，再按共享框架补齐项目落点、失败路径、取舍和验收。
- 白板复述时用结论 -> 例子 -> 风险 -> 指标四层结构。
