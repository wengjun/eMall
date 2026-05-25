# 025 泛型擦除是什么？

[返回按分类学习面试题](../README.md)

## 题目

泛型擦除是什么？

## 先给面试官的短答案

Java 泛型主要在编译期提供类型检查。编译后，大部分泛型类型信息会被擦除，
运行时通常只知道原始类型，不知道 `List<String>` 还是 `List<Long>`。

这就是泛型擦除。它影响反射、序列化、泛型数组、运行时类型判断和通用框架设计。

## 从零基础理解

写代码时：

```java
List<String> names = new ArrayList<>();
List<Long> ids = new ArrayList<>();
```

编译器会检查：

```java
names.add("alice"); // ok
names.add(1L);      // compile error
```

但运行时，这两个对象主要都是 `ArrayList`。

你不能这样判断：

```java
if (names instanceof List<String>) {
}
```

因为运行时没有完整的 `List<String>` 信息。

## 为什么 Java 要擦除？

主要是历史兼容。Java 早期没有泛型，后来为了兼容老代码和 JVM 字节码模型，引入了类型擦除方案。

好处：

- 兼容旧版本集合代码。
- 不需要为每种泛型生成一份新类。

代价：

- 运行时泛型信息不完整。
- 某些代码写起来不直观。

## 泛型擦除带来的限制

### 不能直接 new T

```java
public class Factory<T> {
    public T create() {
        return new T(); // compile error
    }
}
```

因为运行时不知道 T 是什么。

通常要传入：

```java
private final Class<T> type;
```

### 不能创建泛型数组

```java
T[] values = new T[10]; // compile error
```

### 不能判断参数化类型

```java
if (value instanceof List<String>) {
}
```

不允许。

### JSON 反序列化需要额外类型信息

如果要反序列化：

```java
ApiResponse<List<OrderResponse>>
```

运行时需要 `TypeReference` 这类方式保留泛型信息。

## 在后端工程中的影响

### 通用 API 响应

```java
public record ApiResponse<T>(boolean success, T data, String code, String message) {
}
```

序列化时通常没问题，但反序列化 `ApiResponse<List<OrderResponse>>` 时要传完整类型。

### 通用 Repository 或 Client

如果写通用 HTTP 客户端：

```java
public <T> T get(String url, Class<T> type) {
}
```

简单类型可以用 `Class<T>`，复杂泛型要用 `ParameterizedTypeReference<T>`。

### 事件反序列化

MQ 消费时，如果 payload 是泛型对象，必须明确事件类型和 schema。

## 常见追问

### 泛型擦除后为什么还能知道方法返回 List<String>？

class 文件里可能保留 Signature 元数据，反射有时能读到声明信息。
但对象实例本身通常不知道自己是 `List<String>`。

### 泛型是不是没有用？

不是。泛型在编译期非常有用，可以提前发现类型错误，减少强制类型转换。

## 专家级完整回答

```text
Java 泛型主要是编译期类型安全机制，编译后大部分泛型类型会被擦除。
所以运行时通常只能看到原始类型，例如 ArrayList，而不知道它是 List<String> 还是 List<Long>。

这会影响 new T、泛型数组、instanceof 参数化类型和 JSON 反序列化。
在后端框架或通用 HTTP 客户端中，如果要处理 ApiResponse<List<OrderDto>>，
需要 TypeReference 或 ParameterizedTypeReference 保留完整类型信息。
```

## 回答评分点

高分答案应该覆盖：

- 泛型主要提供编译期类型安全。
- 运行时大部分泛型信息被擦除。
- 能举 `List<String>` 和 `List<Long>` 例子。
- 能说明 new T、泛型数组、instanceof 限制。
- 能联系 JSON 反序列化和通用客户端。
