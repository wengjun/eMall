# 407 token 泄露如何处理？

[返回按分类学习面试题](../README.md)

## 题目

token 泄露如何处理？

## 先给面试官的短答案

token 泄露后要立即缩小影响范围，包括撤销 token、提高 token 版本、加入黑名单、强制重新登录、
轮换密钥、排查泄露来源、审计异常访问并通知受影响用户。

处理顺序是先止血，再溯源和修复。

## 止血动作

动作：

- 将 token 加入黑名单。
- 提高用户 token version。
- 撤销 refresh token。
- 强制用户重新登录。
- 冻结高风险账号。
- 临时提高风控等级。

如果签名密钥泄露，要执行密钥轮换。

## 溯源排查

排查：

- 是否写入日志。
- 是否前端 XSS。
- 是否浏览器插件。
- 是否 HTTPS 配置问题。
- 是否第三方 SDK 泄露。
- 是否内部人员误操作。

找到来源后才能防止再次发生。

## 审计和恢复

需要：

- 查询泄露期间的访问日志。
- 标记异常 IP 和设备。
- 检查高危操作。
- 通知用户修改密码。
- 补偿受影响交易。
- 更新安全规则。

安全事件要有完整闭环。

## 在 eMall 项目中怎么讲？

eMall 如果发现用户 token 泄露，应立即撤销 refresh token，提高该用户 token version，让旧 access
token 校验失败。

如果泄露来自日志，要删除日志中的敏感字段，补充日志脱敏规则，并审计泄露期间的订单、地址和支付
相关操作。

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
token 泄露要先止血，包括黑名单、token version、撤销 refresh token、强制重新登录和必要时冻结
账号。如果签名密钥泄露，还要轮换密钥。

之后要溯源泄露渠道，审计异常访问和高危操作，并修复日志、前端 XSS、传输和第三方 SDK 等问题。
处理要形成安全事件闭环。
```

## 回答评分点

高分答案应该覆盖：

- 先撤销或失效 token。
- refresh token 也要处理。
- 密钥泄露要轮换。
- 排查泄露来源。
- 审计异常访问和高危操作。
