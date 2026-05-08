# 404 认证和授权有什么区别？

[返回按分类学习面试题](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

认证和授权有什么区别？

## 先给面试官的短答案

认证解决“你是谁”，授权解决“你能做什么”。认证确认主体身份，例如用户名密码、短信验证码、
OAuth2 登录。授权根据角色、权限、资源和策略判断该主体是否能访问某个接口或数据。

两者都需要，但不能混为一谈。

## 认证

认证方式：

- 密码登录。
- 短信验证码。
- 多因素认证。
- Session。
- JWT。
- OAuth2。
- mTLS。

认证结果通常是一个可信身份上下文。

## 授权

授权判断：

- 是否能访问接口。
- 是否能操作资源。
- 是否能查看某类数据。
- 是否能执行高危操作。
- 是否满足组织、店铺、地区等约束。

授权必须结合资源和动作。

## 常见误区

误区：

- 登录成功就允许所有操作。
- 只在前端控制按钮。
- 只校验角色不校验资源归属。
- 内部接口不鉴权。
- 运维接口共用普通权限。

认证成功不代表授权成功。

## 在 eMall 项目中怎么讲？

eMall 用户登录是认证。用户查看自己的订单，需要授权校验订单 `userId` 是否属于当前用户。

商家后台修改商品，不仅要登录，还要校验该商品是否属于当前商家，以及该账号是否有商品编辑权限。

## 深度增强：开放平台安全图

![开放平台 API 安全链路](../assets/openapi-security.svg)

这张图可以用来解释认证和授权的边界。网关先完成认证，确认调用方或用户身份，并生成身份上下文。
授权中心再结合接口、资源、租户、商家和动作判断是否允许访问。业务服务层还要做资源级校验，
因为网关通常不知道订单、商品、店铺这些业务资源的真实归属。

## 深度增强：Java 17 资源级授权示例

```java
import java.util.EnumSet;
import java.util.Set;

enum Action {
    READ_ORDER,
    EDIT_PRODUCT,
    REFUND_ORDER
}

record PrincipalContext(
        long userId,
        long merchantId,
        Set<String> roles,
        Set<Action> actions) {
}

record OrderResource(long orderId, long ownerUserId, long merchantId) {
}

final class AuthorizationService {

    boolean canReadOrder(PrincipalContext principal, OrderResource order) {
        if (principal.roles().contains("ADMIN")) {
            return principal.actions().contains(Action.READ_ORDER);
        }
        boolean ownsOrder = principal.userId() == order.ownerUserId();
        boolean belongsToMerchant = principal.merchantId() == order.merchantId();
        return principal.actions().contains(Action.READ_ORDER) && (ownsOrder || belongsToMerchant);
    }

    boolean canOperateMerchantProduct(PrincipalContext principal, long productMerchantId) {
        boolean hasPermission = principal.actions().contains(Action.EDIT_PRODUCT);
        return hasPermission && principal.merchantId() == productMerchantId;
    }

    static PrincipalContext merchantOperator(long userId, long merchantId) {
        return new PrincipalContext(
                userId,
                merchantId,
                Set.of("MERCHANT_OPERATOR"),
                EnumSet.of(Action.READ_ORDER, Action.EDIT_PRODUCT));
    }
}
```

这段代码体现三个面试重点：认证后的用户 ID 只是身份，不能直接放行；授权要同时看动作和资源；
管理员、用户、商家和运营后台最好使用不同策略，避免把所有权限都压成一个简单角色。

## 深度增强：生产边界

认证失败通常返回 `401`，表示身份不可确认；授权失败通常返回 `403`，表示身份已确认但权限不足。
内部服务之间也不能默认信任，要通过 mTLS、服务身份、JWT 受众校验或网关注入的可信上下文保护。

高并发电商系统里，权限数据需要缓存，但缓存必须有版本号或短 TTL。权限变更、账号冻结和商家封禁
属于安全事件，应能快速失效缓存。否则用户被封禁后仍能继续调用接口。

## 深度增强：面试高分表达

我不会把认证和授权混在一起设计。认证负责建立可信主体，授权负责在具体资源和动作上做决策。
对于订单、支付、售后、商家商品这些核心资源，我会在服务层做二次资源级授权，因为只靠网关鉴权
无法判断业务归属。这样既能保护外部 API，也能保护内部误调用和横向越权。

## 专家级完整回答

```text
认证是确认主体身份，授权是判断该主体是否有权限执行某个动作或访问某个资源。认证回答你是谁，
授权回答你能做什么。

生产系统要在网关和服务层都保留身份上下文，并在服务层做资源级授权。前端按钮隐藏不是安全控制，
内部接口和运维接口也必须鉴权。
```

## 回答评分点

高分答案应该覆盖：

- 认证是身份确认。
- 授权是权限判断。
- 登录成功不代表有权限。
- 要做资源级授权。
- 内部和运维接口也要保护。
## 深度完善：专项验收清单

围绕「认证和授权有什么区别？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。
