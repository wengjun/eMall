# 123 自调用为什么绕过事务代理？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Spring 声明式事务基于 AOP 代理。只有外部通过代理对象调用事务方法时，事务拦截器才有机会执行。
同一个类内部方法调用本质是 `this.method()`，直接调用目标对象自身，不经过代理对象，因此事务切面不会触发。

解决方式是调整调用边界、拆分 Bean、使用编程式事务，或在极少数情况下通过代理对象调用。

## 代理调用路径

正常事务调用路径：

```text
caller -> proxy -> transaction interceptor -> target method
```

代理在目标方法前开启事务，方法执行后提交或回滚。

## 自调用路径

同类内部调用：

```text
target outer -> this.inner()
```

没有经过代理。

事务拦截器没有机会执行。

所以 `inner()` 上的 `@Transactional` 不生效。

## 示例

```java
@Service
public class OrderService {
    public void create() {
        saveInTransaction();
    }

    @Transactional
    public void saveInTransaction() {
        repository.save();
    }
}
```

`create()` 调用 `saveInTransaction()` 是自调用。

如果外部调用的是 `create()`，`saveInTransaction()` 的事务不会按预期开启。

## 推荐解决方式

更推荐：

- 把事务放到外部入口方法。
- 把事务方法拆到另一个 Spring Bean。
- 使用 `TransactionTemplate`。
- 重构服务职责。

不推荐为了绕过问题滥用 `AopContext.currentProxy()`。

## 为什么不建议暴露代理？

通过当前代理调用会让业务代码依赖 Spring AOP 细节。

这会降低可测试性和可维护性。

通常说明事务边界设计不清楚。

## 在 eMall 项目中怎么讲？

订单服务中应把一次本地数据库状态变更设计成明确事务边界。

例如 `OrderApplicationService.createOrder()` 作为事务入口。

内部领域方法不依赖 `@Transactional` 自调用，而是由应用服务统一控制事务。
