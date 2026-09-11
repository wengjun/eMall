# Java 技术栈学习（按分类）

[返回工程 README](../../README.md)

这是本题库唯一的学习入口，面向已有 C++ 服务端开发经验、有一定 Java 基础，但需要熟练掌握 Java 技术栈的开发者。
当前保留 **106 道题**，按“一级技术方向 -> 二级专题 -> 具体题目”组织，原题号不重排。

## 学习范围

不再重复通用服务端设计、微服务拆分、容量规划、交易一致性方案、事故处置流程、行为面试和反问模板。
MySQL、Redis、Kafka 等中间件的通用原理不作为本题库的独立复习任务，但保留它们在 Java 客户端和框架中的具体使用边界。

Java 基础题只需快速核对语言差异、API 和常见坑，已经能写出的部分可以跳过。
重点学习 JVM、Java 内存语义与异步 API、Spring 生命周期/代理/事务、MyBatis、Dubbo、Nacos，以及 Java 诊断和测试工具。
电商案例仅用于说明代码，不要求绑定某个项目或重新学习完整电商架构。

以 **Java 17** 为语言基线，Spring 示例主要对应 Boot 3.x / Framework 6.x，涉及具体版本的行为在题内说明。
代码块既有可独立实验的示例，也有方法或配置片段；片段中的 Mapper、客户端和业务类型需要放进对应依赖环境，
不能把每个代码块都当成可直接运行的完整程序。

## 阅读顺序

重点学习第四、第五类以熟悉业务开发；第二、第三类用于理解运行机制和定位问题。
第六类补齐 Java 项目的构建、测试与运行工具。不要求从头背诵，也不再设置完成标记。

## 02 JVM 机制与诊断

### 内存、对象与垃圾回收

- [042. 堆、栈、方法区、直接内存分别存什么？](answers/042-heap-stack-metaspace-direct-memory.md)
- [043. 对象从创建到回收大致经历什么过程？](answers/043-object-lifecycle.md)
- [044. GC Roots 包括哪些？](answers/044-gc-roots.md)
- [045. Minor GC、Major GC、Full GC 有什么区别？](answers/045-minor-major-full-gc.md)
- [046. G1、ZGC、Shenandoah 的设计目标有什么不同？](answers/046-g1-zgc-shenandoah.md)
- [047. 为什么低延迟服务要关注 GC 暂停？](answers/047-low-latency-gc-pause.md)
- [048. 如何判断线上服务是否存在内存泄漏？](answers/048-detect-memory-leak.md)
- [049. `OutOfMemoryError` 常见类型有哪些？](answers/049-oome-types.md)
- [050. 堆 OOM 和直接内存 OOM 如何区分？](answers/050-heap-vs-direct-oom.md)
- [051. 线程数过多时，Java 17 应检查什么？](answers/051-too-many-threads.md)

### 诊断工具与启动分析

- [052. jstack 可以定位哪些问题？](answers/052-jstack-diagnostics.md)
- [053. jmap、jcmd、JFR 分别适合什么场景？](answers/053-jmap-jcmd-jfr.md)
- [054. 如何分析 CPU 飙高？](answers/054-analyze-high-cpu.md)
- [057. Java 服务启动慢可能有哪些原因？](answers/057-java-service-slow-startup.md)

### 类加载与 JIT

- [058. 类加载机制是什么？](answers/058-class-loading-mechanism.md)
- [059. 双亲委派模型解决什么问题？](answers/059-parent-delegation.md)
- [060. 什么场景需要自定义 ClassLoader？](answers/060-custom-classloader.md)
- [061. JIT 编译是什么？](answers/061-jit-compilation.md)
- [062. 热点代码和解释执行有什么区别？](answers/062-hot-code-vs-interpretation.md)
- [063. 逃逸分析有什么作用？](answers/063-escape-analysis.md)
- [064. 对象分配为什么通常很快？](answers/064-fast-object-allocation.md)
- [065. 为什么频繁创建短生命周期对象不一定总是坏事？](answers/065-short-lived-objects.md)
- [066. 如何减少不必要的对象分配？](answers/066-reduce-object-allocation.md)

### 参数、日志与性能验证

- [067. 如何设置生产环境 JVM 参数？](answers/067-production-jvm-options.md)
- [068. 容器环境下 JVM 如何感知内存限制？](answers/068-jvm-container-memory.md)
- [069. -Xmx 设置过大或过小分别有什么风险？](answers/069-xmx-too-large-or-small.md)
- [070. 线上是否应该主动调用 System.gc()？](answers/070-system-gc-production.md)
- [071. 如何采集和解释 JVM 指标？](answers/071-jvm-metrics-monitoring.md)
- [073. GC 日志如何阅读？](answers/073-read-gc-log.md)
- [074. 线程池队列堆积和 JVM 内存上涨有什么关系？](answers/074-threadpool-queue-memory.md)
- [075. 如何定位死锁？](answers/075-diagnose-deadlock.md)
- [076. 如何定位锁竞争？](answers/076-diagnose-lock-contention.md)
- [077. 如何做 Java 性能剖析，避免微基准误判？](answers/077-java-loadtest-profiling.md)

## 03 Java 并发与异步 API

### 线程、锁与内存语义

- [079. Java 线程状态有哪些？](answers/079-java-thread-states.md)
- [080. synchronized 的原理是什么？](answers/080-synchronized-principle.md)
- [081. Java 17 还需要背偏向锁和锁升级路径吗？](answers/081-lock-upgrade.md)
- [082. ReentrantLock 和 synchronized 怎么选？](answers/082-reentrantlock-vs-synchronized.md)
- [083. 公平锁和非公平锁有什么区别？](answers/083-fair-vs-nonfair-lock.md)
- [084. volatile 解决什么问题，不能解决什么问题？](answers/084-volatile.md)
- [085. happens-before 规则是什么？](answers/085-happens-before.md)
- [086. Java 内存模型解决什么问题？](answers/086-java-memory-model.md)
- [089. AtomicInteger 和 LongAdder 如何取舍？](answers/089-atomicinteger-vs-longadder.md)
- [090. CountDownLatch、CyclicBarrier、Semaphore 分别适合什么场景？](answers/090-latch-barrier-semaphore.md)

### 异步组合、线程池与取消

- [091. CompletableFuture 如何处理异步编排？](answers/091-completablefuture-async-composition.md)
- [092. CompletableFuture 的回调到底在哪个线程执行？](answers/092-completablefuture-default-pool-risk.md)
- [093. 公共 ForkJoinPool 和 parallelStream 有哪些 Java 使用边界？](answers/093-avoid-common-forkjoinpool.md)
- [094. ThreadPoolExecutor 参数怎样影响实际执行顺序？](answers/094-threadpool-core-parameters.md)
- [096. Java 线程池的队列类型该怎么选？](answers/096-bounded-vs-unbounded-queue.md)
- [097. 拒绝策略怎么选？](answers/097-rejection-policy.md)
- [098. 如何避免 Java 线程池中的任务饥饿和失控？](answers/098-avoid-threadpool-avalanche.md)
- [102. 任务超时后线程是否真的停止？](answers/102-timeout-does-not-stop-thread.md)
- [103. Java 中断机制如何正确使用？](answers/103-java-interruption.md)
- [104. 如何设计可取消的 Java 异步任务？](answers/104-cancellable-async-task.md)

## 04 Spring 编程机制

### 容器、自动配置与代理

- [115. Spring Boot 自动配置原理是什么？](answers/115-spring-boot-auto-configuration.md)
- [116. @SpringBootApplication 包含哪些注解？](answers/116-springbootapplication.md)
- [117. Bean 的生命周期是什么？](answers/117-spring-bean-lifecycle.md)
- [118. 构造函数注入、字段注入、Setter 注入如何取舍？](answers/118-injection-styles.md)
- [120. Spring AOP 的代理机制是什么？](answers/120-spring-aop-proxy.md)
- [121. JDK 动态代理和 CGLIB 有什么区别？](answers/121-jdk-proxy-vs-cglib.md)
- [147. Spring 如何处理 Bean 循环依赖？](answers/147-avoid-bean-circular-dependency.md)
- [148. 如何设计 starter 或 auto-configuration？](answers/148-design-starter-auto-configuration.md)
- [149. 如何在多模块项目中复用公共配置？](answers/149-reuse-common-config-in-multi-module.md)

### 事务与数据库资源边界

- [122. @Transactional 为什么有时不生效？](answers/122-transactional-not-effective.md)
- [123. 自调用为什么绕过事务代理？](answers/123-self-invocation-bypass-transaction.md)
- [124. 事务传播行为有哪些？](answers/124-transaction-propagation.md)
- [125. REQUIRED、REQUIRES_NEW、NESTED 有什么区别？](answers/125-required-requires-new-nested.md)
- [126. 事务隔离级别如何配置？](answers/126-transaction-isolation-config.md)
- [127. 如何控制 Spring 数据库事务里的远程调用边界？](answers/127-remote-call-in-transaction-risk.md)

### Web、校验与异常处理

- [129. @ControllerAdvice 如何做统一异常处理？](answers/129-controller-advice.md)
- [130. Bean Validation 适合做哪些校验？](answers/130-bean-validation.md)
- [653. 手写统一异常处理。](answers/653-exception-handler.md)

### 配置绑定、健康检查与缓存

- [132. Spring Boot 配置加载优先级如何排查？](answers/132-spring-config-priority.md)
- [133. Spring profile、属性绑定和动态配置如何配合？](answers/133-profile-env-config-center.md)
- [135. Actuator 暴露哪些端点比较合理？](answers/135-actuator-endpoints.md)
- [137. readiness 和 liveness 在 Spring 中如何实现？](answers/137-readiness-liveness-spring.md)
- [145. Spring Cache 的注解和失效边界是什么？](answers/145-spring-cache-boundary.md)
- [146. Spring 事件如何绑定执行线程和事务阶段？](answers/146-spring-event-vs-mq.md)

### HTTP 客户端与 Reactor

- [138. RestClient、WebClient、Feign 如何取舍？](answers/138-restclient-webclient-feign.md)
- [139. Java 阻塞调用如何接入 Reactor？](answers/139-blocking-vs-reactive.md)
- [140. WebFlux 是否一定比 MVC 性能更高？](answers/140-webflux-vs-mvc-performance.md)
- [141. 如何配置 Java HTTP 客户端的连接池？](answers/141-http-client-connection-pool.md)
- [142. Java HTTP 超时分别在哪里设置？](answers/142-http-timeouts.md)

## 05 持久化与中间件 Java 接入

### MyBatis、MyBatis-Plus 与 HikariCP

- [323. MyBatis-Plus 和手写 SQL 如何取舍？](answers/323-mybatis-plus-vs-handwritten-sql.md)
- [654. 如何用 MyBatis-Plus 实现分页查询？](answers/654-pagination-api.md)
- [690. MyBatis 从 Mapper 调用到 SQL 执行经历什么过程？](answers/690-mybatis-execution-pipeline.md)
- [691. MyBatis 一级、二级缓存的事务边界和风险是什么？](answers/691-mybatis-cache-transaction-boundary.md)
- [692. MyBatis 插件和 MyBatis-Plus 拦截器如何工作？](answers/692-mybatis-plugin-mybatisplus-interceptor.md)
- [446. HikariCP 连接池耗尽时应检查哪些 Java 配置？](answers/446-db-connection-pool-exhaustion.md)

### Redisson 与 Kafka 客户端

- [350. Redisson 看门狗解决什么问题？](answers/350-redisson-watchdog.md)
- [362. Spring Kafka 如何决定 offset 提交时机？](answers/362-offset-commit-timing.md)
- [367. Kafka Java Producer 的幂等如何配置？](answers/367-kafka-producer-idempotence.md)
- [368. Kafka Java 事务 API 如何使用？](answers/368-kafka-transactions.md)
- [383. Spring Kafka 如何处理反序列化失败？](answers/383-message-deserialization-failure.md)

### Dubbo 与 Nacos

- [693. Dubbo 从代理调用到 Provider 执行经历什么过程？](answers/693-dubbo-invocation-pipeline.md)
- [694. Dubbo 的超时、重试和容错配置如何生效？](answers/694-dubbo-timeout-retry-loadbalance.md)
- [695. Dubbo SPI 的自适应扩展和自动激活如何工作？](answers/695-dubbo-spi-extension.md)
- [696. Nacos 服务发现的一致性、订阅和本地缓存如何工作？](answers/696-nacos-discovery-consistency.md)
- [697. Nacos Java 客户端如何接收并应用配置？](answers/697-nacos-config-push-recovery.md)

## 06 Java 工程与运行工具

### 日志与追踪上下文

- [143. Java 线程池切换时如何保留日志上下文？](answers/143-trace-id-propagation.md)
- [700. OpenTelemetry Context 和 Baggage 如何跨线程、跨服务传播？](answers/700-otel-context-baggage.md)

### 测试与构建

- [489. Java 集成测试如何接入 Testcontainers？](answers/489-testcontainers-use-cases.md)
- [505. Checkstyle、SpotBugs、PMD 分别检查什么？](answers/505-checkstyle-spotbugs-pmd.md)
- [510. Maven 如何管理和排查 Java 依赖版本？](answers/510-dependency-version-management.md)

### 打包与运行生命周期

- [459. Java 17 运行镜像有哪些特有的打包问题？](answers/459-small-secure-java-image.md)
- [467. CPU 配额如何影响 Java 17 的并行度？](answers/467-cpu-limit-impact-on-java.md)
- [470. Spring Boot 和 Java 线程池如何优雅关闭？](answers/470-graceful-shutdown-config.md)
