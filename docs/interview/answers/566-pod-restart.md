# 566 一个 Pod 频繁重启，如何排查？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

先通过 Pod 状态、退出码和事件区分 OOMKilled、探针失败、应用崩溃、驱逐和节点故障，并在重启覆盖现场前保存日志与 dump。
止血时可以摘流或回滚，但不能只不断重启掩盖根因。

## 核心拆解

- 查看 previous logs、termination reason、exit code、restart count、Pod events 和节点状态。
- OOM 时核对容器限制、JVM MaxRAMPercentage、堆外内存和线程数；探针失败则区分应用不健康与探针配置错误。
- 将异常 Pod 摘流并保留一个现场样本，发布相关时回滚，节点相关时迁移工作负载。
- 修复后验证 readiness/liveness/startup probe、资源 request/limit 和优雅停机行为。
