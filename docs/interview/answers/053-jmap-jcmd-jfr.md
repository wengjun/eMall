# 053 jmap、jcmd、JFR 分别适合什么场景？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`jmap` 更偏传统内存排查，常用于生成 heap dump 和查看堆对象概况。
`jcmd` 是更通用的 JVM 诊断入口，可以触发 GC、查看 JVM 参数、生成 dump、查看 native memory、
启动 JFR。JFR 是低开销持续事件记录工具，适合生产性能分析、延迟分析、锁分析和 GC 分析。

现在生产环境我会优先考虑 `jcmd` 和 JFR，`jmap` 作为兼容和补充工具。

## 三者定位

可以这样理解：

- `jmap`：主要看 Java 堆。
- `jcmd`：统一 JVM 诊断命令入口。
- JFR：持续记录 JVM 和应用运行事件。

如果只是想看某一刻堆里有什么，`jmap` 和 `jcmd GC.heap_dump` 都可以。
如果想知道一段时间内为什么慢，JFR 更合适。

## jmap 适合什么场景？

`jmap` 常见用途：

- 导出 heap dump。
- 查看堆对象直方图。
- 分析堆 OOM。
- 分析大对象和对象数量异常。
- 配合 MAT 查找内存泄漏。

示例：

```powershell
jmap -histo <pid>
jmap -dump:format=b,file=heap.hprof <pid>
```

使用 heap dump 时要注意文件很大，生产机器磁盘可能被打满。

## jmap 的风险

一些 `jmap` 操作可能造成 Stop-The-World，或者对目标 JVM 有明显影响。

风险包括：

- dump 文件很大。
- 导出过程消耗 IO。
- 可能拉长服务停顿。
- 在高峰期影响线上请求。

所以生产上不要随便在高峰期 dump heap。更安全的方式是：

- 在副本实例上操作。
- 先确认磁盘空间。
- 在流量低峰执行。
- 优先使用 JFR 或指标做初步判断。

## jcmd 适合什么场景？

`jcmd` 是 JDK 自带的通用诊断工具。

常见用途：

- 查看 JVM 启动参数。
- 查看系统属性。
- 导出 heap dump。
- 查看类加载统计。
- 查看线程信息。
- 查看 native memory。
- 启动和停止 JFR。
- 触发诊断命令。

示例：

```powershell
jcmd <pid> VM.flags
jcmd <pid> VM.command_line
jcmd <pid> GC.class_histogram
jcmd <pid> GC.heap_dump heap.hprof
jcmd <pid> VM.native_memory summary
```

`jcmd` 的覆盖面比 `jmap` 更广，很多新诊断能力都集中在 `jcmd`。

## Native Memory Tracking

如果怀疑堆外内存问题，`jcmd` 很重要。

示例：

```powershell
jcmd <pid> VM.native_memory summary
```

它能帮助分析：

- Java heap。
- class metadata。
- thread stack。
- code cache。
- GC native memory。
- internal native memory。
- direct buffer 相关占用。

前提是 JVM 启动时启用了 NMT，例如：

```text
-XX:NativeMemoryTracking=summary
```

## JFR 适合什么场景？

JFR 是 Java Flight Recorder。

它会记录一段时间内的 JVM 和应用事件，适合分析：

- CPU 热点。
- 方法耗时。
- GC 暂停。
- 对象分配。
- 锁竞争。
- 线程阻塞。
- 文件 IO。
- Socket IO。
- 异常频率。
- 类加载。

JFR 的优势是低开销、时间维度完整，适合生产环境长期或按需开启。

## JFR 示例

可以通过 `jcmd` 启动 JFR：

```powershell
jcmd <pid> JFR.start name=profile settings=profile duration=120s filename=profile.jfr
```

然后用 JDK Mission Control 分析。

JFR 比单次 `jstack` 或 heap dump 更适合回答“这两分钟到底发生了什么”。

## 如何选择？

可以按问题类型选择：

- 堆对象异常：`jmap`、`jcmd GC.class_histogram`、heap dump。
- native memory 异常：`jcmd VM.native_memory`。
- CPU 飙高：JFR、async-profiler、`jstack`。
- P99 升高：JFR、链路追踪、线程 dump、依赖监控。
- GC 抖动：JFR、GC log、JVM metrics。
- 锁竞争：JFR、`jstack`。

生产排查通常不是单一工具，而是多工具交叉验证。

## 在 eMall 项目中怎么讲？

如果 eMall 网关内存持续上涨但 heap used 不高，优先用 `jcmd VM.native_memory` 看 direct memory、
thread stack 和 metaspace。

如果订单服务疑似 Java heap 泄漏，导出 heap dump 后用 MAT 查引用链。

如果秒杀接口 P99 飙升但没有明显错误，使用 JFR 记录高峰 2 分钟，看锁竞争、对象分配、GC 和 socket IO。
