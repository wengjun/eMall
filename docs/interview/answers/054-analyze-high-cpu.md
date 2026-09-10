# 054 如何分析 CPU 飙高？

[返回按分类学习面试题](../README.md)

## 核心结论

分析 CPU 飙高要先判断是用户态 CPU、系统态 CPU、GC、锁自旋、热点计算还是外部压力导致。
Java 服务常用流程是先定位进程，再定位高 CPU 线程，把线程 ID 转成十六进制后在 `jstack` 中找调用栈，
同时结合 JFR、GC 日志、接口流量、慢日志和链路追踪验证。

不要一上来就重启。重启会丢失现场。

## 第一步：确认是不是 Java 进程

先看主机或容器指标：

- CPU 使用率。
- load average。
- 容器 CPU throttling。
- 请求 QPS。
- 错误率。
- P99 延迟。

如果多个进程共享机器，要先确认高 CPU 来自哪个进程。

在容器环境中，还要关注 CPU limit。如果 limit 太低，服务可能被频繁 throttling，表现为延迟升高。

## 第二步：定位高 CPU 线程

Linux 上常见方法是用 `top -H -p <pid>` 看进程内线程。

找到 CPU 高的线程后，把线程 ID 转成十六进制，再去 `jstack` 中查 `nid`。

示例逻辑：

```text
decimal thread id -> hex thread id -> jstack nid
```

Windows 环境可以使用 Process Explorer、JFR 或 IDE profiler 辅助定位。

## 第三步：看线程栈在做什么

常见高 CPU 调用栈：

- 死循环。
- 大量 JSON 序列化或反序列化。
- 复杂正则。
- 加密和签名。
- 压缩和解压。
- 大集合排序和过滤。
- 规则引擎计算。
- 日志格式化。
- GC 线程占用。
- 锁自旋或 CAS 重试。

如果连续几次 `jstack` 中高 CPU 线程都在同一段业务代码，基本可以锁定热点。

## 第四步：区分业务 CPU 和 GC CPU

如果 CPU 高同时伴随频繁 GC，问题可能不是业务逻辑，而是内存压力。

判断方式：

- 看 GC 日志是否频繁。
- 看 young GC 或 mixed GC 是否变多。
- 看 allocation rate 是否异常。
- 看 old gen 是否接近上限。
- 看 JFR 中 GC 事件和对象分配。

如果 CPU 被 GC 消耗，大概率要优化对象分配、缓存大小、批量加载或 JVM 参数。

## 第七步：使用 JFR 或 profiler

`jstack` 适合快速定位线程栈，但对持续热点分析不够完整。

JFR 可以记录：

- 方法采样。
- CPU 使用。
- 对象分配。
- 锁等待。
- GC。
- IO。

如果是生产问题，JFR 比侵入式 profiler 更安全。
