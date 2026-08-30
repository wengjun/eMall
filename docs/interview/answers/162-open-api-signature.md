# 162 如何设计开放 API 签名？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

开放 API 签名通常使用 appId 标识调用方，使用 appSecret 或私钥生成签名。签名内容应包含 HTTP 方法、
路径、查询参数、请求体摘要、时间戳、nonce 和版本，并按固定规则 canonicalize 后计算 HMAC 或非对称签名。
服务端验签、校验时间窗口和 nonce，防止篡改和重放。

签名规则必须稳定、明确、可测试。

## 签名要解决什么？

主要解决：

- 调用方身份识别。
- 请求未被篡改。
- 防止重放攻击。
- 访问权限控制。
- 审计追踪。

签名不是加密，请求内容如果敏感还需要 HTTPS 和字段级保护。

## 参与签名的字段

通常包括：

- appId。
- timestamp。
- nonce。
- HTTP method。
- path。
- query parameters。
- body hash。
- signature version。

请求体不一定直接参与签名，可以用 body hash。

## canonicalization

签名前要规范化：

- 参数按字典序排序。
- URL 编码规则一致。
- 空值处理一致。
- JSON 序列化规则明确。
- 大小写规则明确。

很多签名失败来自规范化不一致。

## 签名算法

常见：

- HMAC-SHA256。
- RSA-SHA256。
- ECDSA。

内部合作方可用 HMAC。

开放平台更适合非对称签名，私钥由调用方保存，平台保存公钥。

## 电商系统实践

商家开放 API 请求带：

```text
X-App-Id
X-Timestamp
X-Nonce
X-Signature
X-Signature-Version
```

服务端校验签名、时间窗口、nonce 去重和接口权限，再处理业务。
