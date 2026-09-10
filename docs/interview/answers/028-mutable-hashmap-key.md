# 028 为什么可变对象不适合作为 `HashMap` 的 key？

[返回按分类学习面试题](../README.md)

## 风险在参与相等性判断的字段

不是所有可变对象都不能当 key，而是**入表后不能改变影响 equals/hashCode 的状态**。
HashMap 保存插入时的哈希信息，修改 key 不会自动把它搬到新位置。

```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

var key = new ArrayList<>(List.of(1));
var map = new HashMap<List<Integer>, String>();
map.put(key, "order");

key.add(2);
System.out.println(map.get(key)); // null
System.out.println(map.size()); // 1
```

同一个引用也找不回值，因为查找先按新的哈希定位。不要试图在修改后通过 remove/put 补救；
若确实要修改，必须先用旧状态删除，但并发使用时仍很难维护。

## Java 写法

用稳定标识或不可变值对象作 key：

```java
record OrderKey(long tenantId, long orderId) {
}
```

record 只保证组件引用不可重新赋值。若组件是可变 List、数组或实体，仍需防御性复制并定义正确的相等性。
