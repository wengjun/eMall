# 458 Docker 镜像分层是什么？

[返回按分类学习面试题](../README.md)

## 题目

Docker 镜像分层是什么？

## 先给面试官的短答案

Docker 镜像由多层只读文件系统叠加组成。Dockerfile 中的每个构建步骤通常会产生一层，容器运行时
在镜像层之上加一个可写层。

分层带来缓存复用、快速分发和存储节省，但也要求我们控制层数和敏感文件残留。

## 分层机制

机制：

- 基础镜像是一层或多层。
- `COPY`、`RUN` 等指令产生新层。
- 相同层可被多个镜像复用。
- 容器运行时新增可写层。

镜像层不可变，有利于可重复部署。

## 构建缓存

Docker 会复用未变化的层。

如果依赖下载层没有变化，后续构建可以直接使用缓存。合理安排 Dockerfile 顺序能显著加速构建。

## 风险

风险：

- 在某层写入 secret 后再删除，历史层仍可能保留。
- 层过多增加复杂度。
- 不必要文件会增加镜像体积。
- 基础镜像漏洞会继承到上层。

敏感文件不能进入任何镜像层。

## 在 eMall 项目中怎么讲？

eMall Java 服务镜像可以先复制 `pom.xml` 下载依赖，再复制源码构建，这样依赖层可缓存。

最终运行镜像只包含 JRE、应用 jar 和必要配置，不包含 Maven 仓库、源码和构建密钥。

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
Docker 镜像是由多层只读文件系统叠加而成，容器启动时在只读镜像层上增加可写层。分层带来缓存、
复用和分发效率。

但分层也意味着敏感文件不能先复制再删除，因为它可能留在历史层中。生产镜像要控制体积、基础镜像
漏洞和构建层内容。
```

## 回答评分点

高分答案应该覆盖：

- 镜像由多层只读层组成。
- 容器有可写层。
- 分层支持缓存和复用。
- Dockerfile 顺序影响缓存。
- secret 不能进入历史层。
