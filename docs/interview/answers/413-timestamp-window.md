# 413 timestamp 窗口如何设置？

[返回按分类学习面试题](../README.md)

## 题目

timestamp 窗口如何设置？

## 先给面试官的短答案

timestamp 窗口要在安全性和可用性之间权衡。窗口太短会因为客户端时钟偏差和网络延迟误拒请求，
窗口太长会扩大重放攻击有效期。

开放平台常见窗口是 5 分钟左右，但具体要结合业务风险、网络环境和时钟同步能力。

## 影响因素

因素：

- 客户端时钟准确性。
- 网络延迟。
- 跨地域调用。
- 请求重试策略。
- 接口风险等级。
- nonce 存储成本。

高危接口窗口应更短。

## 校验逻辑

服务端校验：

```text
abs(serverTime - requestTimestamp) <= allowedWindow
```

如果超过窗口，直接拒绝。timestamp 要参与签名，避免被篡改。

## 配合 nonce

timestamp 只能限制请求时间，不能单独阻止窗口内重放。

因此需要：

- timestamp 限制有效期。
- nonce 防止窗口内重复使用。
- HMAC 防止 timestamp 和 nonce 被篡改。

三者组合才完整。

## 在 eMall 项目中怎么讲？

eMall 开放平台普通查询接口可以设置 5 分钟窗口。批量退款、批量改价等高危接口可以设置更短窗口，
并要求二次审批。

如果商家系统时间偏差过大，接口返回明确错误码，引导对方同步 NTP。

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
timestamp 窗口设置要平衡误拒和安全。窗口太短会受时钟偏差和网络延迟影响，太长会扩大重放攻击
窗口。常见开放平台可以用 5 分钟，高危接口更短。

timestamp 必须参与签名，并和 nonce 一起使用。timestamp 限制有效期，nonce 防止窗口内重放，
HMAC 防止二者被篡改。
```

## 回答评分点

高分答案应该覆盖：

- 窗口太短影响可用性。
- 窗口太长增加重放风险。
- timestamp 要参与签名。
- 需要 nonce 防窗口内重放。
- 不同风险接口窗口不同。
