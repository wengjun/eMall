# 654 手写分页查询接口。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
record PageRequest(int pageNo, int pageSize) {
    PageRequest {
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("invalid page request");
        }
    }

    int offset() {
        return (pageNo - 1) * pageSize;
    }
}
```

### 必测用例

- 覆盖首页、末页、空结果、非法页号和超过最大 page size。
- 在相同排序字段值和并发插入数据时验证顺序稳定，不丢项、不重复。
- 游标被篡改、过期或与筛选条件不匹配时必须拒绝。

### 生产化差异

- 大数据集优先使用基于唯一稳定排序键的游标分页，限制最大页大小。
- 游标应签名并携带查询上下文；是否返回总数要根据查询成本取舍。
