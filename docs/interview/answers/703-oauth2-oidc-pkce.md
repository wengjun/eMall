# 703 OAuth2、OIDC 和 PKCE 分别解决什么问题？

[返回按分类学习面试题](../README.md)

## 三者不能互换

[第 405 题](405-session-jwt-oauth2-tradeoff.md)已经讲过 Session、JWT 和 OAuth2 的选型，本题不再重复，
只补充 OIDC 与 PKCE 的协议细节。

- OAuth 2.x 是授权框架：客户端获得 Access Token 去访问 Resource Server。
- OpenID Connect 在 OAuth 之上增加身份认证协议，核心产物是描述本次登录的 ID Token。
- PKCE 把授权码绑定到发起流程的客户端实例，攻击者即使截获 code，没有 `code_verifier` 也无法兑换 Token。

“拿到 OAuth Token 就证明用户是谁”是错误表述；客户端登录应按 OIDC 校验 ID Token。

## Authorization Code + PKCE 流程

```text
客户端生成 verifier
  -> challenge = BASE64URL(SHA256(verifier))
  -> 浏览器跳转授权端，提交 challenge
  <- 回调得到一次性 code
  -> 后端用 code + verifier 换 Token
  <- Access Token + ID Token + 可选 Refresh Token
```

授权端在发 code 时绑定 challenge，Token Endpoint 再验证 verifier。推荐使用 `S256`，不能用固定 verifier，也不能在请求中传输原 verifier。

## 每种 Token 给谁看

- Access Token 的 audience 是 Resource Server，API 校验签名、issuer、audience、有效期、scope 和撤销策略。
- ID Token 的 audience 是 Client，用于确认登录；API 不应把任意 ID Token 当访问凭证。
- Refresh Token 只交给授权端换新 Token，存储和轮换要求高于短期 Access Token。

JWT 只是 Token 格式，不等同于 OAuth/OIDC。Resource Server 也不能只做 Base64 解码，必须验证允许算法、签名和 claims，并缓存 JWKS 时支持密钥轮换。

## Web 与手机端的落地

公开客户端无法安全保存固定 client secret，所以手机 App 和浏览器都应使用 PKCE。Web 可采用 BFF，
把 Token 保存在服务端受控会话中，浏览器只持有 `HttpOnly`、`Secure`、合适 `SameSite` 的 cookie，
降低 Token 被脚本窃取的风险。

回调 URI 使用预注册精确匹配；`state`/`nonce` 按协议绑定会话，防 CSRF、重放和混淆。不要使用 implicit flow，也不要把 Access Token 放 URL。

## 电商系统的授权模型

网关完成 Token 密码学验证后，下游仍按资源和动作授权，例如“只能读取自己的订单”“客服需特定 role 且操作留审计”。不能信任客户端传入的 `userId`，应从已验证 subject 映射内部用户。

注销、风控冻结和权限变更要求短 Token TTL、Refresh Token 轮换/撤销或高风险接口在线检查。安全测试覆盖 code 重放、PKCE downgrade、错误 audience、旧 key、开放重定向和跨账号资源访问。

参考：

- [RFC 9700 OAuth 2.0 Security BCP](https://www.rfc-editor.org/info/rfc9700/)
- [OpenID Connect Core](https://openid.net/specs/openid-connect-core-1_0-18.html)
