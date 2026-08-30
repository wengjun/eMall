# 702 指标基数、Histogram 和 Exemplar 如何正确设计？

[返回按分类学习面试题](../README.md)

## 基数先于指标数量

一条 time series 由指标名和完整标签集合确定。标签组合数近似相乘：50 个服务、20 个接口、10 个状态、3 个区域可能产生 30,000 条序列；再加用户 ID 就会变成不可控规模。

禁止把 `user_id`、`order_id`、trace ID、完整 URL、异常消息放入普通指标标签。它们属于日志或 trace。HTTP route 应使用模板 `/orders/{id}`，错误标签使用受控枚举。

## 选择正确的 instrument

- Counter：只增不减的事件总量，如订单创建数、错误数。
- UpDownCounter：可增可减的并发任务、队列占用等变化量。
- Gauge：某时刻观测值，如线程池队列长度或副本延迟。
- Histogram：记录延迟、请求大小和批次大小的分布。

平均延迟会掩盖长尾；Histogram 在采集端聚合每个 bucket 的累计计数，可以计算分位数和 SLO 区间。
bucket 应围绕业务目标设计，例如 50、100、200、500、1000 ms，而不是复制一套与 SLO 无关的默认边界。

## Histogram 的精度和成本

边界越多、标签越多，序列和传输成本越大；边界太粗则无法判断 300 ms SLO。聚合后的 histogram 可以跨实例求和，客户端直接上报的 P99 通常不能正确求全局 P99。

指数 Histogram 用尺度自动覆盖宽值域，后端支持情况和误差预算要验证。变更 bucket 会改变序列语义，应版本化仪表盘与告警。

## Exemplar 把“趋势”连到“案例”

Exemplar 在某个 histogram 观测样本旁附带 trace/span 引用。值班人员从“500-1000 ms bucket 激增”可跳到一条代表性慢 trace，而不把高基数 trace ID 变成普通标签。

Exemplar 只保存少量样本，不替代 trace 采样，也不能证明 bucket 内所有请求具有相同根因。

## 电商系统指标契约

```text
http.server.duration{service,route,method,status_class,region}
order.created.total{channel,result,region}
inventory.reserve.duration{result,shard}
```

`shard` 只有在数量有硬上限时才可用；商家和 SKU 不进入标签。每个新标签在代码评审中估算最大基数，Collector 和后端设置溢出/限制并暴露 dropped series。

告警用全量 Counter/Histogram 计算错误预算消耗，再通过 Exemplar 和 trace 定位根因，形成指标发现、链路解释、日志取证的闭环。

参考：[OpenTelemetry Metrics Specification](https://opentelemetry.io/docs/specs/otel/metrics/)
