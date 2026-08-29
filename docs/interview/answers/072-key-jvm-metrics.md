# 072 需要重点监控哪些 JVM 指标？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

重点监控 heap、old gen、metaspace、direct memory、GC 次数和暂停、allocation rate、线程数、
线程状态、类加载数量、CPU、进程内存、容器内存和线程池指标。对微服务来说，还要把 JVM 指标和接口 P99、
错误率、QPS、连接池、下游延迟一起看。

最重要的是 old gen 趋势、GC pause、线程堆积和容器内存余量。

## 内存指标

需要关注：

- heap used。
- heap committed。
- old gen used。
- young gen used。
- metaspace used。
- non-heap used。
- direct buffer memory。
- mapped buffer memory。

old gen 持续上涨且 GC 后不回落，通常比 heap 瞬时高更危险。

## GC 指标

需要关注：

- young GC count。
- young GC duration。
- old GC count。
- old GC duration。
- Full GC count。
- GC pause max。
- GC pause P99。
- GC CPU 占比。
- allocation rate。
- promotion rate。

GC 告警要关注持续恶化和与 P99 的相关性。

## 线程指标

需要关注：

- live thread count。
- daemon thread count。
- peak thread count。
- runnable 线程数。
- blocked 线程数。
- waiting 线程数。
- deadlock 检测。

线程数持续上涨可能表示线程泄漏、线程池不受控或下游阻塞。

## 类加载指标

需要关注：

- loaded class count。
- total loaded class count。
- unloaded class count。

类加载数量异常上涨可能来自动态代理、脚本、插件、表达式编译或类加载器泄漏。

普通业务服务类加载数量通常启动后趋于稳定。

## 进程和容器指标

需要关注：

- process CPU。
- system CPU。
- process RSS。
- container memory working set。
- container memory limit。
- CPU throttling。
- file descriptor count。

容器 OOMKilled 很多时候不是 heap OOM，而是进程总内存超过限制。

## 线程池和连接池指标

严格说它们不是 JVM 原生指标，但对 Java 服务非常关键。

线程池：

- active count。
- pool size。
- queue size。
- completed task count。
- rejected count。
- task execution time。

连接池：

- active connections。
- idle connections。
- pending acquire。
- acquire latency。
- timeout count。

线程池和连接池经常是 P99 升高的直接原因。

## 指标优先级

如果只能先做一批告警，建议优先：

- old gen 使用率和回收趋势。
- GC pause P99。
- Full GC 次数。
- live thread count。
- blocked thread count。
- container memory 使用率。
- 业务线程池 queue size。
- HTTP/DB 连接池 pending。

这些指标最容易提前发现线上风险。

## 在 eMall 项目中怎么讲？

订单服务重点看 old gen、GC pause、订单线程池、数据库连接池和下游 HTTP 连接池。

网关重点看 direct memory、线程数、event loop 延迟、连接数和容器内存。

营销服务重点看规则计算 CPU、对象分配速率和缓存大小。

不同服务的 JVM 监控重点应该按服务特性调整。
