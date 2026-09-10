# 032 注解是如何在运行时生效的？

[返回按分类学习面试题](../README.md)

## 注解只是元数据

编译器、注解处理器或运行时框架读取注解后执行逻辑；注解本身不会调用方法。

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Audited {
    String value();
}

class OrderActions {
    @Audited("cancel")
    public void cancel() {
    }
}
```

读取方式为 `OrderActions.class.getMethod("cancel").getAnnotation(Audited.class)`。
这仅拿到标记，要记录审计仍需拦截器、AOP 或显式调用。

| 保留策略 | 能否运行时反射读取 | 典型用途 |
| --- | --- | --- |
| SOURCE | 不能，编译时丢弃 | 编译期提示、处理 |
| CLASS | 不能，保留在 class 文件 | 字节码工具 |
| RUNTIME | 可以 | 运行时框架 |

`@Inherited` 只影响类注解沿父类继承，不自动覆盖接口、方法注解。
Spring 对组合注解的查找也不等于直接调用 JDK 反射 API。

遇到 `@Transactional` 不生效，检查代理与调用入口，而不是怀疑 JVM 没有“执行注解”。
具体机制看 [120](120-spring-aop-proxy.md)。
