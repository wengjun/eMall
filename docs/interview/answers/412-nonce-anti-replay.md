# 412 nonce 如何防重放？

[返回按分类学习面试题](../README.md)

## 题目

nonce 如何防重放？

## 先给面试官的短答案

nonce 是一次性随机数或唯一请求号。服务端在签名验证通过后检查同一个 appKey 在时间窗口内是否
已经使用过该 nonce，如果使用过就拒绝，从而防止攻击者重复提交同一请求。

nonce 通常要和 timestamp 一起使用，避免无限期保存。

## 工作方式

流程：

- 客户端生成随机 nonce。
- nonce 参与签名。
- 服务端验证签名。
- 服务端检查 nonce 是否已存在。
- 不存在则写入缓存并设置 TTL。
- 已存在则拒绝重放请求。

检查和写入最好原子完成。

## 为什么要 timestamp

timestamp 作用：

- 限制请求有效时间窗口。
- 限制 nonce 保存时长。
- 降低存储成本。
- 拒绝过旧请求。

没有 timestamp，服务端需要永久记住所有 nonce。

## 存储设计

可以使用 Redis：

```text
replay:{appKey}:{nonce}
```

使用 `SET NX PX` 写入。写入成功表示首次请求，写入失败表示 nonce 已被使用。

## 在 eMall 项目中怎么讲？

eMall 开放平台支付回调或商家订单接口，应要求请求携带 nonce。服务端用 Redis 记录 `appKey` 和
nonce，TTL 与 timestamp 窗口一致。

如果同一 nonce 再次出现，即使签名正确，也拒绝执行，避免重复改价或重复回调。

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
nonce 防重放的思路是让每个请求都有唯一编号，并让该编号参与签名。服务端验证签名后，用 Redis
等存储检查同一 appKey 的 nonce 是否已使用，首次写入成功才处理。

nonce 必须和 timestamp 搭配，否则服务端需要永久保存所有 nonce。生产实现要用 SET NX PX 或 Lua
保证检查和写入原子。
```

## 回答评分点

高分答案应该覆盖：

- nonce 是一次性请求标识。
- nonce 要参与签名。
- 服务端保存已使用 nonce。
- 与 timestamp 配合控制窗口。
- 检查和写入要原子。
