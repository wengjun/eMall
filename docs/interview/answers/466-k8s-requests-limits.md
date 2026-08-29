# 466 requests 和 limits 如何设置？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

requests 表示调度时保留的资源，limits 表示容器可使用的资源上限。设置要基于压测和生产观测，
保证 Pod 能稳定运行，同时避免资源浪费和节点过载。

Java 服务尤其要让 JVM 内存配置和容器 memory limit 匹配。

## requests

requests 用于：

- Kubernetes 调度。
- 资源预留。
- HPA 计算 CPU 利用率基准。
- 确保节点容量规划。

requests 太低会导致节点过度打包。

## limits

limits 用于：

- 限制最大资源使用。
- 防止单个容器拖垮节点。
- memory 超限会 OOMKill。
- CPU limit 会触发 throttling。

limits 太紧会导致性能抖动。

## 设置方法

方法：

- 先压测估算基线。
- 观察生产 P95/P99 资源使用。
- request 设置为稳定运行需要的资源。
- limit 设置为可接受峰值。
- 定期根据真实负载调整。

不要拍脑袋设置。

## 在 eMall 项目中怎么讲？

eMall 订单服务如果稳定需要 1 核和 1.5GB 内存，可以把 request 设在接近这个水平，limit 给合理峰值。

同时 JVM `-Xmx` 要小于容器 memory limit，给 metaspace、线程栈和 direct memory 留空间。
