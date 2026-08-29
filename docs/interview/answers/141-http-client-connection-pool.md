# 141 如何设置 HTTP 客户端连接池？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

HTTP 客户端连接池要按下游维度设置最大连接数、每路由连接数、连接获取超时、连接空闲回收、连接存活时间和指标监控。
连接池容量不能只看调用方线程数，还要看下游承载能力、实例数、P99 和超时策略。

连接池过小会排队，过大会打垮下游。

## 为什么需要连接池？

HTTP 每次新建连接会有成本：

- TCP 握手。
- TLS 握手。
- 认证。
- 内核资源。

连接池复用连接，可以降低延迟和 CPU 消耗。

但连接池本身也需要容量治理。

## 核心参数

常见参数：

- max connections。
- max connections per route。
- connection acquire timeout。
- connect timeout。
- read timeout。
- keep alive。
- idle eviction。
- max connection lifetime。

不同客户端名字不同，但思路一致。

## 每个下游单独配置

不要所有下游共享一套连接池参数。

支付、库存、推荐、物流的 SLA 和容量不同。

核心下游应有独立连接池、独立指标和独立熔断。

## 容量估算

连接数要结合：

- 调用 QPS。
- 平均耗时和 P99。
- 下游实例数。
- 下游限流阈值。
- 调用方实例数。
- 是否有重试。

例如 20 个调用方实例，每个实例给支付开 500 连接，总连接可能是 10000，下游未必承受得住。

## 监控指标

必须监控：

- active connections。
- idle connections。
- pending acquire。
- acquire latency。
- timeout count。
- error count。
- per-host connection usage。

连接池 pending 升高通常会直接推高接口 P99。

## 在 eMall 项目中怎么讲？

订单服务调用库存和支付时，应该分别设置连接池。

库存高峰 QPS 大，连接池和线程池要受库存服务容量约束。

支付下游通常更敏感，连接池、超时和重试要更保守，避免重复支付和故障放大。
