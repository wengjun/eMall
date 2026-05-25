# 484 什么时候不应该引入 Service Mesh？

[返回按分类学习面试题](../README.md)

## 题目

什么时候不应该引入 Service Mesh？

## 先给面试官的短答案

当服务规模不大、语言栈单一、现有 SDK 和网关已经能满足治理需求、团队没有 Mesh 运维能力，或者系统主要瓶颈在业务建模
和数据一致性时，不应该急着引入 Service Mesh。Mesh 会增加代理资源、控制面复杂度、排障路径和发布风险。

## 不适合引入的场景

第一，服务数量少。十几个 Java 服务可以通过网关、Spring、Resilience4j、Micrometer 和 Kubernetes 完成基础治理，
不一定需要额外引入 sidecar 和控制平面。

第二，团队经验不足。Mesh 涉及证书、策略、代理、控制面、流量规则和可观测性。如果团队不能排查代理层问题，
生产事故会更难处理。

第三，延迟和资源极度敏感。sidecar 会带来额外 hop、CPU、内存和连接管理成本。高频低延迟链路要先评估收益和代价。

第四，治理目标不清晰。如果只是因为“看起来先进”而引入，最后可能只多了一层复杂性。

## 更务实的替代路径

可以先用网关统一外部流量治理，用 Resilience4j 做服务内熔断、限流和重试，用 Micrometer 做指标，
用 OpenTelemetry 做 trace，用 Kubernetes 做基础服务发现和发布。

当服务数量、语言栈、团队数量和安全合规要求继续提升，再评估 Service Mesh。

## 在 eMall 项目中怎么讲？

eMall 当前以 Java17 为主，优先把交易链路、测试、观测、限流、熔断、配置和发布治理做扎实。
如果未来发展为多语言、多集群、多团队的大规模系统，再引入 Mesh 统一 mTLS、流量治理和访问策略。

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
我不会把 Service Mesh 当成微服务的默认选项。

如果服务规模不大、语言栈统一、网关和 SDK 已经能满足治理需求，引入 Mesh 的收益可能小于成本。
它会增加 sidecar 资源消耗、控制面复杂度、证书管理和排障路径。

我会先问三个问题：现有治理是否失控，团队是否有运维 Mesh 的能力，收益是否能覆盖复杂度。
只有当服务规模、多语言、安全合规和统一流量治理需求足够强时，Mesh 才值得引入。
```

## 回答评分点

高分答案应该覆盖：

- 说明 Mesh 不是默认必选项。
- 能指出服务少、语言单一、团队能力不足时不适合引入。
- 覆盖资源、延迟、控制面和排障成本。
- 能给出网关、SDK、Kubernetes、可观测性的替代路径。
- 能体现技术选型要看收益和代价。
