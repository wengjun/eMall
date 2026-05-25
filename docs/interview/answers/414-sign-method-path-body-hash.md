# 414 签名为什么要覆盖 method、path、body hash？

[返回按分类学习面试题](../README.md)

## 题目

签名为什么要覆盖 method、path、body hash？

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

## 在 eMall 项目中怎么讲？

eMall 开放平台商家调用退款接口时，签名必须覆盖 `POST`、`/refunds`、timestamp、nonce 和 body
hash。

如果攻击者修改退款金额、订单号或把请求挪到批量退款接口，签名验证会失败。

## 深度增强：安全治理图

![开放平台 API 安全链路](../assets/openapi-security.svg)

安全题要从身份、权限、数据、审计和攻击面分层回答。认证只证明是谁，授权判断能做什么；
加密保护机密性，签名保护完整性；审计负责事后追踪，风控负责发现异常行为。

## 深度增强：Java 17 安全策略示例

```java
import java.util.Set;

record SecurityDecision(boolean allowed, String reason) {
}

final class ScopePolicy {

    SecurityDecision check(Set<String> scopes, String requiredScope) {
        if (scopes.contains(requiredScope)) {
            return new SecurityDecision(true, "allowed");
        }
        return new SecurityDecision(false, "missing scope: " + requiredScope);
    }
}
```

这段代码体现最小权限原则。生产系统还要结合租户、资源归属、IP、设备、风险等级和操作审计。

## 深度增强：生产边界

安全不能只靠前端隐藏按钮，也不能只在网关做一次判断。核心资源要在服务层做资源级授权，
敏感数据要脱敏，密钥要支持轮换，失败日志不能泄露 token、secret、身份证号和银行卡号。

## 深度增强：面试高分表达

我会把安全问题拆成认证、授权、防篡改、防重放、数据保护、审计和风控。
每一层解决的问题不同，不能互相替代。这能体现我理解开放平台和电商系统的真实攻击面。

## 专家级完整回答

```text
签名要覆盖 method、path、query、body hash、timestamp 和 nonce，因为这些字段共同决定请求语义。
只签部分字段会留下重放到其他接口、改方法或改请求体的空间。

body 通常先做 hash 再参与签名，既能防篡改，又避免直接拼接大 body 的规范化问题。原则是所有影响
业务决策的内容都要进入签名。
```

## 回答评分点

高分答案应该覆盖：

- method 改变操作语义。
- path 决定资源和接口。
- body hash 防请求体篡改。
- 防签名被挪用到其他接口。
- 签名覆盖业务关键内容。
