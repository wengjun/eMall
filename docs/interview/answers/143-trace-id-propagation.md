# 143 Java 线程池切换时如何保留日志上下文？

[返回按分类学习面试题](../README.md)

## 不重复链路追踪设计，只看 MDC

SLF4J MDC 通常由日志实现提供线程本地存储。线程池复用不会自动把提交线程的 MDC 带到工作线程，
也不会自动清除上一项任务留下的值。

下面是仅处理 MDC 的任务装饰器片段，需要 SLF4J 以及支持 MDC 的日志实现：

```java
import java.util.Map;
import org.slf4j.MDC;

static Runnable withMdc(Runnable task) {
    Map<String, String> captured = MDC.getCopyOfContextMap();
    return () -> {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            if (captured == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(captured);
            }
            task.run();
        } finally {
            if (previous == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    };
}
```

在提交处 `executor.execute(withMdc(task))`，不是等工作线程开始后才捕获。
恢复 previous 而不是一律清空，能正确处理嵌套调用或任务退回提交线程执行的情况。

这个包装器不传播 Spring 事务、认证状态或 OpenTelemetry Context。
已有 instrumentation 时优先使用它的上下文传播，不要重复创建 Span。
完整追踪上下文只需继续看 [700](700-otel-context-baggage.md)。
