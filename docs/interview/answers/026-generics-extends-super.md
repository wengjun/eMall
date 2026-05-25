# 026 泛型通配符 `extends` 和 `super` 怎么理解？

[返回按分类学习面试题](../README.md)

## 题目

泛型通配符 `extends` 和 `super` 怎么理解？

## 先给面试官的短答案

可以用 PECS 原则理解：Producer Extends, Consumer Super。
如果一个泛型结构主要生产数据给你读取，用 `? extends T`；
如果它主要消费你写入的数据，用 `? super T`。

简单说：

- `? extends Order`：可以安全读出 Order，但不适合写入具体对象。
- `? super PaidOrder`：可以安全写入 PaidOrder，但读出来只能当 Object 或上界处理。

## 从零基础理解

假设：

```java
class Order {
}

class PaidOrder extends Order {
}
```

### `extends` 适合读取

```java
void printOrders(List<? extends Order> orders) {
    for (Order order : orders) {
        System.out.println(order);
    }
}
```

调用方可以传：

```java
List<Order>
List<PaidOrder>
```

因为不管里面具体是哪种 Order 子类，读出来都至少是 Order。

但你不能安全 add 一个普通 `Order`：

```java
orders.add(new Order()); // compile error
```

因为实际传入的可能是 `List<PaidOrder>`。

### `super` 适合写入

```java
void addPaidOrder(List<? super PaidOrder> orders, PaidOrder paidOrder) {
    orders.add(paidOrder);
}
```

调用方可以传：

```java
List<PaidOrder>
List<Order>
List<Object>
```

因为这些集合都能接收一个 PaidOrder。

但读出来时类型只能安全看作 Object：

```java
Object value = orders.get(0);
```

## PECS 原则

```text
Producer Extends
Consumer Super
```

意思是：

- 你从它里面读数据，它是生产者，用 extends。
- 你往它里面写数据，它是消费者，用 super。

## 后端工程中的例子

### 批量处理订单只读

```java
void exportOrders(List<? extends Order> orders) {
    for (Order order : orders) {
        // Read order data.
    }
}
```

### 收集处理结果

```java
void collectPaidOrders(List<? super PaidOrder> target, PaidOrder paidOrder) {
    target.add(paidOrder);
}
```

## 不要过度使用复杂泛型

业务代码可读性很重要。如果泛型写得太复杂，新人很难理解。

公共库和框架层可以适当使用通配符提升扩展性；普通业务代码优先简单清晰。

## 专家级完整回答

```text
extends 和 super 可以用 PECS 理解。一个集合如果主要被我读取，它生产 T，
就用 ? extends T；如果主要被我写入，它消费 T，就用 ? super T。

? extends Order 能让我安全读出 Order，但不能安全写入具体 Order；
? super PaidOrder 能让我安全写入 PaidOrder，但读出来只能当 Object 处理。
在公共库 API 中合理使用通配符能提升扩展性，但业务代码要避免过度复杂。
```

## 回答评分点

高分答案应该覆盖：

- 能说出 PECS。
- 能解释 extends 适合读。
- 能解释 super 适合写。
- 能举父类子类集合例子。
- 能强调业务代码可读性。
