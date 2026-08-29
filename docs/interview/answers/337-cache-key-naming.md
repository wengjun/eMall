# 337 缓存 key 如何命名？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

缓存 key 命名要可读、可管理、可定位、可演进。常见格式是业务域、对象类型、版本、标识和字段，
例如 `product:detail:v1:sku:10001`。

好的 key 设计能降低冲突、方便排查、支持灰度和版本切换。

## 命名原则

原则：

- 使用统一分隔符。
- 包含业务域。
- 包含对象类型。
- 必要时包含版本号。
- 标识字段稳定。
- 避免过长 key。
- 避免用户输入直接拼接。

key 命名也是系统接口的一部分。

## 示例

示例：

```text
product:detail:v1:sku:10001
cart:item:v1:user:20001
coupon:claimed:v1:coupon:30001
rate-limit:api:v1:user:20001
flash-sale:token:v1:activity:90001
```

这些 key 能直接看出业务含义。

## 版本设计

版本号用于：

- 缓存结构升级。
- 灰度发布。
- 避免旧 value 反序列化失败。
- 快速切换 key 空间。

但版本切换会导致缓存重建，要控制雪崩风险。

## 在 eMall 项目中怎么讲？

eMall 可以约定 key 格式：

```text
{domain}:{object}:v{version}:{id-type}:{id}
```

例如商品详情用 `product:detail:v1:sku:10001`，购物车用 `cart:items:v1:user:20001`。
如果是 Redis Cluster 多 key 操作，可以使用 hash tag，例如 `cart:{user:20001}:items`。
