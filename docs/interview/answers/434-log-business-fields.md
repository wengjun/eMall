# 434 日志中必须包含哪些业务字段？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

日志字段要能支持排障、审计和业务追溯。电商核心日志至少应包含 trace ID、用户 ID、订单号、
请求入口、接口名、结果码、耗时、关键业务状态和错误原因摘要。

字段要足够定位问题，但不能写入敏感明文。

## 通用字段

通用字段：

- trace ID。
- span ID。
- service name。
- instance ID。
- environment。
- timestamp。
- log level。
- request path。

这些字段支持技术排障。

## 业务字段

业务字段：

- userId。
- orderNo。
- paymentNo。
- skuId。
- merchantId。
- idempotencyKey。
- businessStatus。
- resultCode。

具体字段要按业务域选择。

## 禁止内容

不能记录：

- 完整手机号。
- 身份证号。
- 银行卡。
- token。
- secret。
- 完整地址。
- 密码或验证码。

敏感字段要脱敏或删除。

## 在 eMall 项目中怎么讲？

eMall 下单日志应记录 trace ID、userId、orderNo、skuId 列表摘要、幂等号、订单状态、结果码和耗时。

不要记录完整收货地址、手机号和支付凭证。排障需要的是关联字段，不是用户隐私。
