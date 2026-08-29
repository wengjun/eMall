# 462 ConfigMap 和 Secret 有什么区别？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

ConfigMap 用于保存非敏感配置，Secret 用于保存密码、token、证书等敏感配置。两者都可以通过环境
变量或文件挂载给 Pod 使用，但 Secret 需要更严格的访问控制、加密存储和轮换机制。

Secret 不是天然绝对安全，默认只是更适合敏感数据的 Kubernetes 对象。

## ConfigMap

适合：

- 普通配置项。
- 功能开关。
- 日志级别。
- 外部服务地址。
- 非敏感业务参数。

ConfigMap 不应保存密码和密钥。

## Secret

适合：

- 数据库密码。
- API token。
- TLS 证书。
- app secret。
- 私有仓库凭证。

Secret 要配合 RBAC 和 etcd 加密。

## 注意点

注意：

- Secret base64 不是加密。
- 限制谁能读取 Secret。
- 避免通过环境变量泄露。
- 支持密钥轮换。
- 不把 Secret 写入镜像。

敏感配置要全链路保护。

## 在 eMall 项目中怎么讲？

eMall 的日志级别、限流阈值可以放 ConfigMap。

数据库密码、开放平台签名 secret、支付通道证书应放 Secret，并限制只有对应服务账号能读取。
