# 140 WebFlux 是否一定比 MVC 性能更高？

[返回按分类学习面试题](../README.md)

## 不重复 IO 模型，只辨别框架边界

不一定。选型时先识别 Java 调用栈：MVC 通常在 Servlet 请求线程上执行阻塞业务；
WebFlux 链路需要避免在少量事件循环线程上阻塞。

| 现有组件 | 是否因换 WebFlux 自动非阻塞 |
| --- | --- |
| MyBatis / JDBC | 否 |
| RestClient | 否 |
| WebClient 的非阻塞调用链 | 可以非阻塞，但调用 block 又会等待 |
| R2DBC | 是另一套数据库访问与事务栈，不是给 JDBC 加个注解 |

MVC 也支持异步请求处理，不能把它简单说成“完全不支持异步”。
WebFlux 也不是所有线程都一定是 Reactor Netty 的线程，实际服务器和调度配置影响执行模型。

若订单查询依赖 JDBC，首先掌握 MVC + Spring 事务 + 连接池就够了。
需要阅读网关或响应式代码时，再学习 [139](139-blocking-vs-reactive.md)。
性能比较只补一个 Java 维度：保持相同 JDK、GC、CPU 配额和预热状态，否则框架对比很容易失真。
