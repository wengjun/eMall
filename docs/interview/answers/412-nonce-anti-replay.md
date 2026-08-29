# 412 nonce 如何防重放？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

nonce 是一次性随机数或唯一请求号。服务端在签名验证通过后检查同一个 appKey 在时间窗口内是否
已经使用过该 nonce，如果使用过就拒绝，从而防止攻击者重复提交同一请求。

nonce 通常要和 timestamp 一起使用，避免无限期保存。

## 工作方式

流程：

- 客户端生成随机 nonce。
- nonce 参与签名。
- 服务端验证签名。
- 服务端检查 nonce 是否已存在。
- 不存在则写入缓存并设置 TTL。
- 已存在则拒绝重放请求。

检查和写入最好原子完成。

## 为什么要 timestamp

timestamp 作用：

- 限制请求有效时间窗口。
- 限制 nonce 保存时长。
- 降低存储成本。
- 拒绝过旧请求。

没有 timestamp，服务端需要永久记住所有 nonce。

## 存储设计

可以使用 Redis：

```text
replay:{appKey}:{nonce}
```

使用 `SET NX PX` 写入。写入成功表示首次请求，写入失败表示 nonce 已被使用。

## 在 eMall 项目中怎么讲？

eMall 开放平台支付回调或商家订单接口，应要求请求携带 nonce。服务端用 Redis 记录 `appKey` 和
nonce，TTL 与 timestamp 窗口一致。

如果同一 nonce 再次出现，即使签名正确，也拒绝执行，避免重复改价或重复回调。
