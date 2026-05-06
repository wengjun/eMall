# 411 HMAC 签名如何防篡改？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

HMAC 签名如何防篡改？

## 先给面试官的短答案

HMAC 使用共享密钥和消息内容计算摘要。服务端收到请求后，用同样密钥和同样规范化内容重新计算
签名，并与客户端签名比较。如果请求内容被篡改，重新计算出的签名就不一致。

安全关键是密钥保密、签名内容完整、规范化一致和防重放。

## 工作流程

流程：

- 客户端准备 method、path、timestamp、nonce 和 body hash。
- 按约定顺序拼接 canonical string。
- 使用 secret 计算 HMAC。
- 请求携带 appKey、签名和时间戳。
- 服务端查 secret 并重新计算。
- 签名一致才放行。

签名验证必须在业务处理前完成。

## 能防什么

可以防：

- 请求体被篡改。
- path 被篡改。
- method 被篡改。
- 关键参数被篡改。
- 未持有 secret 的伪造请求。

不能单独防重放，需要 nonce 和 timestamp 配合。

## 注意点

注意：

- secret 不能传输。
- canonical string 必须稳定。
- body 要做 hash。
- 比较签名用常量时间比较。
- 密钥要支持轮换。
- 错误日志不能打印 secret。

签名方案最怕实现细节不一致。

## 在 eMall 项目中怎么讲？

eMall 开放平台给商家系统调用订单接口时，可以要求每个请求带 `appKey`、`timestamp`、`nonce` 和
HMAC 签名。

服务端根据 `appKey` 找到 secret，重新计算签名，避免请求金额、订单号或路径被中间人篡改。

## 深度增强：开放平台验签图

![开放平台 API 安全链路](../../assets/openapi-security.svg)

HMAC 验签通常放在 API Gateway 或开放平台接入层。它要在业务逻辑前完成，失败请求不能进入订单、
支付和售后服务。验签不是单点动作，还要和 appKey 状态、接口权限、nonce 防重放和限流一起判断。

## 深度增强：Java 17 HMAC 验签示例

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

record SignedRequest(
        String method,
        String path,
        String timestamp,
        String nonce,
        String bodyHash,
        String signature) {
}

final class HmacVerifier {
    private static final Duration ALLOWED_SKEW = Duration.ofMinutes(5);
    private final Clock clock;

    HmacVerifier(Clock clock) {
        this.clock = clock;
    }

    boolean verify(SignedRequest request, String secret) {
        Instant requestTime = Instant.parse(request.timestamp());
        Duration skew = Duration.between(requestTime, Instant.now(clock)).abs();
        if (skew.compareTo(ALLOWED_SKEW) > 0) {
            return false;
        }
        String canonical = String.join("\n",
                request.method().toUpperCase(),
                request.path(),
                request.timestamp(),
                request.nonce(),
                request.bodyHash());
        String expected = hmacSha256(secret, canonical);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                request.signature().getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String secret, String canonical) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate HMAC signature", ex);
        }
    }
}
```

这里的关键不是 HMAC 算法本身，而是 canonical string 必须稳定。method、path、timestamp、nonce、
body hash 的拼接规则要写进 SDK 和文档，否则客户端和服务端各自理解会导致大量误拒。

## 深度增强：生产边界

HMAC 可以证明“持有 secret 的调用方发送了未被篡改的内容”，但不能证明调用方有业务权限。
所以验签之后还要校验 appKey 是否启用、是否有接口 scope、是否属于对应商家、是否命中限流和风控。

防重放不能只靠 timestamp。攻击者可以在时间窗口内重复发送同一个合法请求，所以 nonce 需要落 Redis
或专用去重存储，并设置过期时间。签名失败日志只记录 appKey、错误类型和 traceId，不能打印 secret。

## 深度增强：面试高分表达

我会把 HMAC 讲成开放平台安全链路的一环，而不是一个孤立算法。它解决防篡改和调用方证明，
防重放靠 timestamp 加 nonce，权限靠 scope 和资源校验，稳定性靠限流和审计。这样回答能体现
我知道算法、协议细节和生产治理之间的边界。

## 专家级完整回答

```text
HMAC 防篡改的原理是只有客户端和服务端知道 secret。客户端用 secret 对规范化请求内容计算签名，
服务端用同样方式重新计算。如果 method、path、参数或 body 被篡改，签名就不匹配。

生产中还要配合 timestamp、nonce、防重放、常量时间比较、密钥轮换和日志脱敏。HMAC 只能证明
请求未被篡改且来自持有 secret 的一方，不自动解决权限问题。
```

## 回答评分点

高分答案应该覆盖：

- HMAC 使用共享密钥和消息内容。
- 服务端重新计算签名。
- 篡改内容会导致签名不一致。
- 防重放需要 timestamp 和 nonce。
- secret 管理和常量时间比较。
## 深度完善：专项验收清单

围绕「HMAC 签名如何防篡改？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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
