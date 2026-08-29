# 697 Nacos 配置发布、推送、回滚和故障恢复如何设计？

[返回按分类学习面试题](../README.md)

## 发布链路

配置由 namespace、group、dataId 唯一定位。发布首先把新内容写入 Nacos 的权威状态；
服务端向订阅客户端发送变化通知，客户端再查询最新内容、校验并触发 Listener。
通知和获取内容是两个步骤，不能把通知本身当作配置数据。

客户端长连接断开后应重连并重新订阅；本地 snapshot/failover 文件让应用在 Nacos 暂时不可达时仍能启动或保持最后一次已知配置。

## “收到配置”不等于“安全生效”

配置变更应经过四个阶段：解析、语义校验、构造新不可变对象、原子替换。任何一步失败都保留旧对象。

```java
public final class RiskConfigHolder {
    private final AtomicReference<RiskConfig> current;

    public RiskConfigHolder(RiskConfig initial) {
        this.current = new AtomicReference<>(initial);
    }

    public void install(String content) {
        RiskConfig candidate = RiskConfigParser.parse(content);
        candidate.validate();
        current.set(candidate);
    }

    public RiskConfig current() {
        return current.get();
    }
}
```

不要逐字段修改共享 Bean，否则并发请求会看到半新半旧状态。连接池大小、线程数等资源型配置还需先构造新资源、通过健康检查，再交换并优雅关闭旧资源。

## 灰度和回滚协议

- 配置必须带 schema version、变更单号、操作者和业务版本兼容范围。
- 先对少量实例或标签发布，比较错误率、延迟和业务指标，再逐批扩大。
- 保存上一个已验证版本，回滚是重新发布旧内容，不是直接改数据库。
- 删除字段采用 expand/contract：先让所有程序兼容缺省值，再停止写旧字段，最后删除。

配置回滚不能回滚已经发生的外部副作用。例如误把优惠比例从 10% 改为 90%，恢复配置只能停止继续损失，已创建订单仍需审计与补偿。

## 安全与权限

配置中心不是秘密管理器的天然替代品。数据库密码、私钥等应由 KMS/Secret Manager 管理并按工作负载授权；
Nacos 使用 TLS、认证、RBAC 和 namespace 隔离，发布操作进入审计日志。应用日志不得打印完整敏感配置。

## eMall 故障演练

测试 Nacos 不可达启动、运行时断连、坏格式、语义越界、监听器抛异常、灰度实例回滚和旧程序读取新 schema。关键指标包括配置版本分布、生效失败数、客户端滞后时间和回滚耗时。

参考：[Nacos 配置发布、查询与监听](https://nacos.io/en/docs/latest/manual/user/config/publish-query-listen/)
