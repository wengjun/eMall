# 468 PodDisruptionBudget 解决什么问题？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

PodDisruptionBudget 用于限制自愿中断期间可同时不可用的 Pod 数量，例如节点维护、驱逐和集群升级。
它能防止维护操作一次性驱逐太多副本，导致服务容量不足。

PDB 保护的是可用性，不处理应用自身崩溃这种非自愿中断。

## 自愿中断

自愿中断包括：

- 节点维护 drain。
- 集群升级。
- 人工驱逐 Pod。
- 节点缩容。

这些场景 Kubernetes 可以遵守 PDB。

## 配置方式

常见配置：

- `minAvailable`。
- `maxUnavailable`。

例如订单服务 5 个副本，可以要求至少 4 个可用，维护时一次最多影响 1 个。

## 注意点

注意：

- PDB 不能替代多副本部署。
- PDB 不处理节点突然宕机。
- PDB 可能阻塞节点维护。
- 需要结合 readinessProbe。
- 单副本服务设置 PDB 意义有限。

PDB 要和容量规划一起看。

## 在 eMall 项目中怎么讲？

eMall 订单服务和支付服务应配置 PDB，避免集群升级时同时驱逐多个 Pod。

如果服务只有两个副本且 PDB 要求 `minAvailable: 2`，节点维护可能无法进行，因此副本数和 PDB 要
一起设计。
