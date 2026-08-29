# 641 手写生产者消费者模型。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.concurrent.BlockingQueue;

final class Consumer implements Runnable {
    private final BlockingQueue<String> queue;

    Consumer(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String message = queue.take();
                handle(message);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void handle(String message) {
        System.out.println(message);
    }
}
```

### 必测用例

- 多生产者、多消费者下无丢失、无重复，并能处理生产速度高于消费速度。
- 正常关闭应处理完已接收任务；强制关闭应中断阻塞线程并在期限内返回。
- 消费者抛异常不能静默终止整个消费能力。

### 生产化差异

- 使用受管线程池和有界队列，暴露生产速率、消费速率、积压和处理失败指标。
- 跨进程场景应使用消息系统，并补充确认、幂等、重试与死信语义。
