# 417 HMAC hash 为什么适合精确查询？

[返回按分类学习面试题](../README.md)

## 题目

HMAC hash 为什么适合精确查询？

## 先给面试官的短答案

加密后的敏感字段通常不能直接用于精确查询，尤其是使用随机 nonce 的安全加密。HMAC hash 使用
密钥对明文计算稳定摘要，同一明文得到同一 hash，因此可以建索引用于精确匹配。

它适合手机号、邮箱、证件号等字段的等值查询。

## 为什么不用明文

明文存储风险：

- 数据库泄露直接暴露隐私。
- 日志和备份风险高。
- 不符合合规要求。
- 内部滥用难控制。

敏感字段应尽量不明文保存。

## 为什么不用密文查询

安全加密通常使用随机 nonce。

同一个手机号每次加密结果不同，这提升安全性，但无法直接等值查询。因此需要额外保存稳定的
HMAC hash。

## HMAC 的价值

HMAC hash：

- 同一输入输出稳定。
- 需要密钥才能计算。
- 可建唯一索引。
- 比普通 hash 更抗字典攻击。
- 不需要解密即可查询。

密钥仍必须保护。

## 在 eMall 项目中怎么讲？

eMall 保存手机号时，可以保存 `phone_ciphertext` 和 `phone_hash`。

登录或客服按手机号查用户时，先用 HMAC 计算输入手机号的 hash，再按 `phone_hash` 索引查询，查到
后按权限解密展示脱敏手机号。

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
AES-GCM 这类安全加密使用随机 nonce，同一明文多次加密密文不同，不适合直接等值查询。HMAC hash
对同一明文输出稳定值，可以建索引用于精确查询。

相比普通 hash，HMAC 需要密钥，能降低手机号这类低熵数据被彩虹表反推的风险。生产中通常密文用于
展示解密，HMAC hash 用于查询。
```

## 回答评分点

高分答案应该覆盖：

- 安全加密密文不适合等值查询。
- HMAC 对同一明文输出稳定。
- 可以建索引。
- 比普通 hash 更安全。
- 密文和 HMAC hash 配合使用。
