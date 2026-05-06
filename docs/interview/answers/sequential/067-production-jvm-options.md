# 067 如何设置生产环境 JVM 参数？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

## 题目

如何设置生产环境 JVM 参数？

## 先给面试官的短答案

生产 JVM 参数要围绕目标设置：内存上限、GC 策略、GC 日志、OOM 诊断、容器适配、编码时区和可观测性。
Java 17 微服务通常可以使用 G1 默认配置作为起点，再根据延迟、吞吐、内存和容器限制调优。

不要直接复制网上参数。参数必须和服务类型、流量模型、容器资源和 SLO 匹配。

## 参数设计目标

设置 JVM 参数前先确定目标：

- 服务是低延迟还是高吞吐？
- 容器内存限制是多少？
- CPU 核数是多少？
- 请求对象分配速率多高？
- P99 目标是多少？
- 是否需要快速 OOM 诊断？
- 是否运行在 Kubernetes？

没有目标的 JVM 调优通常是无效调优。

## 内存参数

常见参数：

```text
-Xms
-Xmx
-XX:MaxRAMPercentage
-XX:InitialRAMPercentage
```

传统方式是设置 `-Xms` 和 `-Xmx`。

容器环境中也可以用百分比参数，让 JVM 根据容器内存限制计算堆大小。

注意：容器 memory limit 不只包含 heap，还包含 metaspace、direct memory、thread stack、code cache 和 native memory。

## GC 参数

Java 17 默认 GC 通常是 G1，适合大多数服务作为起点。

常见参数：

```text
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

`MaxGCPauseMillis` 是目标，不是保证。设置过小可能导致 GC 更频繁，吞吐下降。

如果是极低延迟服务，可以评估 ZGC，但要基于压测验证。

## GC 日志

生产必须开启 GC 日志。

Java 17 使用统一日志：

```text
-Xlog:gc*,safepoint:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=100m
```

GC 日志用于分析：

- GC 频率。
- pause 时间。
- heap 变化。
- old gen 压力。
- allocation rate。
- safepoint。

没有 GC 日志，线上内存问题会非常难复盘。

## OOM 诊断参数

常用参数：

```text
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/app/heapdump.hprof
-XX:+ExitOnOutOfMemoryError
```

注意 heap dump 很大，要确认磁盘空间。

`ExitOnOutOfMemoryError` 适合 Kubernetes 场景，让异常实例退出并由平台拉起新实例。

## 容器适配

容器中要关注：

- JVM 是否识别 cgroup 限制。
- 堆大小是否给 native memory 留余量。
- CPU limit 是否导致 GC 或 JIT 受限。
- Pod memory limit 是否过小。
- readiness 和 liveness 是否合理。

Java 17 已经具备较好的容器感知能力，但参数仍要和资源配置匹配。

## 时区和编码

建议显式设置：

```text
-Dfile.encoding=UTF-8
-Duser.timezone=UTC
```

服务内部统一 UTC 可以减少跨时区数据问题。

前端展示再转换用户时区。

## Native Memory Tracking

排查堆外内存可以开启：

```text
-XX:NativeMemoryTracking=summary
```

它有一定开销，不一定所有服务都长期启用。

核心网关、Netty、上传下载服务可以按需启用或在问题实例启用。

## 示例参数

一个通用起点：

```text
-XX:+UseG1GC
-XX:MaxRAMPercentage=65
-XX:InitialRAMPercentage=65
-Xlog:gc*,safepoint:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=100m
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/app
-XX:+ExitOnOutOfMemoryError
-Dfile.encoding=UTF-8
-Duser.timezone=UTC
```

这只是起点，不能替代压测和线上观测。

## 在 eMall 项目中怎么讲？

eMall 交易服务更关注 P99 和稳定性，建议使用 G1、开启 GC 日志、设置 OOM dump，并为容器内存保留堆外余量。

网关和搜索服务可能有更多 direct memory 和网络缓冲，要特别关注堆外内存。

报表、离线任务、数据仓库模块可能更关注吞吐和批处理内存。

不同模块不应该完全复制同一套参数。

## 深度增强：JVM 容器内存预算图

![Java 17 容器内 JVM 内存结构](../../assets/jvm-runtime-memory.svg)

生产 JVM 参数首先是资源预算问题。容器内存不是只给 heap，用 `MaxRAMPercentage` 或 `-Xmx`
设置堆时，要给 direct memory、metaspace、线程栈、code cache 和 JVM native 留空间。

## 深度增强：Java 17 参数生成示例

```java
import java.util.List;

record JvmProfile(int maxRamPercentage, String gc, boolean enableNmt) {
}

final class JvmOptionPlanner {

    List<String> options(JvmProfile profile) {
        List<String> base = new java.util.ArrayList<>();
        base.add("-XX:+Use" + profile.gc() + "GC");
        base.add("-XX:MaxRAMPercentage=" + profile.maxRamPercentage());
        base.add("-XX:InitialRAMPercentage=" + profile.maxRamPercentage());
        base.add("-Xlog:gc*,safepoint:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=100m");
        base.add("-XX:+HeapDumpOnOutOfMemoryError");
        base.add("-XX:HeapDumpPath=/var/log/app");
        base.add("-XX:+ExitOnOutOfMemoryError");
        base.add("-Dfile.encoding=UTF-8");
        base.add("-Duser.timezone=UTC");
        if (profile.enableNmt()) {
            base.add("-XX:NativeMemoryTracking=summary");
        }
        return base;
    }
}
```

这不是让生产动态拼 JVM 参数，而是表达参数应按服务画像生成。网关可以降低 heap 占比并关注 direct memory；
订单服务关注 heap、GC pause 和 OOM dump；离线任务可能更关注吞吐和批处理内存。

## 深度增强：生产边界

JVM 参数不是越多越专业。很多参数会改变 GC 行为、JIT 行为或诊断开销。生产应以少量明确参数为基线，
通过压测和线上指标验证，再逐步调整。参数变更本身也要灰度发布。

OOM dump 和 GC 日志需要磁盘预算。核心服务开启 dump 后，如果磁盘挂载不合理，可能在 OOM 时把节点磁盘打满。
所以诊断参数要和日志轮转、磁盘配额、采集链路一起设计。

## 深度增强：面试高分表达

我不会背一套固定 JVM 参数。我会先看服务 SLO、容器内存、CPU、对象分配、direct memory 和故障诊断要求。
Java 17 微服务通常以 G1 和 GC 日志为起点，设置合理堆占比和 OOM dump，再通过压测验证 P99、吞吐、
CPU 和内存水位。

## 专家级完整回答

```text
生产 JVM 参数不是固定模板，而是围绕 SLO、容器资源和服务类型设计。Java 17 微服务通常以 G1
为起点，设置合理堆比例，开启 GC 和 safepoint 日志，配置 OOM heap dump 和退出策略，并显式设置
编码、时区和容器内存余量。对 Netty 或网关类服务，还要关注 direct memory 和 native memory。

调优必须通过压测和线上指标验证，不能照抄网上参数。
```

## 回答评分点

高分答案应该覆盖：

- 参数要匹配服务目标。
- 内存不只有 heap。
- Java 17 可用 G1 作为起点。
- 生产要开 GC 日志和 OOM dump。
- 容器环境要留 native memory 余量。
- 不照抄参数。
