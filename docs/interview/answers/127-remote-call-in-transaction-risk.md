# 127 如何控制 Spring 数据库事务里的远程调用边界？

[返回按分类学习面试题](../README.md)

## 只看 Spring 事务绑定了什么

命令式 Spring 事务通常把 Connection 等资源绑定到当前线程。
HTTP/Dubbo 调用不会因发生在 @Transactional 方法里就加入同一个数据库事务；
事务超时也不是能够中断任意远程调用的通用定时器。

只读报价查询可以先完成，再进入短数据库事务。示意片段如下，priceClient 和 mapper 为注入依赖：

```java
var quote = priceClient.quote(skuId);
transactionTemplate.executeWithoutResult(status -> {
    int changed = mapper.updateQuotedPrice(orderId, expectedVersion, quote.priceInCents());
    if (changed != 1) {
        throw new IllegalStateException("order changed; quote must be revalidated");
    }
});
```

这里 TransactionTemplate 管理回调边界，但若调用处已经在 REQUIRED 事务里，它默认仍会加入外层事务，
并不会凭空缩短事务。必须同时检查上层 @Transactional 入口。
报价也可能过期，不能为了缩短事务而删去版本/有效期校验。

## 哪些 Spring API 容易被误解

`NOT_SUPPORTED` 会挂起已有事务，但不表示它的数据库资源已释放。
`REQUIRES_NEW` 往往还要另一条连接，不能当成“把远程调用放出事务”的廉价修复。

写支付、扣库存等远程副作用需要独立的一致性协议；本题只学习 Java 事务边界，不重复该设计。
