# 035 为什么工程代码要重视不可变对象？

[返回按分类学习面试题](../README.md)

## 不重复讲设计价值，只看 Java 写法

Java 的 final 引用不能重新赋值，但引用指向的对象仍可能被修改。record 自动生成构造器、
访问器、equals/hashCode/toString，也只提供浅层不可变性。

```java
import java.util.ArrayList;
import java.util.List;

record OrderSnapshot(long orderId, List<String> skuCodes) {
    OrderSnapshot {
        skuCodes = List.copyOf(skuCodes);
    }
}

var source = new ArrayList<>(List.of("SKU-1"));
var snapshot = new OrderSnapshot(42, source);
source.add("SKU-2");
System.out.println(snapshot.skuCodes()); // [SKU-1]
```

`List.copyOf` 防止外部列表修改影响快照，同时返回不可修改列表，但不深复制元素。
这里元素是不可变 String，因此足够；若是可变实体，需要转换为不可变值。
它也拒绝 null 元素，不能随意替换允许 null 的旧接口。

`Collections.unmodifiableList(source)` 只是包装视图，source 后续修改仍可见。
数组需要构造时复制、访问时也复制；不能只在构造器里克隆一次就把内部数组直接返回。
