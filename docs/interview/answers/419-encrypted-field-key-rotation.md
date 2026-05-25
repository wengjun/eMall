# 419 加密字段如何做密钥轮换？

[返回按分类学习面试题](../README.md)

## 题目

加密字段如何做密钥轮换？

## 先给面试官的短答案

加密字段密钥轮换要支持多版本密钥。每条密文记录 key version，读取时按版本解密，写入时使用新
密钥，历史数据通过后台任务逐步重加密。

不能一次性停机全量改密钥，也不能丢失旧密钥导致历史数据无法解密。

## 数据格式

密文字段应保存：

- key version。
- nonce。
- ciphertext。
- auth tag。
- algorithm。

有了 key version，系统才能选择正确密钥解密。

## 轮换流程

流程：

- 在 KMS 中创建新密钥版本。
- 服务发布支持多版本解密。
- 新写入使用新密钥。
- 后台扫描旧版本数据。
- 解密后用新密钥重加密。
- 校验成功后标记版本。
- 旧密钥保留到迁移完成。

轮换要可暂停和可恢复。

## 风险控制

控制：

- 小批量处理。
- 记录进度。
- 校验解密和重加密结果。
- 失败重试。
- 监控错误率。
- 严格限制密钥访问。

密钥轮换是高风险操作。

## 在 eMall 项目中怎么讲？

eMall 手机号密文字段保存 `key_version`。当 KMS 生成 v2 密钥后，新用户手机号使用 v2 加密。

后台任务逐步把 v1 手机号解密后用 v2 重加密。迁移期间服务同时支持 v1 和 v2 解密。

## 深度增强：安全治理图

![开放平台 API 安全链路](../assets/openapi-security.svg)

安全题要从身份、权限、数据、审计和攻击面分层回答。认证只证明是谁，授权判断能做什么；
加密保护机密性，签名保护完整性；审计负责事后追踪，风控负责发现异常行为。

## 深度增强：Java 17 安全策略示例

```java
import java.util.Set;

record SecurityDecision(boolean allowed, String reason) {
}

final class ScopePolicy {

    SecurityDecision check(Set<String> scopes, String requiredScope) {
        if (scopes.contains(requiredScope)) {
            return new SecurityDecision(true, "allowed");
        }
        return new SecurityDecision(false, "missing scope: " + requiredScope);
    }
}
```

这段代码体现最小权限原则。生产系统还要结合租户、资源归属、IP、设备、风险等级和操作审计。

## 深度增强：生产边界

安全不能只靠前端隐藏按钮，也不能只在网关做一次判断。核心资源要在服务层做资源级授权，
敏感数据要脱敏，密钥要支持轮换，失败日志不能泄露 token、secret、身份证号和银行卡号。

## 深度增强：面试高分表达

我会把安全问题拆成认证、授权、防篡改、防重放、数据保护、审计和风控。
每一层解决的问题不同，不能互相替代。这能体现我理解开放平台和电商系统的真实攻击面。

## 专家级完整回答

```text
加密字段轮换要使用多版本密钥。密文记录 key version，服务支持按版本解密，新写入使用新密钥，
历史数据由后台任务分批重加密。

旧密钥不能立即删除，必须等所有历史数据迁移和校验完成。轮换任务要可暂停、可恢复、可审计，并
通过 KMS 管理密钥访问。
```

## 回答评分点

高分答案应该覆盖：

- 密文记录 key version。
- 多版本解密。
- 新写入用新密钥。
- 历史数据分批重加密。
- 旧密钥保留到迁移完成。
