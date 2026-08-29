# 701 Head Sampling 和 Tail Sampling 如何取舍？

[返回按分类学习面试题](../README.md)

## 决策时点决定能力边界

Head Sampling 在 trace 开始时决定是否采样，只知道入口属性；Tail Sampling 在 Collector 收到一条 trace 的多个 Span 后再决定，能够依据最终错误、总延迟或关键业务属性保留。

| 方式 | 优点 | 代价 |
| --- | --- | --- |
| Head | 决策快、内存低、成本可预测 | 无法预知后续错误与慢调用 |
| Tail | 可保留错误、慢链路和稀有路径 | 需缓存未完成 trace，延迟高且有状态 |

## Head Sampling 如何保持整条链一致

入口采样决策写入 trace flags，下游使用 parent-based sampler 继承。若每个服务独立按 1% 抽样，一条链会被切碎，最终既无法还原调用关系，也难以估算真实采样率。

概率采样适合高流量正常请求；还可以在入口按租户层级、接口或显式调试标记做受控采样，但不能允许任意客户端把所有请求设为 sampled 制造遥测拒绝服务。

## Tail Sampling 的容量账

Collector 必须等待 trace 完成或超时，内存近似为：

```text
每秒进入 trace 数 × 平均保留秒数 × 每条 trace 平均字节数
```

若每秒 50,000 条、等待 10 秒、平均 20 KB，未计开销就需要约 10 GB 缓冲。
Collector 重启、Span 晚到或负载不均会造成不完整 trace；需要按 trace ID 一致性路由到同一决策节点，
并设置内存保护和丢弃指标。

## 推荐的混合策略

1. SDK 端保留一个可承受的 head 比例，或让边缘 Collector 接收全量但严格限流。
2. Tail policy 保留错误、超过 P99 阈值、支付/结算等关键链路和少量随机正常样本。
3. 对已 head-drop 的 Span，Tail Sampling 无法恢复，因此两级策略要明确先后。
4. 指标和日志有独立保留策略，不能因为 trace 未采样就丢掉错误计数。

## 避免错误分析

基于错误和延迟过采样后的 trace 集合不是总体流量的无偏样本，不能直接用其中 50% 错误推断线上错误率。SLO 来自全量或统计正确的指标，trace 用于解释案例。

在 eMall 中，正常商品浏览低比例 head sample，订单支付错误、超时和补偿链路由 tail policy 强制保留；
发布前根据峰值 trace 大小压测 Collector，并告警 dropped spans、decision latency 和内存使用。
