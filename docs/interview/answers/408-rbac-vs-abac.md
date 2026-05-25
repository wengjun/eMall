# 408 RBAC 和 ABAC 有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

RBAC 和 ABAC 有什么区别？

## 先给面试官的短答案

RBAC 是基于角色的访问控制，通过用户、角色、权限映射做授权。ABAC 是基于属性的访问控制，根据
用户属性、资源属性、环境属性和动作属性动态决策。

RBAC 简单易管理，ABAC 更灵活但规则治理更复杂。

## RBAC

RBAC 模型：

- 用户。
- 角色。
- 权限。
- 用户绑定角色。
- 角色绑定权限。

例如客服角色可以查看订单，商家运营角色可以编辑商品。

## ABAC

ABAC 使用属性决策：

- 用户所属部门。
- 用户风险等级。
- 资源所属商家。
- 订单金额。
- 当前时间。
- 来源 IP。
- 操作类型。

它适合复杂条件授权。

## 取舍

RBAC 适合：

- 后台菜单权限。
- 常规岗位权限。
- 稳定组织角色。

ABAC 适合：

- 多租户资源隔离。
- 高危操作条件控制。
- 数据权限。
- 风控联动权限。

很多系统会组合使用。

## 在 eMall 项目中怎么讲？

eMall 商家后台可以用 RBAC 管理店铺管理员、运营、客服等角色。

但“只能编辑自己店铺的商品”“高风险账号不能导出订单”“夜间大额退款需要审批”更适合用 ABAC
规则表达。

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
RBAC 基于角色授权，适合组织岗位和菜单接口权限；ABAC 基于属性授权，结合用户、资源、环境和动作
属性做动态决策。

生产系统通常组合使用：RBAC 管基础权限，ABAC 管资源归属、租户隔离、风险等级和高危操作条件。
ABAC 更灵活，但需要规则治理、审计和测试。
```

## 回答评分点

高分答案应该覆盖：

- RBAC 基于角色。
- ABAC 基于属性。
- RBAC 简单易管理。
- ABAC 灵活但复杂。
- 电商后台通常组合使用。
