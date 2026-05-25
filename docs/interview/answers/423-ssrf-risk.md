# 423 SSRF 风险在哪里？

[返回按分类学习面试题](../README.md)

## 题目

SSRF 风险在哪里？

## 先给面试官的短答案

SSRF 是服务端根据用户可控 URL 发起请求，攻击者可能借服务端访问内网地址、云元数据地址、管理
接口或本不应暴露的服务。

风险点在于服务端网络位置比用户更可信，攻击者利用它绕过边界。

## 常见入口

入口：

- 图片 URL 抓取。
- webhook 回调。
- 文件导入。
- 远程模板加载。
- URL 预览。
- 第三方接口配置。

只要服务端能访问用户提供的 URL，就要警惕 SSRF。

## 防护方式

方式：

- URL 白名单。
- 禁止访问内网 IP。
- 禁止访问云元数据地址。
- DNS 解析后校验 IP。
- 限制协议为 HTTPS。
- 限制重定向。
- 独立出网代理。
- 网络层 egress 控制。

不能只检查字符串前缀。

## 难点

难点：

- DNS rebinding。
- 重定向到内网。
- IPv6 和特殊 IP 表达。
- 短链接跳转。
- 内网域名解析。

所以需要解析后校验和网络层限制。

## 在 eMall 项目中怎么讲？

eMall 商家上传商品图片如果允许填写远程图片 URL，图片抓取服务必须限制只能访问公网图片域名，
并通过独立代理出网。

禁止访问 `169.254.169.254`、`localhost`、内网网段和 Kubernetes Service 域名。

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
SSRF 的风险是攻击者控制服务端发起请求，从而访问内网服务、云元数据、管理接口或绕过防火墙。
入口常见于图片抓取、webhook、URL 预览和文件导入。

防护要做 URL 白名单、解析后 IP 校验、禁止内网和元数据地址、限制重定向、出网代理和网络层
egress 控制。字符串黑名单很容易被绕过。
```

## 回答评分点

高分答案应该覆盖：

- SSRF 是服务端请求被利用。
- 风险是访问内网和元数据。
- 常见入口是 URL 抓取和 webhook。
- 白名单和解析后 IP 校验。
- 网络层出网控制。
