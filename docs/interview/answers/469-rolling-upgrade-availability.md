# 469 滚动升级如何保证可用性？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

滚动升级通过分批替换旧 Pod，配合多副本、readinessProbe、maxUnavailable、maxSurge、PDB、优雅
关闭和自动回滚来保证服务持续可用。

关键是新实例真正就绪后再接流量，旧实例处理完请求后再退出。

## 关键配置

配置：

- 副本数大于 1。
- readinessProbe。
- `maxUnavailable`。
- `maxSurge`。
- terminationGracePeriod。
- preStop hook。
- PDB。

这些共同控制发布过程。

## 风险点

风险：

- readiness 过早成功。
- 旧 Pod 被立即杀死。
- 连接未排空。
- 新版本启动慢。
- 数据库变更不兼容。
- 下游依赖未准备好。

滚动升级不自动解决兼容性问题。

## 验证方式

验证：

- 观察错误率。
- 观察 P99。
- 观察 readiness 状态。
- 小流量灰度。
- 自动回滚条件。
- 业务指标确认。

发布成功不等于 Pod 全部 Running。

## 电商系统实践

大型电商系统订单服务升级时，设置 `maxUnavailable: 0` 和合理 `maxSurge`，让新 Pod 通过 readiness 后再接
流量。

旧 Pod 收到终止信号后先从流量中摘除，等待正在执行的下单请求完成，再退出。
