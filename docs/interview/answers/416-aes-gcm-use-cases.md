# 416 AES-GCM 适合什么场景？

[返回按分类学习面试题](../README.md)

## 题目

AES-GCM 适合什么场景？

## 先给面试官的短答案

AES-GCM 适合需要同时保密性和完整性校验的数据加密场景。它是认证加密模式，既能加密明文，又能
检测密文是否被篡改。

适合加密手机号、地址、证件号等敏感字段，但要正确管理 nonce、密钥和认证标签。

## 能提供什么

AES-GCM 提供：

- 机密性。
- 完整性。
- 篡改检测。
- 认证标签。
- 可附加认证数据。

相比只加密不认证的模式，GCM 更适合现代应用。

## 使用注意

注意：

- 同一密钥下 nonce 不能重复。
- 密钥要安全存储。
- 认证标签必须验证。
- 不要自己发明加密协议。
- 需要支持密钥轮换。
- 加密字段要记录 key version。

nonce 重复是严重风险。

## 适用场景

适用：

- 手机号加密。
- 地址加密。
- 身份证号加密。
- 银行卡部分敏感信息。
- 外部凭证加密。

如果还需要精确查询，可以额外保存 HMAC hash。

## 在 eMall 项目中怎么讲？

eMall 用户手机号可以用 AES-GCM 加密保存，同时保存 `phone_hash` 用于精确查询。

密文字段保存 nonce、ciphertext、tag 和 key version。密钥由 KMS 管理，轮换时按版本逐步重加密。

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
AES-GCM 是认证加密模式，适合同时需要保密和防篡改的字段级加密。它不仅加密数据，还通过认证标签
验证密文是否被篡改。

使用时要保证同一密钥下 nonce 不重复，密钥由 KMS 管理，字段保存 key version，并设计密钥轮换。
需要查询时不要解密全表扫描，可额外保存 HMAC hash。
```

## 回答评分点

高分答案应该覆盖：

- AES-GCM 是认证加密。
- 同时提供保密性和完整性。
- nonce 不能重复。
- 密钥要安全管理和轮换。
- 查询可配合 HMAC hash。
