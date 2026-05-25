# 420 日志脱敏如何实现？

[返回按分类学习面试题](../README.md)

## 题目

日志脱敏如何实现？

## 先给面试官的短答案

日志脱敏要在日志写入前对敏感字段进行识别、掩码、哈希或删除。常见敏感字段包括手机号、身份证、
银行卡、token、secret、地址和支付信息。

脱敏要在统一日志组件、网关、审计和异常处理链路中落地，不能靠开发者手工记忆。

## 脱敏方式

方式：

- 掩码显示。
- 删除字段。
- HMAC hash。
- 分类分级记录。
- 禁止记录原始 body。
- 异常堆栈过滤敏感参数。

不同字段选择不同方式。

## 常见规则

示例：

```text
13812345678 -> 138****5678
idCard -> 110***********1234
Authorization -> [REDACTED]
secret -> [REDACTED]
```

token 和 secret 通常应完全移除。

## 落地点

落地点：

- 统一日志框架。
- Web 过滤器。
- RPC 拦截器。
- 异常处理器。
- 审计日志组件。
- 日志采集管道。

越靠近源头越好。

## 在 eMall 项目中怎么讲？

eMall 订单日志可以记录 `orderId`、`userId` 和 trace ID，但不能记录完整手机号、地址、身份证和
支付凭证。

开放平台请求日志要移除 `Authorization`、`secret` 和签名原文，只保留必要摘要用于排查。

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
日志脱敏要在日志写入前统一处理敏感字段，包括手机号、地址、证件号、银行卡、token 和 secret。
常见做法是掩码、删除、HMAC hash 或分类记录。

生产中应在统一日志组件、过滤器、拦截器、异常处理和日志采集管道中落实，而不是依赖每个开发者
手工脱敏。还要通过扫描和审计发现漏脱敏。
```

## 回答评分点

高分答案应该覆盖：

- 敏感字段写日志前处理。
- 掩码、删除和 hash。
- token 和 secret 不应记录。
- 在统一组件中落地。
- 需要扫描和审计。
