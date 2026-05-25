# 436 什么信息不能写入日志？

[返回按分类学习面试题](../README.md)

## 题目

什么信息不能写入日志？

## 先给面试官的短答案

不能写入日志的信息包括密码、验证码、token、secret、完整手机号、身份证号、银行卡号、完整地址、
支付凭证、私钥和敏感请求体。

日志系统通常会被更多人和系统访问，敏感信息一旦进入日志就很难彻底清除。

## 禁止记录

禁止：

- 密码。
- 验证码。
- access token。
- refresh token。
- app secret。
- 私钥。
- 完整身份证号。
- 完整银行卡号。

这些字段通常应完全删除。

## 需要脱敏

可脱敏记录：

- 手机号。
- 邮箱。
- 地址。
- 用户姓名。
- 证件号末尾。
- IP 地址。

是否记录取决于业务需要和合规要求。

## 常见泄露点

泄露点：

- 打印完整请求体。
- 打印异常上下文。
- 网关访问日志。
- 第三方 SDK debug 日志。
- SQL 参数日志。
- 审计日志设计不当。

敏感字段可能从很多入口进入日志。

## 在 eMall 项目中怎么讲？

eMall 支付回调日志不能打印完整支付凭证和签名 secret。开放平台请求日志不能打印 Authorization
header。

订单日志可记录 orderNo 和 userId，但手机号、地址和支付信息必须脱敏或不记录。

## 深度增强：Kubernetes 运维治理图

![Kubernetes 生产运行和故障治理](../assets/kubernetes-operations.svg)

Kubernetes 题不能只背 Deployment、Service 和 Ingress。生产稳定性还取决于资源 requests/limits、探针、HPA、PDB、
灰度发布、配置回滚、日志指标 Trace 和故障 Runbook。

## 深度增强：Java 17 发布门禁示例

```java
record ReleaseSignal(double errorRate, long p99Millis, double cpuThrottleRate, boolean rollbackSafe) {

    boolean canContinue() {
        return errorRate < 0.001
                && p99Millis < 300
                && cpuThrottleRate < 0.05
                && rollbackSafe;
    }
}
```

这段代码表达发布平台的核心：放量不是人工拍脑袋，而是由错误率、延迟、资源和回滚安全共同决定。

## 深度增强：生产边界

K8s 会重启失败容器，但不保证业务一定恢复。错误的 liveness probe 可能造成重启风暴；
过低的 CPU limit 会造成 throttling；不兼容数据库变更会让回滚失效。平台能力要和应用设计配合。

## 深度增强：面试高分表达

我会把 K8s 视为运行平台，而不是稳定性的全部答案。真正生产级要有容量规划、灰度门禁、配置治理、可观测性、
自动回滚和数据库兼容检查，才能支撑核心交易链路。

## 专家级完整回答

```text
日志不能写入密码、验证码、token、secret、私钥、完整证件号、银行卡号、完整地址和支付凭证。
这些信息一旦进入日志，会被日志采集、备份、索引和多人访问扩大泄露面。

生产中要通过统一日志组件、过滤器、异常处理和日志扫描防止敏感信息落盘，并对必要字段做脱敏。
```

## 回答评分点

高分答案应该覆盖：

- 密码、token、secret 不能记录。
- 个人敏感信息要脱敏。
- 完整请求体风险大。
- 日志泄露面比数据库更广。
- 需要统一组件和扫描治理。
