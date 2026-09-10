# 026 泛型通配符 `extends` 和 `super` 怎么理解？

[返回按分类学习面试题](../README.md)

## 只记 Java 的类型约束

Java 泛型不协变：`List<Integer>` 不是 `List<Number>` 的子类型。与 C++ 模板实例化不同，
Java 泛型主要通过类型擦除实现；通配符约束编译器允许的操作，不会生成一套新的容器实现。

| 声明 | 读取类型 | 可安全写入的类型 |
| --- | --- | --- |
| `List<? extends Number>` | `Number` | 不能添加非 null 元素 |
| `List<? super Integer>` | `Object` | `Integer` |
| `List<Number>` | `Number` | `Number` 及其子类 |

PECS：来源生产 T，使用 `extends T`；目标消费 T，使用 `super T`。

```java
import java.util.List;

static <T> void copy(List<? extends T> source, List<? super T> target) {
    for (T value : source) {
        target.add(value);
    }
}
```

例如可把 `List<Integer>` 复制到可修改的 `List<Number>`，反方向不成立。
`extends` 不是“只读容器”：仍可能允许 `clear()`、删除等操作；不可修改性要由容器实现保证。
需要读写同一种确定类型时直接用 `List<T>`，不要机械地给所有泛型加通配符。
