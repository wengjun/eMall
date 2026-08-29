# 651 手写 HMAC 签名校验。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

final class HmacSigner {
    static String sign(String payload, byte[] secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes()));
    }
}
```

### 必测用例

- 正确签名通过，方法、路径、查询、正文或时间戳任一字段变化都失败。
- 覆盖参数顺序、空正文、字符编码和十六进制大小写的规范化边界。
- 过期时间戳、重复 nonce、未知 key ID 和错误密钥必须被拒绝。

### 生产化差异

- 定义唯一规范串并使用常量时间比较，密钥来自 Secret 管理系统且支持轮换。
- HMAC 防篡改不等于防重放，必须结合时间窗、nonce 和 HTTPS。
