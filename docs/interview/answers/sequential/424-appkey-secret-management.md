# 424 开放平台如何做 appKey 和 secret 管理？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

开放平台如何做 appKey 和 secret 管理？

## 先给面试官的短答案

appKey 用于标识应用，secret 用于签名认证，必须安全生成、加密存储、分权限展示、支持轮换、支持
禁用和审计。secret 只在创建或重置时展示一次，平台不应长期明文保存。

密钥管理要和签名、防重放、权限和限流一起设计。

## 生命周期

生命周期：

- 应用创建。
- 生成 appKey 和 secret。
- 分配接口权限。
- 密钥轮换。
- 临时禁用。
- 应用下线。
- 审计访问。

每个阶段都要可追踪。

## secret 存储

存储要求：

- secret 明文不落库。
- 使用 KMS 或加密存储。
- 展示一次后不可再次查看。
- 支持重置。
- 保存 key version。
- 访问 secret 操作审计。

泄露后要能快速轮换。

## 权限绑定

appKey 应绑定：

- 商家或租户。
- 可访问 API 范围。
- 回调地址白名单。
- IP 白名单。
- 限流配额。
- 风险等级。

不是有 appKey 就能访问所有接口。

## 在 eMall 项目中怎么讲？

eMall 开放平台给商家应用发放 appKey 和 secret。商家调用订单接口时用 secret 做 HMAC 签名。

平台按 appKey 查权限、限流、IP 白名单和商家范围。secret 泄露时可禁用旧版本并生成新版本。

## 深度增强：密钥治理图

![开放平台 API 安全链路](../../assets/openapi-security.svg)

appKey 和 secret 不只是两列数据库字段，而是一套生命周期。应用创建、密钥展示、加密存储、
权限绑定、调用验签、泄露处置、轮换、禁用和审计都要闭环。否则开放平台越大，泄露影响越难控制。

## 深度增强：Java 17 密钥生命周期示例

```java
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

enum SecretStatus {
    ACTIVE,
    ROTATING,
    DISABLED
}

record ClientSecret(
        String appKey,
        int version,
        String encryptedSecret,
        SecretStatus status,
        Instant createdAt) {
}

final class SecretFactory {
    private final SecureRandom random = new SecureRandom();

    PlainSecretIssue issue(String appKey, int version, SecretEncryptor encryptor) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String plainSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ClientSecret stored = new ClientSecret(
                appKey,
                version,
                encryptor.encrypt(plainSecret),
                SecretStatus.ACTIVE,
                Instant.now());
        return new PlainSecretIssue(stored, plainSecret);
    }
}

record PlainSecretIssue(ClientSecret storedSecret, String plainSecretShownOnce) {
}

interface SecretEncryptor {
    String encrypt(String plainText);
}
```

这段代码故意把 `storedSecret` 和 `plainSecretShownOnce` 分开，表达生产原则：平台可以展示一次明文，
但落库的是加密密文或 KMS 包装后的密文。后续页面不应再次展示旧 secret，只能重置或轮换。

## 深度增强：生产边界

密钥轮换要支持“双版本并存”。新 secret 生效后，旧 secret 可以在短窗口内继续验签，帮助商家平滑升级。
窗口结束后再禁用旧版本。否则一键重置会导致外部商家系统同时故障，形成平台级事故。

appKey 也要绑定租户、接口 scope、IP 白名单、回调地址、限流配额和风险等级。secret 泄露后，
应能按 appKey 快速禁用、强制轮换、追踪调用日志，并通知商家和平台安全团队。

## 深度增强：面试高分表达

我会把 appKey 视为应用身份，把 secret 视为高敏签名材料。设计时不只回答如何生成，
还会覆盖只展示一次、加密存储、版本轮换、禁用、权限绑定、审计和泄露处置。这样说明我考虑的是
开放平台长期运营，而不是一次接口调用。

## 专家级完整回答

```text
appKey 是应用标识，secret 是签名密钥。开放平台要安全生成、加密存储、只展示一次、支持轮换、
禁用和审计。secret 不能明文长期保存。

appKey 还要绑定租户、接口权限、IP 白名单、回调地址和限流配额。请求时通过 HMAC、timestamp 和
nonce 校验身份、防篡改和防重放。
```

## 回答评分点

高分答案应该覆盖：

- appKey 标识应用。
- secret 用于签名。
- secret 不长期明文保存。
- 支持轮换和禁用。
- appKey 绑定权限、限流和租户。
## 深度完善：专项验收清单

围绕「开放平台如何做 appKey 和 secret 管理？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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
