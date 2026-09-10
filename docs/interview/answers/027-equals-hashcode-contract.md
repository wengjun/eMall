# 027 `equals` 和 `hashCode` 的契约是什么？

[返回按分类学习面试题](../README.md)

## 与 C++ 使用习惯的差异

Java 对象的 `==` 比较引用身份，值相等由 `equals` 定义。默认 `Object.equals` 仍比较身份。
`equals` 必须满足自反、对称、传递、一致，且对 null 返回 false。

**相等对象必须有相同 hashCode，反过来不成立。** 覆写 `equals` 时必须配套覆写 `hashCode`，
否则 `HashMap`、`HashSet` 的查找和去重会出错。

```java
record OrderKey(long tenantId, long orderId) {
}

var first = new OrderKey(10, 42);
var second = new OrderKey(10, 42);
System.out.println(first == second); // false
System.out.println(first.equals(second)); // true
System.out.println(first.hashCode() == second.hashCode()); // true
```

## 实际容易踩的坑

- record 自动生成按组件比较的实现，但数组组件默认仍按数组身份比较，不会自动深比较。
- Lombok `@Data` 不只是 getter/setter，还生成相等性方法；不要默认让实体的所有可变字段参与比较。
- `BigDecimal("1.0")` 与 `BigDecimal("1.00")` 的 `equals` 为 false，而 `compareTo` 为 0。
- 数据库实体从“没有 ID”变成“已有 ID”时，相等性不能在哈希集合中随之改变。

哈希 key 的可变性单独看 [028](028-mutable-hashmap-key.md)，不必重复学习哈希表原理。
