# 682 InnoDB Buffer Pool 如何工作？

[返回按分类学习面试题](../README.md)

## 它不只是“数据库缓存”

Buffer Pool 以页为单位缓存聚簇索引、二级索引和其他 InnoDB 数据结构。查询命中页后可直接在内存中读取；
更新先修改内存页并产生 redo，数据页可以稍后异步刷盘，这正是 Write-Ahead Logging 能提高吞吐的原因。

一页在运行中可能同时属于几类结构：

- free list：尚未承载数据，可直接分配。
- LRU list：参与冷热淘汰的缓存页。
- flush list：按最早未刷新的 LSN 组织脏页，供检查点推进。
- hash/page table：根据表空间号和页号定位内存 frame。

## 为什么不是简单 LRU

全表扫描若按普通 LRU 插入头部，会快速逐出真正热点。InnoDB 使用 midpoint insertion，把新读入页先放到 old 区头部；页在满足访问时间条件后再次访问才晋升到 young 区。一次性扫描页因此更容易从尾部淘汰。

```text
LRU 头部                                             LRU 尾部
[长期热点 young ................][新读入 old ........ 冷页]
                                  ^ midpoint
```

`innodb_old_blocks_pct` 控制 old 区比例，`innodb_old_blocks_time` 控制晋升前观察窗口。调整前必须用真实扫描负载验证，不能只背默认值。

## 读、写与刷盘路径

1. 读取先按页标识查 Buffer Pool；未命中才发起磁盘 IO，可能触发线性或随机预读。
2. 更新在持有相应 latch/记录锁时修改内存页，页成为 dirty page。
3. 后台 page cleaner 根据脏页比例、redo 生成速度和 IO 能力刷页。
4. checkpoint 记录“此前 redo 对应的数据页已可恢复到什么位置”，从而允许复用旧 redo 空间。

若脏页刷盘长期追不上 redo 产生速度，redo 空间逼近耗尽时，前台事务会被迫帮助刷页或等待，延迟会突然出现平台期和尖峰。

## 容量配置不能机械套 80%

专用物理数据库常把大部分内存给 Buffer Pool，但容器还要给连接线程、排序和 join buffer、
Performance Schema、redo/undo 元数据、操作系统页缓存以及备份代理留空间。并发连接数乘以每连接潜在内存，
比单一全局参数更容易造成 OOM。

多实例 Buffer Pool 可降低内部争用；大页预热、NUMA 和容器 memory limit 也要通过压测确认。Buffer Pool 很大不代表索引合理，低选择性扫描仍会浪费内存带宽。

## 生产诊断

重点联看而不是孤立看命中率：

- `Innodb_buffer_pool_reads` 与逻辑读，判断真实磁盘缺页。
- dirty page 比例、page flush、checkpoint age 和 redo 写入速率。
- LRU eviction、read ahead 有效性和 pending read/write。
- 数据集增长、热点集合大小、存储设备 IO 延迟。

99.9% 命中率在每秒一百万逻辑读时仍有每秒一千次物理读。大型电商系统大促前应按订单与库存热点工作集验证容量，并隔离报表全扫，避免分析查询污染交易 Buffer Pool。

参考：[MySQL 8.4 Buffer Pool](https://dev.mysql.com/doc/refman/8.4/en/innodb-buffer-pool.html)
