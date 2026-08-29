# 639 手写线程安全单例。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
final class Singleton {
    private Singleton() {
    }

    private static final class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }

    static Singleton instance() {
        return Holder.INSTANCE;
    }
}
```

### 必测用例

- 顺序和高并发调用都返回同一个实例，构造次数只能为 1。
- 若实现声称抵抗序列化或反射破坏，应分别提供反序列化和反射测试。
- 验证实例发布后字段可见性，不能只比较引用地址。

### 生产化差异

- Spring 应用优先让容器管理单例生命周期，不用全局静态状态隐藏依赖。
- 必须手写时，枚举或静态内部类通常比双重检查锁更容易证明正确。
