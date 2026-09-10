# 132 Spring Boot 配置加载优先级如何排查？

[返回按分类学习面试题](../README.md)

## 先看最终 PropertySource

对于普通运行场景，常见覆盖关系是：命令行参数高于 Java system properties，
后者高于 OS 环境变量，再高于配置数据文件。
测试、开发工具等还会引入其他来源；完整顺序见
[Spring Boot 3.3 外部配置](https://docs.spring.io/spring-boot/3.3/reference/features/external-config.html)。

```text
java -Dserver.port=8081 -jar app.jar --server.port=8082
```

普通启动中此例最终为 8082。-D 参数要位于 -jar 前，不要把 JVM 参数写成了应用参数。

## 配置文件仍有内部顺序

外部配置、jar 内配置、profile 专用文件和 spring.config.import 都会影响合并结果。
spring.config.location 与 spring.config.additional-location 的含义不同：替换搜索位置与追加位置不能混用。
Nacos 的优先级也取决于所用 starter、导入方式与配置，不能一律说“配置中心永远最高”。

使用 Environment、受保护的 Actuator env/configprops 和启动报告查看最终值及来源。
@Value 注入后的字段值、@ConfigurationProperties 对象和 Environment 当前值还可能处于不同更新阶段，
配置源更新不等于所有已有 Bean 自动更新。动态配置另看 [133](133-profile-env-config-center.md)。
