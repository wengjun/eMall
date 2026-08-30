# 135 Actuator 暴露哪些端点比较合理？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

生产环境 Actuator 应最小化暴露。通常可以暴露 `health`、`info`、`prometheus` 或 metrics 采集端点；
`env`、`beans`、`configprops`、`heapdump`、`threaddump` 等敏感端点不应公网暴露，只能在内网、鉴权、
审计和临时授权下使用。

Actuator 是运维能力，也可能是安全风险。

## 常见安全端点

相对常见：

- `health`。
- `info`。
- `prometheus`。
- `metrics`。

即使是这些端点，也应限制访问来源。

## 敏感端点

敏感端点包括：

- `env`。
- `configprops`。
- `beans`。
- `heapdump`。
- `threaddump`。
- `loggers`。
- `shutdown`。

这些可能泄漏配置、密钥、内部类结构、线程栈和内存数据。

生产不能随便开放。

## health 也要分层

健康检查要区分：

- liveness。
- readiness。
- startup。

不要把所有下游都放进 liveness，否则下游短暂故障可能导致应用被 Kubernetes 重启。

## prometheus 端点

`prometheus` 端点通常给监控系统抓取。

应通过：

- 内网访问。
- ServiceMonitor。
- 鉴权。
- 网络策略。

不要直接公网暴露。

## 临时诊断端点

`heapdump`、`threaddump`、`env` 等可以用于排查。

但要满足：

- 内网。
- 鉴权。
- 审计。
- 临时开启。
- 脱敏。

排查结束后关闭。

## 电商系统实践

大型电商系统生产服务默认只暴露健康检查和 Prometheus 指标。

配置、线程 dump、heap dump 通过运维平台临时授权获取。

支付和用户服务尤其要避免泄漏密钥、token 和用户数据。
