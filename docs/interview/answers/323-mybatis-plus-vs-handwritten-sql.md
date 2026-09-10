# 323 MyBatis-Plus 和手写 SQL 如何取舍？

[返回按分类学习面试题](../README.md)

## 先去掉一个误解

MyBatis-Plus 不是“不执行 SQL”，而是基于 MyBatis 生成常用 SQL。
CRUD、条件更新、分页可以用 BaseMapper 和 Wrapper；复杂联表、方言语法或难以表达的查询继续用 XML/注解。
**高并发本身不是必须手写 SQL 的理由，最终生成的 SQL 与数据库约束才是依据。**

假设 OrderEntity 是常规可映射实体，OrderMapper 继承 BaseMapper：

```java
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

var update = new LambdaUpdateWrapper<OrderEntity>()
        .eq(OrderEntity::getId, orderId)
        .eq(OrderEntity::getTenantId, tenantId)
        .eq(OrderEntity::getStatus, "CREATED")
        .set(OrderEntity::getStatus, "CANCELLED");

int changed = orderMapper.update(null, update);
if (changed != 1) {
    throw new IllegalStateException("order missing or status changed");
}
```

条件和新值由参数绑定传入，数据库在一条 UPDATE 中完成条件判断。
这与手写 `UPDATE ... WHERE id = ? AND status = ?` 的并发语义可以一致，
不需要为了条件更新退回原始 JDBC。Wrapper API 见
[MyBatis-Plus 条件构造器](https://baomidou.com/guides/wrapper/)。

## Java 层必须留意

Wrapper 是可变构造对象，不应作为单例在请求间共享。
实体字段名、逻辑删除、租户与乐观锁插件都可能改变最终 SQL；用真实数据库测试确认行为。
`last`、`apply`、`setSql` 接收 SQL 片段，不等于任意输入都会自动安全化，不能拼接用户输入。

读取生成 SQL、检查参数和影响行数仍是学习重点。执行链见
[690](690-mybatis-execution-pipeline.md)，插件顺序见 [692](692-mybatis-plugin-mybatisplus-interceptor.md)。
