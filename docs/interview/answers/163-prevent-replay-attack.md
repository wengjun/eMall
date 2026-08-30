# 163 如何防止重放攻击？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

防重放通常依赖时间戳、nonce、签名和幂等记录。客户端请求带 timestamp 和 nonce，二者参与签名；
服务端校验时间窗口，例如 5 分钟内有效，并用 Redis 或数据库记录 nonce，确保同一 nonce 只能使用一次。

对有副作用接口，还要用业务幂等防止重复执行。

## 重放攻击是什么？

攻击者截获一次合法请求后，原样再次发送。

如果没有防护，系统可能重复执行：

- 下单。
- 扣款。
- 发券。
- 修改地址。

HTTPS 能防窃听，但不能替代应用层重放防护，尤其是日志泄漏或客户端被攻破时。

## timestamp

timestamp 用于限制请求有效期。

服务端校验：

```text
abs(serverTime - clientTime) <= allowedWindow
```

例如允许 5 分钟。

时间窗口过大，重放风险增大；过小，容易受时钟偏差影响。

## nonce

nonce 是一次性随机值。

服务端记录已使用 nonce：

```text
appId + nonce
```

在有效时间窗口内重复出现则拒绝。

Redis `SET NX EX` 很适合记录短期 nonce。

## 签名绑定

timestamp 和 nonce 必须参与签名。

否则攻击者可以改 timestamp 或 nonce 绕过校验。

签名还应绑定 method、path、query 和 body hash，防止内容被篡改。

## 业务幂等兜底

防重放不能替代业务幂等。

例如支付回调、创建订单、发券仍要有：

- 业务流水号。
- 唯一键。
- 状态机。
- 幂等表。

否则 nonce 记录异常时仍可能重复执行。

## 电商系统实践

开放平台商家调用创建售后单接口时，请求必须带 timestamp、nonce 和 signature。

服务端校验签名和 nonce 后，还要用商家请求号建唯一键，防止重复创建售后单。
