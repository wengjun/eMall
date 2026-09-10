# 470 Spring Boot 和 Java 线程池如何优雅关闭？

[返回按分类学习面试题](../README.md)

## Spring Boot 3.3 的显式配置

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Boot 的 WebServer 生命周期会协调停止接收新请求并等待在途请求，具体停止方式取决于服务器实现。
30 秒是每个 shutdown phase 的等待配置，不是整个进程退出的绝对上限。
版本行为见 [Spring Boot 3.3 优雅关闭](https://docs.spring.io/spring-boot/3.3/reference/web/graceful-shutdown.html)。

## 自建 Executor 仍需要管理

下面的方法适用于拥有该线程池生命周期的组件：

```java
static void stop(java.util.concurrent.ExecutorService executor) {
    executor.shutdown();
    try {
        if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    } catch (InterruptedException exception) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

shutdown 拒绝新任务并允许已接收任务完成；shutdownNow 请求中断并返回尚未执行的排队任务。
示例不保证不响应中断的任务已经退出，业务还需处理未执行任务。
Java 17 ExecutorService 不是 AutoCloseable，不能照搬新版本的 try-with-resources 写法。

SmartLifecycle 用于需要阶段协调的组件，@PreDestroy 用于销毁回调。
依赖资源不能先于使用它的工作线程关闭；Kafka 容器、Dubbo 和自建池各有生命周期，不能只配 WebServer。

容器强制终止时不会保证执行所有回调，因此部署的总宽限期必须覆盖这些阶段；此处不展开 Kubernetes 发布设计。
