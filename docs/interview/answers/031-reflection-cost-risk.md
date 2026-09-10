# 031 Java 反射的成本和风险是什么？

[返回按分类学习面试题](../README.md)

## 关注调用链和模块边界

反射把一部分编译期检查移到运行时。核心 API 是 `Class`、`Constructor`、`Method`、`Field`，
Spring、MyBatis 常在启动时解析元数据，而不是每次请求重新扫描类。

```java
import java.lang.reflect.Method;

record OrderView(long id) {
}

Method accessor = OrderView.class.getDeclaredMethod("id");
Object value = accessor.invoke(new OrderView(42));
System.out.println(value); // 42
```

返回基本类型会装箱；目标方法抛出的异常由 `InvocationTargetException` 包装，排障要看 cause。
性能上先区分“重复查找方法”和“调用已缓存方法”，不能用一个固定倍数概括所有反射开销。

## Java 17 特别注意

`setAccessible(true)` 不是无条件绕过访问限制。模块没有开放包时可能抛出
`InaccessibleObjectException`；`trySetAccessible()` 可以探测失败。
优先使用公开 API，`--add-opens` 只应是范围明确的兼容措施，不要把依赖 JDK 私有字段当稳定方案。

缓存反射元数据时要考虑 ClassLoader 生命周期：全局 Map 强引用插件 Class 可能阻止卸载。
只有性能证据表明反射是热点，才考虑 MethodHandle 或生成代码；API 名称更“底层”不保证一定更快。
