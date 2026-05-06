# 414 签名为什么要覆盖 method、path、body hash？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

签名为什么要覆盖 method、path、body hash？

## 先给面试官的短答案

签名覆盖 method、path 和 body hash，是为了防止请求方法、目标资源和请求体被篡改。如果只签部分
参数，攻击者可能把同一个签名挪到其他接口、其他方法或修改 body。

签名必须覆盖所有影响业务语义的内容。

## method

method 影响语义：

- `GET` 通常查询。
- `POST` 通常创建。
- `PUT` 通常更新。
- `DELETE` 通常删除。

如果 method 不签名，攻击者可能改变操作类型。

## path

path 决定访问资源。

如果 path 不签名，同一个签名可能被挪到另一个接口。例如原本签的是查询订单，却被挪到退款接口。

## body hash

body hash 防止请求体被改。

直接签 body 可能受大 body、编码和换行影响。常见做法是先计算 body hash，再把 hash 放入 canonical
string 参与签名。

## 在 eMall 项目中怎么讲？

eMall 开放平台商家调用退款接口时，签名必须覆盖 `POST`、`/refunds`、timestamp、nonce 和 body
hash。

如果攻击者修改退款金额、订单号或把请求挪到批量退款接口，签名验证会失败。

## 深度增强：安全治理图

![开放平台 API 安全链路](../../assets/openapi-security.svg)

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
签名要覆盖 method、path、query、body hash、timestamp 和 nonce，因为这些字段共同决定请求语义。
只签部分字段会留下重放到其他接口、改方法或改请求体的空间。

body 通常先做 hash 再参与签名，既能防篡改，又避免直接拼接大 body 的规范化问题。原则是所有影响
业务决策的内容都要进入签名。
```

## 回答评分点

高分答案应该覆盖：

- method 改变操作语义。
- path 决定资源和接口。
- body hash 防请求体篡改。
- 防签名被挪用到其他接口。
- 签名覆盖业务关键内容。

## 二次深度补强

题目：签名为什么要覆盖 method、path、body hash？

二次补强标记：已完成

### 面试官真正想确认的能力

安全题要覆盖身份、权限、签名、重放、审计、风控和密钥轮换。
围绕这道题，要进一步把概念、项目实现、线上风险和验证闭环连起来。

### 深度和广度补充

- 先区分认证和授权，明确调用者是谁、能做什么、访问什么资源。
- 再说明签名、时间戳、nonce、密钥版本和防重放窗口。
- 随后补齐风控策略：频率、设备、地理位置、金额、历史行为。
- 最后说明审计、告警、封禁、申诉和合规留痕。

### 图片讲解

![二次补强图解](../../assets/openapi-security.svg)

- 图中展示请求从开放网关进入认证、验签、风控和业务服务的路径。
- 读图时要说明安全检查越靠前越能保护内部系统。
- 高分回答要把安全控制和用户体验、误伤率、可审计性结合。

### Java17 HMAC 验签示例

```java
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class HmacSigner {

    String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
```

### 高分表达要点

- 不要只回答定义，要说明为什么这样设计、在什么条件下失效、如何监控和回滚。
- 把答案和当前电商项目联系起来，例如订单、库存、支付、履约、搜索、风控或发布链路。
- 主动给出边界条件和反例，能让面试官看到你具备生产系统判断力。

## 逐题专项补强

逐题专项补强标记：已完成

### 本题专项切入

- 本题要围绕「签名为什么要覆盖 method、path、body hash？」展开，不要只复述分类模板。
- 先区分认证、授权、验签、风控、审计和合规留痕。
- 再说明重放攻击、越权、密钥泄漏、误杀和降级处理。

### 专项图解说明

![逐题专项图解](../../assets/openapi-security.svg)

- 这张图用于把「签名为什么要覆盖 method、path、body hash？」放回生产链路中理解，重点看入口、状态、数据和恢复闭环。
- 面试时可以先按图说明主路径，再补失败路径、监控指标和回滚手段。

### 贴合本题的实现示例

```java
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class HmacVerifier {

    boolean verify(String payload, String secret, String expected) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String actual = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        return actual.equalsIgnoreCase(expected);
    }
}
```

### 进一步追问时的回答边界

- 如果面试官继续追问，要主动说明这个实现是核心模型，不等于完整生产组件。
- 生产级落地还需要接入鉴权、幂等、限流、熔断、监控、告警、灰度和数据修复。
- 回答时把复杂度、失败场景、验证方式和 eMall 项目中的落地位置一起说清楚。

## 面试实战补强

面试实战补强标记：已完成

### 面试追问路线

- 认证、授权、签名、风控、审计分别解决什么问题？
- 如何防重放、防越权、防密钥泄漏，并控制风控误杀率？
- 安全策略变更如何灰度，误伤用户时如何恢复和追溯？

### eMall 项目落点

- 可以落到模块：identity、risk、openapi、gateway。
- 回答「签名为什么要覆盖 method、path、body hash？」时，要从这些模块里选一个主链路做例子。
- 讲清入口、状态变化、数据写入、异步事件、失败补偿和观测指标。

### 生产验证指标

- 验签失败率
- 越权拦截数
- 风控误杀率
- 审计覆盖率

### 低分陷阱

- 只背定义，不说明业务场景和失败场景。
- 只讲正常路径，不讲超时、重试、回滚、补偿和监控。
- 只给方案，不给验证指标和取舍边界。

### 30 秒高分收束

这道题我会用 安全、风控、开放平台 的视角回答。
先给结论，再给项目例子，然后补失败场景、验证指标和取舍边界。
这样能让面试官看到我不是只会背知识点，而是能把知识点落到生产系统。

## 架构取舍与反驳补强

架构取舍补强标记：已完成

### 先给立场

- 回答「签名为什么要覆盖 method、path、body hash？」时，不能只给单一方案，要先说明约束、目标和失败边界。
- 高分回答要让面试官看到你能在正确性、可用性、成本、复杂度和团队能力之间做判断。

### 可选方案对比

- 网关前置校验：能尽早拦截风险，但规则过重会增加入口延迟。
- 服务内精细鉴权：表达能力强，但容易分散导致策略不一致。
- 异步风控：主链路快，但对高风险交易需要同步拦截或二次验证。

### 反驳和防守

- 如果面试官问为什么不直接上最复杂方案，可以回答：复杂方案只有在规模和风险证明必要时才值得引入。
- 如果面试官问为什么不用最简单方案，可以回答：简单方案可以做第一期，但必须提前设计观测和迁移边界。
- 我的判断原则是：如果涉及安全边界，优先选择默认拒绝、最小权限、可审计和可轮换的方案。

### 决策证据

- 拦截率和误杀率
- 审计日志覆盖率
- 密钥轮换记录
- 越权和重放攻击拦截数

### 一句话总结

我会先用简单可靠的方案解决当前确定性问题，同时保留观测、灰度和迁移能力。
当指标证明瓶颈存在，再演进到更复杂的架构，而不是为了显得高级提前复杂化。

## 生产落地验收补强

生产验收补强标记：已完成

### 上线前检查

- 针对「签名为什么要覆盖 method、path、body hash？」，先确认它影响的是正确性、稳定性、性能、安全还是成本。
- 确认默认拒绝、最小权限、密钥轮换、审计日志和风控误杀恢复。
- 上线前要做越权、重放、签名篡改和权限回归测试。

### 灰度和回滚

- 先在测试环境和影子流量中验证，再做 1%、5%、25%、50%、100% 分阶段灰度。
- 每个阶段都设置自动暂停条件和人工回滚负责人。
- 回滚不是只回代码，还要确认配置、数据、缓存、消息和任务状态能一起回到安全状态。

### 监控和验收证据

- 安全回归测试
- 审计日志样例
- 误杀恢复记录
- 密钥轮换记录

### 面试表达

我不会只说方案能实现，还会说明上线前怎么验收、上线中怎么看指标、出问题怎么回滚。
这能证明我关注的是长期稳定运行，而不是只完成一次功能开发。

## 规模化与成本治理补强

规模成本补强标记：已完成

### 规模化视角

- 回答「签名为什么要覆盖 method、path、body hash？」时，要主动放到 10 亿用户、1 亿 DAU、100W 峰值并发的背景下思考。
- 按入口请求量、验签 CPU 成本、风控规则复杂度和审计写入量估算容量。
- 安全链路要避免把重计算全部压在网关同步路径。

### 成本治理

- 用单位成本看问题，例如单请求成本、单订单成本、单消息成本和单 GB 存储成本。
- 先优化浪费最高的环节，而不是平均用力。

### 自动化和 owner

- 为关键指标建立看板、告警、owner 和 Runbook。
- 把经验沉淀成自动化检查、流水线门禁或平台能力。

### 面试表达

我会补一句：方案能跑只是第一步，大规模下还要回答容量怎么估、成本怎么控、故障谁负责。
这能体现我不是只会实现单点功能，而是能长期运营一个高并发业务系统。

