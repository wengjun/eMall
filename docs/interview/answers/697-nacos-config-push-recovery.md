# 697 Nacos Java 客户端如何接收并应用配置？

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

RiskConfig 和 RiskConfigParser 是示意业务类型。Listener 回调中先解析与校验，再整体替换快照；不要逐字段修改共享 Bean。
收到文本、更新属性对象和重建客户端资源不是同一步，Spring 绑定边界见 [133](133-profile-env-config-center.md)。
