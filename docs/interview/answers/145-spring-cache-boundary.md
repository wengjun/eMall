# 145 Spring Cache 的注解和失效边界是什么？

[返回按分类学习面试题](../README.md)

## 把注解、代理和缓存实现分开

`@EnableCaching` 启用相关基础设施；CacheManager 决定使用 Redis、Caffeine 或其他实现。
`@Cacheable` 查询缓存，未命中执行方法；`@CachePut` 执行方法并写缓存；
`@CacheEvict` 删除缓存。默认代理模式下，自调用仍会绕过拦截。

以下是放在受 Spring 管理的查询 Bean 中的方法片段，repository 为注入依赖：

```java
@Cacheable(cacheNames = "product", key = "#p0", unless = "#result == null")
public ProductView find(long productId) {
    return repository.find(productId);
}
```

用 #p0 可以避免依赖 Java 参数名是否被保留。key 必须包含所有影响结果的输入，
不能遗漏租户或权限维度。

## 不是加个注解就完成的部分

TTL 由 CacheManager/底层缓存配置，@Cacheable 本身没有统一的 TTL 属性。
`sync = true` 请求缓存提供方协调同一 key 的并发加载，不代表跨 JVM 的分布式锁；
它也不能随意与 unless 等选项混用。约束见
[Spring 缓存注解](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html)。

`@CacheEvict` 默认在方法成功返回后执行，但这不等于外层数据库事务已经提交。
事务最终回滚仍可能已经修改缓存；需要理解代理顺序和是否使用事务感知的 CacheManager，
不能假定两套资源天然原子提交。

不要用“库存不能缓存”代替边界判断：展示库存可以缓存，真正扣减仍必须走具有并发校验的写入路径。
