# 652 手写敏感字段脱敏工具。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
final class Masking {
    static String phone(String raw) {
        if (raw == null || raw.length() < 7) {
            return "***";
        }
        return raw.substring(0, 3) + "****" + raw.substring(raw.length() - 4);
    }
}
```

### 必测用例

- 覆盖手机号、邮箱、证件号的正常、最短、空值和非法输入。
- 验证掩码不会泄露超出策略允许的前后缀，也不会因 Unicode 截断破坏内容。
- 重复处理已经脱敏的数据应得到稳定结果。

### 生产化差异

- 按数据分类集中维护策略，在日志出口和序列化边界统一执行，避免各业务自行拼接。
- 脱敏不是加密；需要精确查询或恢复原文时应使用 HMAC 索引与字段加密。
