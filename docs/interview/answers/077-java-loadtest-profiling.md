# 077 如何做 Java 性能剖析，避免微基准误判？

[返回按分类学习面试题](../README.md)

## 保留 Java 特有的测量方法

压测流量模型和容量估算不再重复。Java 新增的变量是分层编译、JIT 预热、去优化、逃逸分析与 GC。
同一段代码，冷启动和稳定运行的机器码可能不同。

## 在线服务用 JFR

```text
jcmd <pid> JFR.start name=profile settings=profile duration=60s filename=profile.jfr
```

在有代表性的负载下采样，关注执行样本、分配、monitor 竞争、线程 park 和 IO 事件。
CPU 样本回答“哪里在运行”，等待事件回答“为什么不运行”；两者不能互相替代。
事件是否启用及其阈值由录制设置决定，JFR 里没看到事件不等于从未发生。

## 单段 Java 代码用 JMH

不要用一次 `System.nanoTime()` 循环比较两个实现。JIT 可能消除未使用的计算或把常量提前折叠。

下面是需要 JMH 依赖与注解处理器的基准片段：

```java
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(2)
public class ParseBenchmark {
    private String input = "12345";

    @Benchmark
    public int parse() {
        return Integer.parseInt(input);
    }
}
```

返回结果能让框架消费它，避免无用结果被直接消除；输入仍要覆盖实际分布，不能只测一个常量代表所有场景。
用独立 fork 降低前一个实验编译状态的污染，用 `-prof gc` 观察分配。
微基准只解释局部成本，不能推出整个订单接口的吞吐。优化后仍要回到服务负载下验证。
