# 460 为什么生产容器不建议 root 用户运行？

[返回按分类学习面试题](../README.md)

## 题目

为什么生产容器不建议 root 用户运行？

## 先给面试官的短答案

生产容器不建议 root 运行，是因为一旦应用被攻破，攻击者会获得容器内 root 权限，配合容器逃逸、
挂载目录、内核漏洞或错误配置可能扩大到宿主机或集群。

非 root 运行是降低攻击面和权限影响范围的基本措施。

## root 风险

风险：

- 可修改更多文件。
- 可访问更多系统能力。
- 容器逃逸影响更大。
- 挂载目录可能被破坏。
- 错误权限配置更危险。

容器不是强安全边界。

## 非 root 好处

好处：

- 限制攻击者权限。
- 降低误操作风险。
- 配合只读文件系统。
- 满足安全基线。
- 更符合最小权限原则。

服务只需要运行应用，不需要 root。

## 落地方式

做法：

- Dockerfile 创建普通用户。
- 使用 `USER app`。
- 应用目录授权给普通用户。
- 避免绑定低端口。
- Kubernetes 配置 `runAsNonRoot`。
- 禁用特权容器。

镜像和 Pod 安全上下文要同时配置。

## 在 eMall 项目中怎么讲？

eMall Java 服务只需要读取 jar、写临时目录和输出日志，不需要 root 权限。

镜像中创建 `app` 用户，Kubernetes 设置 `runAsNonRoot: true`，并限制容器能力，降低被攻击后的影响。

## 深度增强：Kubernetes 运维治理图

![Kubernetes 生产运行和故障治理](../assets/kubernetes-operations.svg)

Kubernetes 题不能只背 Deployment、Service 和 Ingress。生产稳定性还取决于资源 requests/limits、探针、HPA、PDB、
灰度发布、配置回滚、日志指标 Trace 和故障 Runbook。

## 深度增强：Java 17 发布门禁示例

```java
record ReleaseSignal(double errorRate, long p99Millis, double cpuThrottleRate, boolean rollbackSafe) {

    boolean canContinue() {
        return errorRate < 0.001
                && p99Millis < 300
                && cpuThrottleRate < 0.05
                && rollbackSafe;
    }
}
```

这段代码表达发布平台的核心：放量不是人工拍脑袋，而是由错误率、延迟、资源和回滚安全共同决定。

## 深度增强：生产边界

K8s 会重启失败容器，但不保证业务一定恢复。错误的 liveness probe 可能造成重启风暴；
过低的 CPU limit 会造成 throttling；不兼容数据库变更会让回滚失效。平台能力要和应用设计配合。

## 深度增强：面试高分表达

我会把 K8s 视为运行平台，而不是稳定性的全部答案。真正生产级要有容量规划、灰度门禁、配置治理、可观测性、
自动回滚和数据库兼容检查，才能支撑核心交易链路。

## 专家级完整回答

```text
容器运行 root 会扩大攻击面。应用漏洞被利用后，攻击者在容器内拥有 root 权限，配合挂载目录、
特权配置或内核漏洞可能造成更大影响。

生产容器应按最小权限运行，使用非 root 用户、只读文件系统、禁用特权模式、限制 Linux capabilities，
并在 Kubernetes 中设置 securityContext。
```

## 回答评分点

高分答案应该覆盖：

- 容器不是绝对安全边界。
- root 被攻破影响更大。
- 非 root 是最小权限。
- Dockerfile 和 Kubernetes 都要配置。
- 禁用特权和限制 capabilities。
