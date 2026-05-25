# 481 如何设计生产 Secret 管理？

[返回按分类学习面试题](../README.md)

## 题目

如何设计生产 Secret 管理？

## 先给面试官的短答案

生产 Secret 管理要做到集中托管、最小权限、加密存储、动态下发、定期轮换、访问审计和泄露快速吊销。
Secret 不应明文写在代码、镜像、配置仓库或日志中，应由专门的密钥系统或云 KMS 管理。

## Secret 包括什么

Secret 不只是数据库密码，还包括：

- 数据库、Redis、Kafka 和第三方系统凭证。
- JWT 签名密钥、HMAC secret、API app secret。
- 支付通道证书、私钥和回调验签密钥。
- 对称加密密钥、数据脱敏盐值和 webhook token。
- 内部运维接口凭证和自动化部署 token。

这些信息泄露后可能导致数据泄露、资金风险或供应链攻击。

## 设计原则

第一，集中管理。Secret 应保存在 KMS、Vault、云 Secret Manager 或受控配置系统中，不进入 Git 仓库。

第二，最小权限。服务只能读取自己需要的 Secret，不能用一个超级账号访问所有依赖。

第三，轮换能力。密钥要支持新旧双活、灰度切换和快速吊销，避免轮换导致全站故障。

第四，审计可追溯。谁读取、谁修改、什么时候生效、影响哪些服务，都要有记录。

第五，运行时安全。Secret 不写日志，不暴露在错误响应中，不放入不必要的环境变量，不打进镜像层。

## Kubernetes 中的注意点

Kubernetes Secret 默认只是 base64 编码，不等于强加密。生产环境要启用 etcd encryption，限制 RBAC 权限，
必要时使用 External Secrets、CSI driver 或 Vault Agent，把 Secret 从外部密钥系统安全注入 Pod。

Secret 更新后还要考虑应用是否能热加载。如果不能热加载，需要滚动重启并保证新旧密钥兼容。

## 在 eMall 项目中怎么讲？

eMall 的支付、开放平台、身份、风控和数据服务都涉及高敏感 Secret。支付证书和 HMAC secret 要支持轮换，
开放平台 app secret 要支持泄露吊销，数据库凭证要按服务拆分权限，所有 Secret 访问要进入审计日志。

## 深度增强：现场编码工程化图

![现场编码题的工程化解法](../assets/coding-patterns.svg)

现场编码题不只是写出算法，还要说明输入输出、边界条件、复杂度、线程安全和可测试性。
面试官通常更看重思考过程、代码结构和验证意识，而不是只看最终代码。

## 深度增强：Java 17 编码模板示例

```java
import java.util.LinkedHashMap;
import java.util.Map;

final class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    LruCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

这段代码展示现场编码的表达方式：先选合适数据结构，再说明复杂度和边界。
若用于生产，还要考虑并发、监控、容量和淘汰策略。

## 深度增强：生产边界

面试中的简化实现通常不是生产实现。生产需要线程安全、容量限制、指标、异常处理、单元测试和压测验证。
如果题目涉及分布式场景，还要说明单机实现和多实例实现的差异。

## 深度增强：面试高分表达

我会先澄清需求和边界，再写最小正确实现，最后补充复杂度、测试用例和生产化改造。
这样即使代码题不复杂，也能体现工程成熟度。

## 专家级完整回答

```text
生产 Secret 管理的核心目标是防泄露、可轮换、可审计和最小权限。

我不会把 Secret 写进代码、镜像或普通配置文件，而是使用 KMS、Vault 或云 Secret Manager 统一管理。
服务通过受控身份读取必要 Secret，权限按服务和环境隔离。

对电商系统来说，支付私钥、开放平台 app secret、数据库密码和 JWT 签名密钥都必须支持轮换。
轮换时要允许新旧密钥并存，先发布读取新密钥的能力，再切换签发或加密密钥，最后吊销旧密钥。
```

## 回答评分点

高分答案应该覆盖：

- 说明 Secret 不能进入代码、镜像、仓库和日志。
- 覆盖集中托管、加密、最小权限、轮换和审计。
- 知道 Kubernetes Secret 默认 base64 不是强加密。
- 能讲清新旧密钥并存和快速吊销。
- 能结合支付私钥、app secret、数据库凭证举例。
