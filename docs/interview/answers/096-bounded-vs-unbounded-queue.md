# 096 Java 线程池的队列类型该怎么选？

[返回按分类学习面试题](../README.md)

## 只记构造器与行为差异

| 队列 | 容量行为 | 常见误用 |
| --- | --- | --- |
| ArrayBlockingQueue(n) | 固定容量数组 | 以为队列满之前会先扩到 maximumPoolSize |
| LinkedBlockingQueue(n) | 指定容量的链表队列 | 忽略节点与任务引用占用 |
| LinkedBlockingQueue() | 默认极大容量 | 当成有实际内存保护的队列 |
| SynchronousQueue | 不存储任务，直接移交 | 把它误解成容量为 1 的队列 |

`Executors.newFixedThreadPool` 使用没有显式小容量边界的队列；
`newCachedThreadPool` 主要通过创建线程应对无可用工作者。
它们限制的资源不同，不能仅凭“fixed”推断内存安全。

排队增长的通用原理你已熟悉。本题只需要结合
[094](094-threadpool-core-parameters.md) 的接收顺序，
理解为什么队列选型会改变线程增长和拒绝时机。

取消了 Future 的任务可能仍留在队列里等待清理；必要时了解 ThreadPoolExecutor.remove/purge，
不要误认为 cancel 已经立即释放所有排队对象引用。
