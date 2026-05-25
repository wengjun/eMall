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

## 回答评分点

高分答案应该覆盖：

- 注解只是元数据。
- 行为来自框架处理器或编译器。
- 能说明 Retention 和 Target。
- 能解释 Spring 扫描、代理、校验。
- 能指出事务自调用等不生效原因。
