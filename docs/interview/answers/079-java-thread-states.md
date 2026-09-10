# 079 Java 线程状态有哪些？

[返回按分类学习面试题](../README.md)

## JVM 状态不是操作系统状态的逐项映射

| Thread.State | 典型来源 | 排障注意 |
| --- | --- | --- |
| NEW | 已 new、未 start | start 只能成功调用一次 |
| RUNNABLE | 执行字节码、部分 native IO | 不表示一定正在消耗 CPU |
| BLOCKED | 等待进入 synchronized monitor | 查看锁对象和拥有者 |
| WAITING | Object.wait、join、LockSupport.park | ReentrantLock 等待常见在这里 |
| TIMED_WAITING | sleep、带超时的 wait/park/join | 正常空闲线程也可能在这里 |
| TERMINATED | run 已结束 | 不能再次 start |

`getState()` 只返回一个瞬时快照，不是线程间同步协议。不要轮询某个状态代替 latch 或 Future。
在 Java 17 中这里讨论的是平台线程，不使用后续版本的虚拟线程 API。

```java
Thread worker = new Thread(() -> System.out.println("running"));
System.out.println(worker.getState()); // NEW
worker.start();
worker.join();
System.out.println(worker.getState()); // TERMINATED
```

大量 WAITING 未必异常；看线程名、调用栈和等待对象才能区分正常线程池空闲与任务相互等待。
