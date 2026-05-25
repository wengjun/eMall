# 405 Session、JWT、OAuth2 如何取舍？

[返回按分类学习面试题](../README.md)

## 题目

Session、JWT、OAuth2 如何取舍？

## 先给面试官的短答案

Session 适合服务端可控的登录态，JWT 适合无状态令牌和跨服务传递身份声明，OAuth2 适合第三方
授权和开放平台授权。三者不是同一层次的替代品。

取舍要看是否需要服务端撤销、跨域、第三方授权、移动端和微服务传播。

## Session

特点：

- 状态保存在服务端。
- 容易撤销。
- 可集中管理登录态。
- 需要共享存储或粘性会话。

适合 Web 登录和安全要求较高场景。

## JWT

特点：

- 令牌自包含。
- 服务端可无状态校验签名。
- 适合微服务身份传递。
- 撤销和权限变更不如 Session 直接。

JWT 要控制有效期和敏感信息。

## OAuth2

特点：

- 授权框架。
- 适合第三方应用访问用户资源。
- 支持授权码、客户端凭证等模式。
- 常和 JWT 或 opaque token 搭配。

OAuth2 重点是授权委托，不只是登录。

## 在 eMall 项目中怎么讲？

eMall C 端登录可以使用 Session 或短期 access token 加 refresh token。内部微服务可以传递签名
JWT 或网关注入的身份上下文。

开放平台给第三方商家应用访问订单和商品接口时，更适合 OAuth2 授权码或客户端凭证模式。

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
Session、JWT、OAuth2 不是简单三选一。Session 是服务端登录态机制，JWT 是自包含令牌格式，
OAuth2 是授权框架。

如果需要强撤销和集中控制，Session 更直接；如果需要微服务无状态身份传递，JWT 常用；如果涉及
第三方应用授权访问用户或商家资源，应使用 OAuth2。
```

## 回答评分点

高分答案应该覆盖：

- 三者不是同一层替代。
- Session 服务端可控。
- JWT 自包含但撤销难。
- OAuth2 是授权框架。
- 能结合 C 端、微服务和开放平台说明。
