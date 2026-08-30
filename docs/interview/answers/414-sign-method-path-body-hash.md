# 414 签名为什么要覆盖 method、path、body hash？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

签名覆盖 method、path 和 body hash，是为了防止请求方法、目标资源和请求体被篡改。如果只签部分
参数，攻击者可能把同一个签名挪到其他接口、其他方法或修改 body。

签名必须覆盖所有影响业务语义的内容。

## method

method 影响语义：

- `GET` 通常查询。
- `POST` 通常创建。
- `PUT` 通常更新。
- `DELETE` 通常删除。

如果 method 不签名，攻击者可能改变操作类型。

## path

path 决定访问资源。

如果 path 不签名，同一个签名可能被挪到另一个接口。例如原本签的是查询订单，却被挪到退款接口。

## body hash

body hash 防止请求体被改。

直接签 body 可能受大 body、编码和换行影响。常见做法是先计算 body hash，再把 hash 放入 canonical
string 参与签名。

## 电商系统实践

大型电商系统开放平台商家调用退款接口时，签名必须覆盖 `POST`、`/refunds`、timestamp、nonce 和 body
hash。

如果攻击者修改退款金额、订单号或把请求挪到批量退款接口，签名验证会失败。
