# 459 Java 17 运行镜像有哪些特有的打包问题？

[返回按分类学习面试题](../README.md)

## 只保留 Java 运行时部分

Spring Boot 可执行 jar 不是把全部 class 平铺在普通 classpath 上：
应用类、依赖和启动器有自己的布局。通过 Boot 打包插件生成后用 java -jar 启动，
不要拿普通库模块 jar 当成独立服务镜像。

```dockerfile
ARG JAVA_RUNTIME_IMAGE
FROM ${JAVA_RUNTIME_IMAGE}
WORKDIR /app
COPY app.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

JAVA_RUNTIME_IMAGE 由构建侧传入批准的 Java 17 运行时镜像与固定 digest。
app.jar 需事先构建，运行用户还必须有日志、临时目录和诊断目录的适当权限。
exec 形式避免多余 shell 影响信号传递；容器通用安全设计不在这里展开。

## 不能只追求小

jlink 可以构建定制运行时，但 jdeps 静态分析可能看不到反射、SPI 和动态加载的模块依赖。
裁剪后要实际验证 TLS、时区、字符集、JDBC 驱动和序列化，不能只验证 main 能运行。

精简运行时可能没有 jcmd/jfr 等诊断工具；需要预先安排匹配版本的诊断方式。
Java 堆并非全部进程内存，参数预算见 [068](068-jvm-container-memory.md)。
