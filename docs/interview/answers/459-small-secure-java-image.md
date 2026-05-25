# 459 如何构建小而安全的 Java 镜像？

[返回按分类学习面试题](../README.md)

## 题目

如何构建小而安全的 Java 镜像？

## 先给面试官的短答案

构建小而安全的 Java 镜像要使用多阶段构建、精简运行时基础镜像、只复制运行所需 jar、非 root
用户运行、固定版本、扫描漏洞、减少工具和 shell、正确设置 JVM 参数。

目标是减少镜像体积、攻击面和不可控依赖。

## 多阶段构建

构建阶段：

- 使用 Maven 或 Gradle 构建。
- 下载依赖。
- 执行测试。
- 产出 jar。

运行阶段：

- 使用 JRE 或精简运行时。
- 只复制 jar。
- 不包含源码和构建工具。

这样能明显减小镜像。

## 安全实践

实践：

- 使用非 root 用户。
- 固定基础镜像版本。
- 定期镜像漏洞扫描。
- 不写入 secret。
- 删除不必要包。
- 最小化 Linux 工具。
- 设置只读文件系统。

生产镜像不应像开发环境一样全。

## Java 注意点

注意：

- 使用 Java 17 运行时。
- 配置容器感知 JVM。
- 设置合理 `-Xmx`。
- 暴露健康检查端口。
- 输出日志到 stdout。
- 支持优雅关闭。

镜像安全和运行稳定要一起考虑。

## 在 eMall 项目中怎么讲？

eMall 每个 Spring Boot 服务可以用 Maven 构建阶段产出 jar，运行阶段使用 Java 17 精简基础镜像。

镜像中只包含应用 jar，使用 `app` 用户运行，并在 CI 中执行镜像漏洞扫描。

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
小而安全的 Java 镜像通常使用多阶段构建。构建阶段包含 Maven 和源码，运行阶段只包含 Java 17
运行时和应用 jar。

安全上要非 root 运行、固定基础镜像版本、漏洞扫描、不写入 secret、减少系统工具、输出日志到
stdout，并配置容器环境下的 JVM 内存和优雅关闭。
```

## 回答评分点

高分答案应该覆盖：

- 多阶段构建。
- 运行镜像只包含必要文件。
- 非 root 用户运行。
- 固定版本和漏洞扫描。
- JVM 容器参数和优雅关闭。
