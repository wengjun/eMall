# 051 线程数过多时，Java 17 应检查什么？

[返回按分类学习面试题](../README.md)

## 与原生服务端共同的成本不再展开

Java 17 平台线程仍需要 OS 线程和 native stack。新增的排查重点是：
**堆空闲不代表还能创建线程**，`-Xmx` 不限制全部进程内存。

`-Xss` 影响单线程栈大小，但“线程数 × Xss”只是粗略的栈保留空间估算，不等于实际 RSS。
降低 Xss 可能引入 StackOverflowError，不能作为无界创建线程的修复方案。

## 看 JVM 证据

```text
jcmd <pid> Thread.print -l
jcmd <pid> VM.native_memory summary
```

第二条需要启动时开启 `-XX:NativeMemoryTracking=summary`。
重点看 NMT 的 Thread 分类及 reserved/committed 差异，再对照 OS 的线程/进程数限制。

`OutOfMemoryError: unable to create native thread` 可能来自 native 内存、系统线程数或容器 PID 限制，
不能一律通过增大 Java 堆处理。

## 从栈回到 Java 对象

按线程名和重复调用栈找到创建者：每次请求新建 Executor、未关闭的客户端、定时任务重复注册都是常见来源。
`newCachedThreadPool` 可能持续扩线程；`newFixedThreadPool` 则可能让任务持续堆在无界队列中。
后者见 [094](094-threadpool-core-parameters.md) 和 [074](074-threadpool-queue-memory.md)。

ThreadLocal 会随池线程存活；任务结束要清理请求上下文，不能等线程自然销毁。
