# 654 如何用 MyBatis-Plus 实现分页查询？

[返回按分类学习面试题](../README.md)

## 聚焦 Java 插件和调用

先在 MybatisPlusInterceptor 中注册 PaginationInnerInterceptor，MySQL 使用 DbType.MYSQL。
MyBatis-Plus 3.5.9 起还需关注分页所需的 mybatis-plus-jsqlparser 依赖。
有租户等插件时，分页拦截器通常放到后面，具体顺序见 [692](692-mybatis-plugin-mybatisplus-interceptor.md)。

下面是查询方法片段，orderMapper 继承 BaseMapper<OrderEntity>，tenantId 来自可信的登录上下文：

```java
public IPage<OrderEntity> page(long tenantId, long pageNo, long pageSize) {
    if (pageNo < 1 || pageNo > 10_000 || pageSize < 1 || pageSize > 100) {
        throw new IllegalArgumentException("invalid page request");
    }

    var request = new Page<OrderEntity>(pageNo, pageSize);
    var query = new LambdaQueryWrapper<OrderEntity>()
            .eq(OrderEntity::getTenantId, tenantId)
            .orderByDesc(OrderEntity::getId);

    return orderMapper.selectPage(request, query);
}
```

IPage、Page 和 LambdaQueryWrapper 来自 MyBatis-Plus。
这里页号从 1 开始；示例上限用于限制学习接口范围，不是所有业务统一用这个数值。

## Java 层的验证点

不注册分页插件可能导致只构造 Page 却没有真正限制 SQL。
默认分页通常伴随 count 查询；不需要总数时可使用关闭 searchCount 的 Page 构造方式，
调用方也必须知道此时 total 不代表真实总量。

返回实体只是演示 Mapper API，对外接口应按需要映射为 DTO。
只让后端选择合法排序字段，不把客户端字符串直接交给 last 或拼接 ORDER BY。
断言生成 SQL、首页/空页、影响租户范围及分页上限即可，不在本题重讲分页算法设计。
