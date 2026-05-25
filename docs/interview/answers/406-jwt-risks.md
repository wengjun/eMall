# 406 JWT 有什么风险？

[返回按分类学习面试题](../README.md)

## 题目

JWT 有什么风险？

## 先给面试官的短答案

JWT 的主要风险包括令牌泄露后可被直接使用、撤销困难、有效期过长、敏感信息暴露、签名算法配置
错误、密钥管理不当和权限变更不能及时生效。

JWT 不是加密数据，默认只是签名后的可读声明。

## 常见风险

风险：

- token 被 XSS 或日志泄露。
- 长有效期导致泄露影响大。
- 无法实时撤销。
- payload 放入敏感信息。
- 接受 `none` 算法或弱算法。
- 签名密钥泄露。
- 权限变更后旧 token 仍可用。

这些都可能变成安全事故。

## 防护措施

措施：

- 使用 HTTPS。
- access token 短有效期。
- refresh token 可撤销。
- 不在 JWT 中放敏感数据。
- 固定允许算法。
- 密钥定期轮换。
- 高危操作二次校验。
- token 黑名单或版本号。

不要把 JWT 当万能登录态。

## 存储风险

浏览器存储要权衡：

- localStorage 容易被 XSS 读取。
- Cookie 要设置 HttpOnly、Secure、SameSite。
- 移动端要使用安全存储。

令牌存储是安全设计的一部分。

## 在 eMall 项目中怎么讲？

eMall JWT 只放用户 ID、租户、角色摘要、过期时间和 token 版本，不放手机号、身份证号和余额。

用户修改密码或冻结账号时，提高 token version 或加入黑名单，让旧 token 失效。

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
JWT 的风险在于它自包含且通常无状态校验，泄露后在有效期内可被使用，撤销也比 Session 困难。
payload 默认可读，不能放敏感信息。

生产中要短有效期、refresh token 可撤销、固定签名算法、密钥轮换、HTTPS、防 XSS、日志脱敏，并
通过 token version 或黑名单处理账号冻结和密码修改。
```

## 回答评分点

高分答案应该覆盖：

- JWT 泄露后风险大。
- JWT 默认不是加密。
- 撤销困难。
- 不放敏感信息。
- 短有效期、密钥轮换和黑名单。
