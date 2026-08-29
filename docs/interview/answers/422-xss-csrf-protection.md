# 422 XSS 和 CSRF 在前后端系统中如何防护？

[返回按分类学习面试题](../README.md)

## 题目

XSS 和 CSRF 在前后端系统中如何防护？

## 先给面试官的短答案

XSS 是攻击者把恶意脚本注入页面，防护重点是输出编码、输入清洗、CSP、HttpOnly Cookie 和避免
危险 HTML 渲染。CSRF 是诱导用户浏览器带着登录态发起请求，防护重点是 CSRF token、SameSite
Cookie、校验 Origin/Referer 和高危操作二次确认。

二者攻击方式不同，防护手段也不同。

## XSS 防护

做法：

- 默认 HTML 转义。
- 富文本白名单清洗。
- 禁止直接渲染不可信 HTML。
- 设置 CSP。
- Cookie 设置 HttpOnly。
- 前端依赖安全升级。

XSS 的目标常常是窃取 token 或执行高危操作。

## CSRF 防护

做法：

- CSRF token。
- SameSite Cookie。
- 校验 Origin 和 Referer。
- 高危操作要求二次验证。
- 不用 GET 做状态变更。

CSRF 利用的是浏览器自动携带 Cookie。

## 后端责任

后端要做：

- 状态变更接口校验 CSRF。
- 鉴权不能只靠前端。
- 返回内容做安全头。
- 高危接口校验用户意图。
- 审计异常操作。

前后端要共同防护。

## 在 eMall 项目中怎么讲？

eMall 商品评价富文本必须清洗，不能允许用户提交 `<script>`。后台管理系统 Cookie 设置 HttpOnly、
Secure 和 SameSite。

批量退款、改价和下架商品等状态变更接口必须使用 POST，并校验 CSRF token 或 Origin。

## 共性安全模型

认证、资源级授权、数据保护、审计和风控的分层方法及 Java 17 最小权限示例见
[共享模型：安全分层和最小权限](../shared-answer-models.md#安全分层和最小权限)。

## 专家级完整回答

```text
XSS 是脚本注入，防护重点是输出编码、富文本清洗、CSP、HttpOnly Cookie 和避免危险渲染。CSRF
是借用户登录态发起伪造请求，防护重点是 CSRF token、SameSite Cookie、Origin 校验和高危操作
二次确认。

两者都不能只靠前端解决。后端必须做鉴权、资源校验、安全头和审计。
```

## 回答评分点

高分答案应该覆盖：

- XSS 是脚本注入。
- CSRF 是伪造用户请求。
- XSS 防输出编码和 CSP。
- CSRF 防 token、SameSite 和 Origin。
- 高危操作要二次确认。
