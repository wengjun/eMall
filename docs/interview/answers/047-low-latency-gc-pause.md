# 047 为什么低延迟服务要关注 GC 暂停？

[返回按分类学习面试题](../README.md)

## 只看 JVM 新增的暂停来源

你已熟悉尾延迟和排队放大，这里只需要补齐 Stop-The-World、Safepoint 和 GC 并发阶段的区别。
并发收集器不等于完全没有暂停；堆里对象回收得快，也不代表线程到达安全点很快。

![GC 暂停与尾延迟放大](../assets/gc-pause-latency.svg)

## Java 17 的定位入口

```text
-Xlog:gc*,safepoint:file=gc.log:time,uptime,level,tags:filecount=5,filesize=20M
```

先把 GC pause 与同一时段的请求延迟对应，再检查暂停原因、回收前后占用和分配速率。
Safepoint 日志还能帮助区分“进入安全点慢”和“停下来后执行 VM 操作慢”，不能把所有暂停都归给垃圾回收。

G1 的 `-XX:MaxGCPauseMillis` 是软目标，不是请求 SLA。
盲目压低目标可能提高回收频率、消耗更多 CPU；扩大堆也可能只是延后问题。

## 代码侧值得检查的点

优先用 JFR 定位真实分配热点：大 JSON、装箱、字符串拼接、中间集合、长期持有的请求对象。
不要用“手写循环一定比 Stream 快”代替测量，示例与手法统一看
[066](066-reduce-object-allocation.md)。

验收优化时同时看分配速率、GC CPU、暂停分布与存活集；降低分配却增加长期保留对象，也可能适得其反。
