# 642 手写简化线程池。

[返回按分类学习面试题](../README.md)

## 题目

手写简化线程池。

## 先给面试官的短答案

现场编码要先澄清输入输出、边界和复杂度，再写最小正确实现，并说明生产化改造。

## 核心拆解

- 先澄清输入、输出、异常和并发边界。
- 用 Java 17 或 SQL 写小而正确的核心实现。
- 补充复杂度、测试用例和失败场景。
- 说明面试实现与生产实现的差异。

## 深度完善：现场编码到生产代码

围绕「手写简化线程池。」，现场编码题要先保证小而正确，再补边界、复杂度、测试和生产化差异。
面试时不要一上来堆代码，先澄清输入规模、线程安全要求、异常策略和是否允许使用 JDK 容器。

### 参考实现或关键片段

```java
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

final class SimpleThreadPool {
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    SimpleThreadPool(int workers) {
        for (int i = 0; i < workers; i++) {
            Thread thread = new Thread(this::runWorker, "simple-pool-" + i);
            thread.start();
        }
    }

    void submit(Runnable task) {
        tasks.add(task);
    }

    private void runWorker() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                tasks.take().run();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### 必测用例

- 正常路径：最小输入、典型输入和容量边界。
- 异常路径：空输入、重复输入、超限输入、非法状态和超时中断。
- 并发路径：多线程同时调用时是否丢数据、重复执行或破坏顺序。
- 复杂度：说明时间复杂度、空间复杂度，以及为什么满足题目约束。

### 生产化差异

- 现场实现追求清晰正确，生产实现还要补指标、日志、超时、限流、配置和回滚。
- 如果涉及状态写入，要考虑幂等、唯一约束、事务边界和失败补偿。
- 如果涉及并发，要说明锁粒度、线程池隔离、队列容量和拒绝策略。

## 编码题面试步骤

围绕「手写简化线程池。」，现场编码题的高分点不是写得最长，而是澄清、实现、验证、优化四步完整。

### 开始前先澄清

- 输入规模是多少，是否可能为空、重复、乱序或超限。
- 是否要求线程安全，是否允许使用 JDK 集合或并发包。
- 失败时返回错误、抛异常还是忽略，是否需要幂等。
- 复杂度目标是什么，是否需要考虑内存上限。

### 写完后主动验证

- 用一个正常用例证明主路径。
- 用一个边界用例证明容量、空值、重复或非法输入。
- 用一个失败用例证明异常或冲突处理。
- 最后说出时间复杂度、空间复杂度和生产化差异。

### 生产化补充

面试代码通常不等于生产代码。生产中还要补参数校验、指标、日志、限流、超时、配置化、
并发安全、资源释放、灰度开关和回滚策略。涉及数据库或 MQ 时，还要补事务、唯一约束、幂等和补偿。
