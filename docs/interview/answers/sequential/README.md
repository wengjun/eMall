# Java 分布式服务专家级面试逐题精讲

[返回答案手册](../README.md) | [返回分类索引](../categories/README.md) | [返回面试题库](../../question-bank.md)

本目录按题库顺序逐题讲解。每道题都尽量做到：

- 先用零基础能理解的语言解释。
- 再补充 Java 工程实践。
- 再结合分布式电商系统说明生产价值。
- 最后给出专家级面试表达和常见追问。

## 进度标记

| 序号 | 问题 | 状态 |
| --- | --- | --- |
| 001 | [Java 17 相比 Java 8 有哪些重要变化？](001-java-17-vs-java-8.md) | 已完成 |
| 002 | [`record` 适合哪些场景，不适合哪些场景？](002-record-use-cases.md) | 已完成 |
| 003 | [`var` 会不会影响可读性，团队中如何约束？](003-var-readability.md) | 已完成 |
| 004 | [`switch` 表达式相比传统 `switch` 有什么优势？](004-switch-expression.md) | 已完成 |
| 005 | [`sealed class` 适合建模哪些业务场景？](005-sealed-class.md) | 已完成 |
| 006 | [`Optional` 应该用在返回值、参数还是字段上？](006-optional-usage.md) | 已完成 |
| 007 | [为什么金额不能用 `double`？](007-money-double.md) | 已完成 |
| 008 | [`BigDecimal` 的 `equals` 和 `compareTo` 有什么区别？](008-bigdecimal-equals-compareto.md) | 已完成 |
| 009 | [Java 时间 API 中 `Instant`、`LocalDateTime`、`ZonedDateTime` 怎么选？](009-java-time-api.md) | 已完成 |
| 010 | [服务端为什么建议统一存储 UTC 时间？](010-utc-storage.md) | 已完成 |
| 011 | [枚举适合表达哪些业务状态？](011-enum-business-status.md) | 已完成 |
| 012 | [枚举状态扩展时如何保证兼容？](012-enum-compatibility.md) | 已完成 |
| 013 | [面向对象中的封装在业务系统里具体体现在哪里？](013-encapsulation.md) | 已完成 |
| 014 | [组合和继承如何取舍？](014-composition-vs-inheritance.md) | 已完成 |
| 015 | [领域对象和 DTO 为什么要分开？](015-domain-object-vs-dto.md) | 已完成 |
| 016 | [贫血模型和充血模型各有什么优缺点？](016-anemic-rich-domain.md) | 已完成 |
| 017 | [如何避免所有业务逻辑堆在 Controller？](017-avoid-fat-controller.md) | 已完成 |
| 018 | [如何设计清晰的包结构？](018-package-structure.md) | 已完成 |
| 019 | [Java 异常分为哪些类型？](019-java-exception-types.md) | 已完成 |
| 020 | [业务异常和系统异常应该如何区分？](020-business-vs-system-exception.md) | 已完成 |
| 021 | [checked exception 和 unchecked exception 如何取舍？](021-checked-vs-unchecked-exception.md) | 已完成 |
| 022 | [为什么不能直接把异常堆栈返回给前端？](022-hide-stacktrace-from-client.md) | 已完成 |
| 023 | [如何设计统一错误码？](023-error-code-design.md) | 已完成 |
| 024 | [错误码如何兼容多语言和多端？](024-error-code-i18n.md) | 已完成 |
| 025 | [泛型擦除是什么？](025-generic-type-erasure.md) | 已完成 |
| 026 | [泛型通配符 `extends` 和 `super` 怎么理解？](026-generics-extends-super.md) | 已完成 |
| 027 | [`equals` 和 `hashCode` 的契约是什么？](027-equals-hashcode-contract.md) | 已完成 |
| 028 | [为什么可变对象不适合作为 `HashMap` 的 key？](028-mutable-hashmap-key.md) | 已完成 |
| 029 | [`ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？](029-java-collections-choice.md) | 已完成 |
| 030 | [`ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？](030-concurrenthashmap-vs-hashtable.md) | 已完成 |
| 031 | [Java 反射的成本和风险是什么？](031-reflection-cost-risk.md) | 已完成 |
| 032 | [注解是如何在运行时生效的？](032-annotation-runtime.md) | 已完成 |
| 033 | [SPI 机制适合解决什么扩展问题？](033-spi-extension.md) | 已完成 |
| 034 | [如何设计一个可扩展的插件机制？](034-plugin-mechanism.md) | 已完成 |
| 035 | [为什么工程代码要重视不可变对象？](035-immutable-objects.md) | 已完成 |
| 036 | [如何判断一段代码是否可测试？](036-testable-code.md) | 已完成 |
| 037 | [如何设计稳定的公共库 API？](037-stable-public-api.md) | 已完成 |
| 038 | [公共库升级如何保证向后兼容？](038-public-library-compatibility.md) | 已完成 |
| 039 | [为什么大型项目要限制循环依赖？](039-cyclic-dependencies.md) | 已完成 |
| 040 | [如何做模块边界和依赖方向治理？](040-module-boundary-governance.md) | 已完成 |
| 041 | [JVM 内存区域包括哪些？](041-jvm-memory-areas.md) | 已完成 |
| 042 | [堆、栈、方法区、直接内存分别存什么？](042-heap-stack-metaspace-direct-memory.md) | 已完成 |
| 043 | [对象从创建到回收大致经历什么过程？](043-object-lifecycle.md) | 已完成 |
| 044 | [GC Roots 包括哪些？](044-gc-roots.md) | 已完成 |
| 045 | [Minor GC、Major GC、Full GC 有什么区别？](045-minor-major-full-gc.md) | 已完成 |
| 046 | [G1、ZGC、Shenandoah 的设计目标有什么不同？](046-g1-zgc-shenandoah.md) | 已完成 |
| 047 | [为什么低延迟服务要关注 GC 暂停？](047-low-latency-gc-pause.md) | 已完成 |
| 048 | [如何判断线上服务是否存在内存泄漏？](048-detect-memory-leak.md) | 已完成 |
| 049 | [`OutOfMemoryError` 常见类型有哪些？](049-oome-types.md) | 已完成 |
| 050 | [堆 OOM 和直接内存 OOM 如何区分？](050-heap-vs-direct-oom.md) | 已完成 |
| 051 | [线程数过多会带来什么问题？](051-too-many-threads.md) | 已完成 |
| 052 | [`jstack` 可以定位哪些问题？](052-jstack-diagnostics.md) | 已完成 |
| 053 | [`jmap`、`jcmd`、JFR 分别适合什么场景？](053-jmap-jcmd-jfr.md) | 已完成 |
| 054 | [如何分析 CPU 飙高？](054-analyze-high-cpu.md) | 已完成 |
| 055 | [如何分析接口 P99 突然升高？](055-analyze-p99-spike.md) | 已完成 |
| 056 | [如何判断是 GC、数据库、锁竞争还是下游慢导致延迟升高？](056-gc-db-lock-downstream-latency.md) | 已完成 |
| 057 | [Java 服务启动慢可能有哪些原因？](057-java-service-slow-startup.md) | 已完成 |
| 058 | [类加载机制是什么？](058-class-loading-mechanism.md) | 已完成 |
| 059 | [双亲委派模型解决什么问题？](059-parent-delegation.md) | 已完成 |
| 060 | [什么场景需要自定义 ClassLoader？](060-custom-classloader.md) | 已完成 |
| 061 | [JIT 编译是什么？](061-jit-compilation.md) | 已完成 |
| 062 | [热点代码和解释执行有什么区别？](062-hot-code-vs-interpretation.md) | 已完成 |
| 063 | [逃逸分析有什么作用？](063-escape-analysis.md) | 已完成 |
| 064 | [对象分配为什么通常很快？](064-fast-object-allocation.md) | 已完成 |
| 065 | [为什么频繁创建短生命周期对象不一定总是坏事？](065-short-lived-objects.md) | 已完成 |
| 066 | [如何减少不必要的对象分配？](066-reduce-object-allocation.md) | 已完成 |
| 067 | [如何设置生产环境 JVM 参数？](067-production-jvm-options.md) | 已完成 |
| 068 | [容器环境下 JVM 如何感知内存限制？](068-jvm-container-memory.md) | 已完成 |
| 069 | [`-Xmx` 设置过大或过小分别有什么风险？](069-xmx-too-large-or-small.md) | 已完成 |
| 070 | [线上是否应该主动调用 `System.gc()`？](070-system-gc-production.md) | 已完成 |
| 071 | [如何做 JVM 指标监控？](071-jvm-metrics-monitoring.md) | 已完成 |
| 072 | [需要重点监控哪些 JVM 指标？](072-key-jvm-metrics.md) | 已完成 |
| 073 | [GC 日志如何阅读？](073-read-gc-log.md) | 已完成 |
| 074 | [线程池队列堆积和 JVM 内存上涨有什么关系？](074-threadpool-queue-memory.md) | 已完成 |
| 075 | [如何定位死锁？](075-diagnose-deadlock.md) | 已完成 |
| 076 | [如何定位锁竞争？](076-diagnose-lock-contention.md) | 已完成 |
| 077 | [如何设计一次 Java 服务压测和性能剖析？](077-java-loadtest-profiling.md) | 已完成 |
| 078 | [线程和进程有什么区别？](078-thread-vs-process.md) | 已完成 |
| 079 | [Java 线程状态有哪些？](079-java-thread-states.md) | 已完成 |
| 080 | [`synchronized` 的原理是什么？](080-synchronized-principle.md) | 已完成 |
| 081 | [偏向锁、轻量级锁、重量级锁是什么？](081-lock-upgrade.md) | 已完成 |
| 082 | [`ReentrantLock` 和 `synchronized` 怎么选？](082-reentrantlock-vs-synchronized.md) | 已完成 |
| 083 | [公平锁和非公平锁有什么区别？](083-fair-vs-nonfair-lock.md) | 已完成 |
| 084 | [`volatile` 解决什么问题，不能解决什么问题？](084-volatile.md) | 已完成 |
| 085 | [happens-before 规则是什么？](085-happens-before.md) | 已完成 |
| 086 | [Java 内存模型解决什么问题？](086-java-memory-model.md) | 已完成 |
| 087 | [CAS 是什么？](087-cas.md) | 已完成 |
| 088 | [ABA 问题是什么，如何解决？](088-aba-problem.md) | 已完成 |
| 089 | [`AtomicInteger` 和 `LongAdder` 如何取舍？](089-atomicinteger-vs-longadder.md) | 已完成 |
| 090 | [`CountDownLatch`、`CyclicBarrier`、`Semaphore` 分别适合什么场景？](090-latch-barrier-semaphore.md) | 已完成 |
| 091 | [`CompletableFuture` 如何处理异步编排？](091-completablefuture-async-composition.md) | 已完成 |
| 092 | [`CompletableFuture` 默认线程池有什么风险？](092-completablefuture-default-pool-risk.md) | 已完成 |
| 093 | [为什么生产代码不能随意使用公共 ForkJoinPool？](093-avoid-common-forkjoinpool.md) | 已完成 |
| 094 | [线程池核心参数如何设置？](094-threadpool-core-parameters.md) | 已完成 |
| 095 | [CPU 密集型和 IO 密集型线程池如何估算大小？](095-cpu-io-threadpool-size.md) | 已完成 |
| 096 | [线程池队列应该用有界还是无界？](096-bounded-vs-unbounded-queue.md) | 已完成 |
| 097 | [拒绝策略怎么选？](097-rejection-policy.md) | 已完成 |
| 098 | [如何避免线程池雪崩？](098-avoid-threadpool-avalanche.md) | 已完成 |
| 099 | [多个下游服务是否应该共享同一个线程池？](099-share-threadpool-downstreams.md) | 已完成 |
| 100 | [什么是线程池隔离？](100-threadpool-isolation.md) | 已完成 |
| 101 | [什么是舱壁隔离？](101-bulkhead-isolation.md) | 已完成 |
| 102 | [任务超时后线程是否真的停止？](102-timeout-does-not-stop-thread.md) | 已完成 |
| 103 | [Java 中断机制如何正确使用？](103-java-interruption.md) | 已完成 |
| 104 | [如何设计可取消的异步任务？](104-cancellable-async-task.md) | 已完成 |
| 105 | [如何避免死锁？](105-avoid-deadlock.md) | 已完成 |
| 106 | [如何减少锁粒度？](106-reduce-lock-granularity.md) | 已完成 |
| 107 | [分段锁和库存桶有什么相似点？](107-segment-lock-and-stock-bucket.md) | 已完成 |
| 108 | [单机锁为什么不能解决多实例并发？](108-local-lock-not-for-multi-instance.md) | 已完成 |
| 109 | [分布式锁适合哪些场景？](109-distributed-lock-use-cases.md) | 已完成 |
| 110 | [分布式锁有哪些风险？](110-distributed-lock-risks.md) | 已完成 |
| 111 | [Redlock 争议是什么？](111-redlock-controversy.md) | 已完成 |
| 112 | [为什么数据库唯一键通常比分布式锁更可靠？](112-db-unique-key-vs-distributed-lock.md) | 已完成 |
| 113 | [并发下如何实现只执行一次？](113-execute-once-concurrency.md) | 已完成 |
| 114 | [如何设计幂等和并发安全的组合方案？](114-idempotency-and-concurrency.md) | 已完成 |
| 115 | [Spring Boot 自动配置原理是什么？](115-spring-boot-auto-configuration.md) | 已完成 |
| 116 | [`@SpringBootApplication` 包含哪些注解？](116-springbootapplication.md) | 已完成 |
| 117 | [Bean 的生命周期是什么？](117-spring-bean-lifecycle.md) | 已完成 |
| 118 | [构造函数注入、字段注入、Setter 注入如何取舍？](118-injection-styles.md) | 已完成 |
| 119 | [为什么推荐构造函数注入？](119-why-constructor-injection.md) | 已完成 |
| 120 | [Spring AOP 的代理机制是什么？](120-spring-aop-proxy.md) | 已完成 |
| 121 | [JDK 动态代理和 CGLIB 有什么区别？](121-jdk-proxy-vs-cglib.md) | 已完成 |
| 122 | [`@Transactional` 为什么有时不生效？](122-transactional-not-effective.md) | 已完成 |
| 123 | [自调用为什么绕过事务代理？](123-self-invocation-bypass-transaction.md) | 已完成 |
| 124 | [事务传播行为有哪些？](124-transaction-propagation.md) | 已完成 |
| 125 | [`REQUIRED`、`REQUIRES_NEW`、`NESTED` 有什么区别？](125-required-requires-new-nested.md) | 已完成 |
| 126 | [事务隔离级别如何配置？](126-transaction-isolation-config.md) | 已完成 |
| 127 | [事务里调用远程服务有什么风险？](127-remote-call-in-transaction-risk.md) | 已完成 |
| 128 | [Controller、Service、Repository 的职责边界是什么？](128-controller-service-repository-boundary.md) | 已完成 |
| 129 | [`@ControllerAdvice` 如何做统一异常处理？](129-controller-advice.md) | 已完成 |
| 130 | [Bean Validation 适合做哪些校验？](130-bean-validation.md) | 已完成 |
| 131 | [参数校验和业务校验如何区分？](131-parameter-vs-business-validation.md) | 已完成 |
| 132 | [Spring 配置加载优先级是什么？](132-spring-config-priority.md) | 已完成 |
| 133 | [profile、环境变量、配置中心如何配合？](133-profile-env-config-center.md) | 已完成 |
| 134 | [如何安全管理生产密钥？](134-production-secret-management.md) | 已完成 |
| 135 | [Actuator 暴露哪些端点比较合理？](135-actuator-endpoints.md) | 已完成 |
| 136 | [健康检查应该包含哪些内容？](136-health-check-design.md) | 已完成 |
| 137 | [readiness 和 liveness 在 Spring 中如何实现？](137-readiness-liveness-spring.md) | 已完成 |
| 138 | [RestClient、WebClient、Feign 如何取舍？](138-restclient-webclient-feign.md) | 已完成 |
| 139 | [阻塞式和响应式调用如何取舍？](139-blocking-vs-reactive.md) | 已完成 |
| 140 | [WebFlux 是否一定比 MVC 性能更高？](140-webflux-vs-mvc-performance.md) | 已完成 |
| 141 | [如何设置 HTTP 客户端连接池？](141-http-client-connection-pool.md) | 已完成 |
| 142 | [如何设置连接超时、读取超时和总超时？](142-http-timeouts.md) | 已完成 |
| 143 | [如何透传 trace ID？](143-trace-id-propagation.md) | 已完成 |
| 144 | [如何设计统一的内部服务调用规范？](144-internal-service-call-standard.md) | 已完成 |
| 145 | [Spring Cache 的使用边界是什么？](145-spring-cache-boundary.md) | 已完成 |
| 146 | [Spring 事件和 MQ 事件有什么区别？](146-spring-event-vs-mq.md) | 已完成 |
| 147 | [如何避免 Bean 循环依赖？](147-avoid-bean-circular-dependency.md) | 已完成 |
| 148 | [如何设计 starter 或 auto-configuration？](148-design-starter-auto-configuration.md) | 已完成 |
| 149 | [如何在多模块项目中复用公共配置？](149-reuse-common-config-in-multi-module.md) | 已完成 |
| 150 | [REST API 的资源建模原则是什么？](150-rest-resource-modeling.md) | 已完成 |
| 151 | [`GET`、`POST`、`PUT`、`PATCH`、`DELETE` 应该如何使用？](151-http-method-semantics.md) | 已完成 |
| 152 | [哪些接口应该设计成幂等？](152-idempotent-apis.md) | 已完成 |
| 153 | [幂等键应该放 Header 还是 Body？](153-idempotency-key-header-or-body.md) | 已完成 |
| 154 | [API 版本如何设计？](154-api-versioning.md) | 已完成 |
| 155 | [什么时候需要新版本 API？](155-when-new-api-version.md) | 已完成 |
| 156 | [如何设计统一响应体？](156-unified-response-body.md) | 已完成 |
| 157 | [错误码和 HTTP 状态码如何配合？](157-error-code-vs-http-status.md) | 已完成 |
| 158 | [分页接口如何设计？](158-pagination-api.md) | 已完成 |
| 159 | [如何防止批量查询拖垮系统？](159-prevent-bulk-query-overload.md) | 已完成 |
| 160 | [API 如何保证向后兼容？](160-api-backward-compatibility.md) | 已完成 |
| 161 | [如何做字段废弃和迁移？](161-field-deprecation-migration.md) | 已完成 |
| 162 | [如何设计开放 API 签名？](162-open-api-signature.md) | 已完成 |
| 163 | [如何防止重放攻击？](163-prevent-replay-attack.md) | 已完成 |
| 164 | [如何做接口限流？](164-api-rate-limiting.md) | 已完成 |
| 165 | [如何设计 API 网关层职责？](165-api-gateway-responsibility.md) | 已完成 |
| 166 | [网关和业务服务分别做什么校验？](166-gateway-vs-service-validation.md) | 已完成 |
| 167 | [如何设计 BFF？](167-bff-design.md) | 已完成 |
| 168 | [移动端和 PC 端 API 是否应该完全共用？](168-mobile-pc-api-sharing.md) | 已完成 |
| 169 | [如何设计内部 API 和外部 API 的边界？](169-internal-vs-external-api.md) | 已完成 |
| 170 | [如何写 API 文档和契约测试？](170-api-docs-contract-tests.md) | 已完成 |
| 171 | [深分页有什么问题？](171-deep-pagination-risk.md) | 已完成 |
| 172 | [游标分页和 offset 分页如何取舍？](172-cursor-vs-offset-pagination.md) | 已完成 |
| 173 | [查询接口如何防止过度复杂？](173-query-api-complexity-control.md) | 已完成 |
| 174 | [批量接口如何设计部分成功？](174-bulk-api-partial-success.md) | 已完成 |
| 175 | [文件上传接口如何做安全限制？](175-file-upload-api-security.md) | 已完成 |
| 176 | [如何做 API 兼容性测试？](176-api-compatibility-tests.md) | 已完成 |
| 177 | [如何设计接口超时预算？](177-api-timeout-budget.md) | 已完成 |
| 178 | [如何处理客户端重试导致的重复请求？](178-client-retry-duplicate-request.md) | 已完成 |
| 179 | [如何设计错误信息，既便于排障又不泄露内部实现？](179-safe-error-message-design.md) | 已完成 |
| 180 | [GraphQL 和 REST 如何取舍？](180-graphql-vs-rest.md) | 已完成 |
| 181 | [gRPC 和 HTTP JSON 如何取舍？](181-grpc-vs-http-json.md) | 已完成 |
| 182 | [如何设计跨服务错误码映射？](182-cross-service-error-code-mapping.md) | 已完成 |
| 183 | [如何治理废弃 API？](183-deprecated-api-governance.md) | 已完成 |
| 184 | [什么情况下应该拆微服务？](184-when-to-split-microservices.md) | 已完成 |
| 185 | [什么情况下不应该拆微服务？](185-when-not-to-split-microservices.md) | 已完成 |
| 186 | [微服务和模块化单体如何取舍？](186-microservice-vs-modular-monolith.md) | 已完成 |
| 187 | [服务边界应该按业务域还是技术层拆？](187-service-boundary-business-vs-technical.md) | 已完成 |
| 188 | [什么是数据所有权？](188-data-ownership.md) | 已完成 |
| 189 | [为什么服务之间不应该共享数据库表？](189-no-shared-database-tables.md) | 已完成 |
| 190 | [拆库后跨服务查询怎么做？](190-cross-service-query.md) | 已完成 |
| 191 | [跨服务事务怎么处理？](191-cross-service-transaction.md) | 已完成 |
| 192 | [同步调用和异步事件如何取舍？](192-sync-vs-async-events.md) | 已完成 |
| 193 | [服务调用链过长有什么风险？](193-long-service-call-chain-risk.md) | 已完成 |
| 194 | [如何避免分布式单体？](194-avoid-distributed-monolith.md) | 已完成 |
| 195 | [如何识别错误的服务边界？](195-identify-wrong-service-boundary.md) | 已完成 |
| 196 | [如何设计订单、库存、支付的边界？](196-order-inventory-payment-boundary.md) | 已完成 |
| 197 | [搜索、推荐、广告为什么通常不放在交易主链路？](197-search-recommendation-ads-not-main-path.md) | 已完成 |
| 198 | [如何处理跨团队服务契约？](198-cross-team-service-contract.md) | 已完成 |
| 199 | [如何做服务依赖治理？](199-service-dependency-governance.md) | 已完成 |
| 200 | [如何画服务依赖图？](200-service-dependency-graph.md) | 已完成 |
| 201 | [如何评估一个服务的故障半径？](201-service-blast-radius.md) | 已完成 |
| 202 | [如何做服务分级？](202-service-tiering.md) | 已完成 |
| 203 | [核心链路和非核心链路如何隔离？](203-core-noncore-isolation.md) | 已完成 |
| 204 | [如何设计内部运维接口？](204-internal-ops-api-design.md) | 已完成 |
| 205 | [运维接口为什么不能暴露公网？](205-ops-api-not-public.md) | 已完成 |
| 206 | [如何做多租户或商家隔离？](206-multitenancy-merchant-isolation.md) | 已完成 |
| 207 | [如何做配置治理？](207-configuration-governance.md) | 已完成 |
| 208 | [如何做灰度开关和功能开关？](208-feature-flag-gray-switch.md) | 已完成 |
| 209 | [配置中心故障会带来什么影响？](209-config-center-failure-impact.md) | 已完成 |
| 210 | [如何处理配置变更回滚？](210-config-change-rollback.md) | 已完成 |
| 211 | [为什么大型分布式系统通常不用全局大事务？](211-avoid-global-distributed-transaction.md) | 已完成 |
| 212 | [2PC 的流程是什么？](212-two-phase-commit-flow.md) | 已完成 |
| 213 | [2PC 有哪些可用性问题？](213-two-phase-commit-availability-issues.md) | 已完成 |
| 214 | [3PC 解决了什么，又有什么限制？](214-three-phase-commit-limits.md) | 已完成 |
| 215 | [TCC 适合哪些场景？](215-tcc-scenarios.md) | 已完成 |
| 216 | [Saga 适合哪些场景？](216-saga-scenarios.md) | 已完成 |
| 217 | [本地事务加 Outbox 解决什么问题？](217-local-transaction-outbox.md) | 已完成 |
| 218 | [Outbox 不能解决什么问题？](218-outbox-limitations.md) | 已完成 |
| 219 | [业务数据写成功但 MQ 发送失败怎么办？](219-business-success-mq-send-failed.md) | 已完成 |
| 220 | [MQ 发送成功但业务事务回滚怎么办？](220-mq-sent-business-rollback.md) | 已完成 |
| 221 | [消息重复投递怎么办？](221-duplicate-message-delivery.md) | 已完成 |
| 222 | [消费成功但 ack 失败怎么办？](222-consume-success-ack-failed.md) | 已完成 |
| 223 | [消费失败后如何重试？](223-consume-failure-retry.md) | 已完成 |
| 224 | [重试多次失败如何进入死信？](224-dead-letter-after-retry.md) | 已完成 |
| 225 | [死信消息如何恢复？](225-dead-letter-recovery.md) | 已完成 |
| 226 | [如何保证同一订单事件顺序？](226-order-event-ordering.md) | 已完成 |
| 227 | [如何处理跨 partition 乱序？](227-cross-partition-out-of-order.md) | 已完成 |
| 228 | [如何设计事件版本？](228-event-versioning.md) | 已完成 |
| 229 | [如何保证事件 schema 兼容？](229-event-schema-compatibility.md) | 已完成 |
| 230 | [最终一致和强一致如何取舍？](230-eventual-vs-strong-consistency.md) | 已完成 |
| 231 | [用户看到中间状态是否可接受？](231-intermediate-state-user-visible.md) | 已完成 |
| 232 | [下单后库存预占失败怎么办？](232-stock-reservation-failed-after-order.md) | 已完成 |
| 233 | [支付成功但订单更新失败怎么办？](233-payment-success-order-update-failed.md) | 已完成 |
| 234 | [订单取消但库存释放失败怎么办？](234-order-cancel-stock-release-failed.md) | 已完成 |
| 235 | [补偿任务如何避免重复执行？](235-compensation-idempotency.md) | 已完成 |
| 236 | [补偿任务如何避免多实例并发执行？](236-compensation-concurrency-control.md) | 已完成 |
| 237 | [对账和补偿有什么区别？](237-reconciliation-vs-compensation.md) | 已完成 |
| 238 | [对账发现差异后如何处理？](238-reconciliation-difference-handling.md) | 已完成 |
| 239 | [幂等和去重有什么区别？](239-idempotency-vs-deduplication.md) | 已完成 |
| 240 | [幂等记录保留多久？](240-idempotency-record-retention.md) | 已完成 |
| 241 | [幂等表无限增长怎么办？](241-idempotency-table-growth.md) | 已完成 |
| 242 | [状态机如何防止非法状态跳转？](242-state-machine-illegal-transition.md) | 已完成 |
| 243 | [如何设计可恢复的业务状态？](243-recoverable-business-state.md) | 已完成 |
| 244 | [如何处理悬挂、空回滚和重复提交？](244-hanging-empty-rollback-duplicate-submit.md) | 已完成 |
| 245 | [如何证明一致性方案是可靠的？](245-prove-consistency-scheme-reliable.md) | 已完成 |
| 246 | [高并发系统的瓶颈通常在哪里？](246-high-concurrency-bottlenecks.md) | 已完成 |
| 247 | [QPS、TPS、并发数、响应时间之间是什么关系？](247-qps-tps-concurrency-latency.md) | 已完成 |
| 248 | [如何做容量估算？](248-capacity-estimation.md) | 已完成 |
| 249 | [如何估算线程池、连接池和数据库连接数？](249-pool-size-estimation.md) | 已完成 |
| 250 | [如何设计限流？](250-rate-limiting-design.md) | 已完成 |
| 251 | [什么是熔断？](251-circuit-breaker.md) | 已完成 |
| 252 | [熔断的 CLOSED、OPEN、HALF_OPEN 如何切换？](252-circuit-breaker-states.md) | 已完成 |
| 253 | [半开状态为什么要小流量探测？](253-half-open-probing.md) | 已完成 |
| 254 | [熔断和限流有什么区别？](254-circuit-breaker-vs-rate-limit.md) | 已完成 |
| 255 | [熔断和降级有什么区别？](255-circuit-breaker-vs-degradation.md) | 已完成 |
| 256 | [哪些业务可以降级？](256-degradable-business.md) | 已完成 |
| 257 | [哪些业务不能降级？](257-non-degradable-business.md) | 已完成 |
| 258 | [推荐服务故障如何降级？](258-recommendation-failure-degradation.md) | 已完成 |
| 259 | [价格服务故障能不能降级？](259-price-service-failure-degradation.md) | 已完成 |
| 260 | [库存服务故障能不能继续下单？](260-inventory-failure-continue-order.md) | 已完成 |
| 261 | [如何设置超时？](261-timeout-design.md) | 已完成 |
| 262 | [如何设计重试？](262-retry-design.md) | 已完成 |
| 263 | [为什么重试可能放大故障？](263-retry-amplifies-failure.md) | 已完成 |
| 264 | [什么是指数退避和 jitter？](264-exponential-backoff-jitter.md) | 已完成 |
| 265 | [如何做请求合并？](265-request-coalescing.md) | 已完成 |
| 266 | [如何做热点隔离？](266-hotspot-isolation.md) | 已完成 |
| 267 | [热点 SKU 为什么会拖垮数据库？](267-hot-sku-database-risk.md) | 已完成 |
| 268 | [库存桶如何降低热点行竞争？](268-inventory-buckets.md) | 已完成 |
| 269 | [秒杀系统为什么要削峰？](269-flash-sale-traffic-shaping.md) | 已完成 |
| 270 | [秒杀令牌如何设计？](270-flash-sale-token-design.md) | 已完成 |
| 271 | [秒杀如何防止超卖？](271-flash-sale-oversell-prevention.md) | 已完成 |
| 272 | [秒杀如何防止黄牛？](272-flash-sale-bot-prevention.md) | 已完成 |
| 273 | [如何保护普通下单不被秒杀拖垮？](273-protect-normal-order-from-flash-sale.md) | 已完成 |
| 274 | [如何做优雅关闭？](274-graceful-shutdown.md) | 已完成 |
| 275 | [服务关闭时如何处理正在执行的请求？](275-inflight-request-on-shutdown.md) | 已完成 |
| 276 | [如何设计下游自动平滑恢复？](276-downstream-smooth-recovery.md) | 已完成 |
| 277 | [下游恢复后为什么不能立刻放开全部流量？](277-why-not-full-traffic-after-recovery.md) | 已完成 |
| 278 | [如何做预热？](278-warmup-design.md) | 已完成 |
| 279 | [如何做过载保护？](279-overload-protection.md) | 已完成 |
| 280 | [如何判断系统已经进入过载？](280-overload-detection.md) | 已完成 |
| 281 | [关系型数据库适合解决什么问题？](281-relational-database-use-cases.md) | 已完成 |
| 282 | [MySQL InnoDB 的索引结构是什么？](282-innodb-index-structure.md) | 已完成 |
| 283 | [B+Tree 为什么适合数据库索引？](283-bplus-tree-database-index.md) | 已完成 |
| 284 | [聚簇索引和二级索引有什么区别？](284-clustered-vs-secondary-index.md) | 已完成 |
| 285 | [回表是什么？](285-bookmark-lookup.md) | 已完成 |
| 286 | [覆盖索引是什么？](286-covering-index.md) | 已完成 |
| 287 | [联合索引最左前缀是什么？](287-leftmost-prefix.md) | 已完成 |
| 288 | [索引下推是什么？](288-index-condition-pushdown.md) | 已完成 |
| 289 | [什么情况下索引会失效？](289-index-invalid-cases.md) | 已完成 |
| 290 | [慢 SQL 如何分析？](290-slow-sql-analysis.md) | 已完成 |
| 291 | [`explain` 重点看哪些字段？](291-explain-key-fields.md) | 已完成 |
| 292 | [选择性低的字段是否适合建索引？](292-low-cardinality-index.md) | 已完成 |
| 293 | [索引越多为什么写入越慢？](293-too-many-indexes-slow-writes.md) | 已完成 |
| 294 | [如何设计订单表索引？](294-order-table-index-design.md) | 已完成 |
| 295 | [如何设计支付表唯一约束？](295-payment-table-unique-constraints.md) | 已完成 |
| 296 | [如何设计 Outbox 扫描索引？](296-outbox-scan-index-design.md) | 已完成 |
| 297 | [事务 ACID 分别是什么？](297-acid.md) | 已完成 |
| 298 | [脏读、不可重复读、幻读是什么？](298-dirty-nonrepeatable-phantom-read.md) | 已完成 |
| 299 | [MVCC 是什么？](299-mvcc.md) | 已完成 |
| 300 | [快照读和当前读有什么区别？](300-snapshot-read-vs-current-read.md) | 已完成 |
| 301 | [间隙锁是什么？](301-gap-lock.md) | 已完成 |
| 302 | [死锁如何产生？](302-deadlock-causes.md) | 已完成 |
| 303 | [如何分析和避免数据库死锁？](303-analyze-avoid-db-deadlock.md) | 已完成 |
| 304 | [悲观锁和乐观锁怎么选？](304-pessimistic-vs-optimistic-lock.md) | 已完成 |
| 305 | [条件更新如何防止库存超卖？](305-conditional-update-prevent-oversell.md) | 已完成 |
| 306 | [`select for update` 有什么风险？](306-select-for-update-risks.md) | 已完成 |
| 307 | [如何设计分库分表？](307-sharding-design.md) | 已完成 |
| 308 | [分片键如何选择？](308-sharding-key-selection.md) | 已完成 |
| 309 | [按用户分片和按订单分片有什么区别？](309-user-vs-order-sharding.md) | 已完成 |
| 310 | [分库分表后如何按用户查订单？](310-query-orders-by-user-after-sharding.md) | 已完成 |
| 311 | [分库分表后如何按订单号查订单？](311-query-order-by-order-no-after-sharding.md) | 已完成 |
| 312 | [全局唯一 ID 如何生成？](312-global-unique-id.md) | 已完成 |
| 313 | [Snowflake ID 的结构和风险是什么？](313-snowflake-id.md) | 已完成 |
| 314 | [时钟回拨如何处理？](314-clock-backward-handling.md) | 已完成 |
| 315 | [数据归档如何设计？](315-data-archiving-design.md) | 已完成 |
| 316 | [冷热分离如何设计？](316-hot-cold-data-separation.md) | 已完成 |
| 317 | [在线 DDL 有什么风险？](317-online-ddl-risks.md) | 已完成 |
| 318 | [数据库扩容如何做？](318-database-capacity-expansion.md) | 已完成 |
| 319 | [读写分离有什么一致性问题？](319-read-write-splitting-consistency.md) | 已完成 |
| 320 | [主从延迟如何影响业务？](320-replication-lag-business-impact.md) | 已完成 |
| 321 | [如何做数据库备份和恢复演练？](321-database-backup-restore-drill.md) | 已完成 |
| 322 | [ORM 能解决哪些问题，不能解决哪些问题？](322-orm-boundaries.md) | 已完成 |
| 323 | [MyBatis Plus 和手写 SQL 如何取舍？](323-mybatis-plus-vs-handwritten-sql.md) | 已完成 |
| 324 | [Redis 常用数据结构有哪些？](324-redis-data-structures.md) | 已完成 |
| 325 | [String、Hash、List、Set、ZSet 分别适合什么场景？](325-redis-structure-use-cases.md) | 已完成 |
| 326 | [Redis 单线程为什么还能很快？](326-redis-single-thread-fast.md) | 已完成 |
| 327 | [Redis IO 多路复用是什么？](327-redis-io-multiplexing.md) | 已完成 |
| 328 | [Redis 持久化 RDB 和 AOF 有什么区别？](328-redis-rdb-vs-aof.md) | 已完成 |
| 329 | [Redis 主从复制如何工作？](329-redis-replication.md) | 已完成 |
| 330 | [Redis Sentinel 解决什么问题？](330-redis-sentinel.md) | 已完成 |
| 331 | [Redis Cluster 如何分片？](331-redis-cluster-sharding.md) | 已完成 |
| 332 | [一致性哈希和 Redis Cluster slot 有什么区别？](332-consistent-hash-vs-redis-slot.md) | 已完成 |
| 333 | [缓存穿透是什么？](333-cache-penetration.md) | 已完成 |
| 334 | [缓存击穿是什么？](334-cache-breakdown.md) | 已完成 |
| 335 | [缓存雪崩是什么？](335-cache-avalanche.md) | 已完成 |
| 336 | [如何设计商品详情缓存？](336-product-detail-cache-design.md) | 已完成 |
| 337 | [缓存 key 如何命名？](337-cache-key-naming.md) | 已完成 |
| 338 | [TTL 如何设置？](338-cache-ttl-design.md) | 已完成 |
| 339 | [TTL 为什么要加随机抖动？](339-cache-ttl-jitter.md) | 已完成 |
| 340 | [更新数据库和删除缓存顺序怎么选？](340-cache-db-update-order.md) | 已完成 |
| 341 | [延迟双删解决什么问题？](341-delayed-double-delete.md) | 已完成 |
| 342 | [缓存和数据库短暂不一致如何接受？](342-cache-db-temporary-inconsistency.md) | 已完成 |
| 343 | [热 key 如何发现？](343-hot-key-discovery.md) | 已完成 |
| 344 | [热 key 如何治理？](344-hot-key-governance.md) | 已完成 |
| 345 | [大 key 有什么危害？](345-big-key-risks.md) | 已完成 |
| 346 | [如何拆分大 key？](346-big-key-splitting.md) | 已完成 |
| 347 | [Redis 分布式锁如何实现？](347-redis-distributed-lock.md) | 已完成 |
| 348 | [`SET NX PX` 有什么注意点？](348-set-nx-px-notes.md) | 已完成 |
| 349 | [Lua 脚本为什么能保证原子性？](349-lua-atomicity.md) | 已完成 |
| 350 | [Redisson 看门狗解决什么问题？](350-redisson-watchdog.md) | 已完成 |
| 351 | [Redis 限流如何实现？](351-redis-rate-limiting.md) | 已完成 |
| 352 | [Redis 计数器如何避免过期窗口问题？](352-redis-counter-expiry-window.md) | 已完成 |
| 353 | [布隆过滤器适合什么场景？](353-bloom-filter-use-cases.md) | 已完成 |
| 354 | [Redis 故障时系统如何降级？](354-redis-failure-degradation.md) | 已完成 |
| 355 | [缓存命中率下降如何排查？](355-cache-hit-rate-drop.md) | 已完成 |
| 356 | [Redis 内存淘汰策略有哪些？](356-redis-eviction-policies.md) | 已完成 |
| 357 | [Redis 是否适合作为订单状态最终数据源？](357-redis-as-order-source.md) | 已完成 |
| 358 | [Kafka 的 Topic、Partition、Replica、Broker 是什么？](358-kafka-topic-partition-replica-broker.md) | 已完成 |
| 359 | [Producer 如何选择 partition？](359-kafka-producer-partition-selection.md) | 已完成 |
| 360 | [Consumer Group 如何工作？](360-kafka-consumer-group.md) | 已完成 |
| 361 | [offset 是什么？](361-kafka-offset.md) | 已完成 |
| 362 | [消息提交 offset 的时机如何选择？](362-offset-commit-timing.md) | 已完成 |
| 363 | [至少一次、至多一次、恰好一次分别是什么意思？](363-kafka-delivery-semantics.md) | 已完成 |
| 364 | [Kafka 的 exactly-once 为什么不等于业务 exactly-once？](364-kafka-eos-vs-business-eos.md) | 已完成 |
| 365 | [ISR 是什么？](365-kafka-isr.md) | 已完成 |
| 366 | [ack=0、ack=1、ack=all 有什么区别？](366-kafka-ack-modes.md) | 已完成 |
| 367 | [Producer 幂等解决什么问题？](367-kafka-producer-idempotence.md) | 已完成 |
| 368 | [Kafka 事务适合什么场景？](368-kafka-transactions.md) | 已完成 |
| 369 | [消息顺序如何保证？](369-message-ordering.md) | 已完成 |
| 370 | [为什么顺序通常只能保证同一个 partition 内？](370-ordering-within-partition.md) | 已完成 |
| 371 | [如何设计订单事件 topic？](371-order-event-topic-design.md) | 已完成 |
| 372 | [事件应该携带全量数据还是只携带 ID？](372-event-full-data-or-id.md) | 已完成 |
| 373 | [消息体过大有什么问题？](373-large-message-problems.md) | 已完成 |
| 374 | [消费者处理慢怎么办？](374-slow-consumer-handling.md) | 已完成 |
| 375 | [consumer lag 如何监控？](375-consumer-lag-monitoring.md) | 已完成 |
| 376 | [消息积压如何排查？](376-message-backlog-troubleshooting.md) | 已完成 |
| 377 | [增加消费者为什么不一定能解决积压？](377-adding-consumers-not-enough.md) | 已完成 |
| 378 | [单分区热点如何处理？](378-hot-partition-handling.md) | 已完成 |
| 379 | [重试 topic 和死信 topic 如何设计？](379-retry-and-dead-letter-topic.md) | 已完成 |
| 380 | [消费者幂等表如何设计？](380-consumer-idempotency-table.md) | 已完成 |
| 381 | [消费端如何保证业务写入和去重记录原子性？](381-consumer-write-dedup-atomicity.md) | 已完成 |
| 382 | [消息 schema 如何演进？](382-message-schema-evolution.md) | 已完成 |
| 383 | [如何处理消息反序列化失败？](383-message-deserialization-failure.md) | 已完成 |
| 384 | [Kafka 和 RabbitMQ 如何取舍？](384-kafka-vs-rabbitmq.md) | 已完成 |
| 385 | [MQ 故障时核心链路怎么办？](385-mq-failure-core-flow.md) | 已完成 |
| 386 | [Outbox Relay 多实例如何避免重复抢事件？](386-outbox-relay-multi-instance.md) | 已完成 |
| 387 | [Outbox 历史数据如何清理？](387-outbox-history-cleanup.md) | 已完成 |
| 388 | [为什么电商搜索通常不用 MySQL 直接实现？](388-why-not-mysql-search.md) | 已完成 |
| 389 | [倒排索引是什么？](389-inverted-index.md) | 已完成 |
| 390 | [分词器如何影响搜索结果？](390-tokenizer-impact.md) | 已完成 |
| 391 | [商品搜索文档应该包含哪些字段？](391-product-search-document-fields.md) | 已完成 |
| 392 | [商品上下架如何同步到搜索索引？](392-product-status-sync-to-search.md) | 已完成 |
| 393 | [搜索索引和商品库不一致怎么办？](393-search-index-inconsistency.md) | 已完成 |
| 394 | [搜索读模型可以重建吗？](394-search-read-model-rebuildable.md) | 已完成 |
| 395 | [如何设计索引重建流程？](395-index-rebuild-flow.md) | 已完成 |
| 396 | [OpenSearch 分片和副本如何设置？](396-opensearch-shards-replicas.md) | 已完成 |
| 397 | [搜索结果排序考虑哪些因素？](397-search-ranking-factors.md) | 已完成 |
| 398 | [相关性、销量、价格、广告如何混排？](398-relevance-sales-price-ads-mix.md) | 已完成 |
| 399 | [搜索服务故障如何降级？](399-search-degradation.md) | 已完成 |
| 400 | [搜索延迟升高如何排查？](400-search-latency-troubleshooting.md) | 已完成 |
| 401 | [商品详情缓存和搜索索引是什么关系？](401-product-cache-vs-search-index.md) | 已完成 |
| 402 | [CQRS 在电商系统中如何体现？](402-cqrs-in-ecommerce.md) | 已完成 |
| 403 | [读模型最终一致对用户有什么影响？](403-read-model-eventual-consistency-impact.md) | 已完成 |
| 404 | [认证和授权有什么区别？](404-authentication-vs-authorization.md) | 已完成 |
| 405 | [Session、JWT、OAuth2 如何取舍？](405-session-jwt-oauth2-tradeoff.md) | 已完成 |
| 406 | [JWT 有什么风险？](406-jwt-risks.md) | 已完成 |
| 407 | [token 泄露如何处理？](407-token-leakage-handling.md) | 已完成 |
| 408 | [RBAC 和 ABAC 有什么区别？](408-rbac-vs-abac.md) | 已完成 |
| 409 | [最小权限原则如何落地？](409-least-privilege.md) | 已完成 |
| 410 | [内部运维接口如何鉴权？](410-internal-ops-auth.md) | 已完成 |
| 411 | [HMAC 签名如何防篡改？](411-hmac-signature-tamper-proof.md) | 已完成 |
| 412 | [nonce 如何防重放？](412-nonce-anti-replay.md) | 已完成 |
| 413 | [timestamp 窗口如何设置？](413-timestamp-window.md) | 已完成 |
| 414 | [签名为什么要覆盖 method、path、body hash？](414-sign-method-path-body-hash.md) | 已完成 |
| 415 | [常量时间比较解决什么问题？](415-constant-time-comparison.md) | 已完成 |
| 416 | [AES-GCM 适合什么场景？](416-aes-gcm-use-cases.md) | 已完成 |
| 417 | [HMAC hash 为什么适合精确查询？](417-hmac-hash-exact-query.md) | 已完成 |
| 418 | [手机号为什么不能明文存储？](418-phone-not-plaintext.md) | 已完成 |
| 419 | [加密字段如何做密钥轮换？](419-encrypted-field-key-rotation.md) | 已完成 |
| 420 | [日志脱敏如何实现？](420-log-masking.md) | 已完成 |
| 421 | [SQL 注入如何防止？](421-sql-injection-prevention.md) | 已完成 |
| 422 | [XSS 和 CSRF 在前后端系统中如何防护？](422-xss-csrf-protection.md) | 已完成 |
| 423 | [SSRF 风险在哪里？](423-ssrf-risk.md) | 已完成 |
| 424 | [开放平台如何做 appKey 和 secret 管理？](424-appkey-secret-management.md) | 已完成 |
| 425 | [如何做接口权限审计？](425-api-permission-audit.md) | 已完成 |
| 426 | [风控规则如何灰度上线？](426-risk-rule-gray-release.md) | 已完成 |
| 427 | [账号冻结对下单链路有什么影响？](427-account-freeze-order-impact.md) | 已完成 |
| 428 | [支付风控应在什么环节介入？](428-payment-risk-intervention.md) | 已完成 |
| 429 | [如何设计高危操作双人审批？](429-high-risk-dual-approval.md) | 已完成 |
| 430 | [如何处理数据合规删除和历史订单保留？](430-compliance-delete-vs-order-retention.md) | 已完成 |
| 431 | [日志、指标、Trace 分别解决什么问题？](431-logs-metrics-traces.md) | 已完成 |
| 432 | [为什么三者都需要？](432-why-need-logs-metrics-traces.md) | 已完成 |
| 433 | [trace ID 如何生成和透传？](433-trace-id-generation-propagation.md) | 已完成 |
| 434 | [日志中必须包含哪些业务字段？](434-log-business-fields.md) | 已完成 |
| 435 | [结构化日志有什么好处？](435-structured-logging-benefits.md) | 已完成 |
| 436 | [什么信息不能写入日志？](436-what-not-to-log.md) | 已完成 |
| 437 | [RED 指标是什么？](437-red-metrics.md) | 已完成 |
| 438 | [USE 指标是什么？](438-use-metrics.md) | 已完成 |
| 439 | [核心交易链路要监控哪些业务指标？](439-core-transaction-business-metrics.md) | 已完成 |
| 440 | [订单成功率下降如何排查？](440-order-success-rate-drop.md) | 已完成 |
| 441 | [支付回调延迟升高如何排查？](441-payment-callback-latency.md) | 已完成 |
| 442 | [库存预占失败率升高如何排查？](442-inventory-reservation-failure-rate.md) | 已完成 |
| 443 | [Outbox 积压如何排查？](443-outbox-backlog-troubleshooting.md) | 已完成 |
| 444 | [Kafka lag 增长如何排查？](444-kafka-lag-growth.md) | 已完成 |
| 445 | [Redis 命中率下降如何排查？](445-redis-hit-rate-drop-troubleshooting.md) | 已完成 |
| 446 | [数据库连接池耗尽如何排查？](446-db-connection-pool-exhaustion.md) | 已完成 |
| 447 | [P99 升高但平均延迟正常说明什么？](447-p99-high-average-normal.md) | 已完成 |
| 448 | [告警为什么不能太多？](448-alert-fatigue.md) | 已完成 |
| 449 | [如何设计告警级别？](449-alert-severity-design.md) | 已完成 |
| 450 | [SLO、SLI、SLA 有什么区别？](450-slo-sli-sla.md) | 已完成 |
| 451 | [错误预算是什么？](451-error-budget.md) | 已完成 |
| 452 | [burn rate 告警如何理解？](452-burn-rate-alerting.md) | 已完成 |
| 453 | [Runbook 应该包含什么？](453-runbook-content.md) | 已完成 |
| 454 | [事故复盘应该关注什么？](454-incident-review-focus.md) | 已完成 |
| 455 | [如何区分根因和触发因素？](455-root-cause-vs-trigger.md) | 已完成 |
| 456 | [如何设计一次故障演练？](456-failure-drill-design.md) | 已完成 |
| 457 | [如何证明系统具备可恢复能力？](457-prove-recoverability.md) | 已完成 |
| 458 | [Docker 镜像分层是什么？](458-docker-image-layers.md) | 已完成 |
| 459 | [如何构建小而安全的 Java 镜像？](459-small-secure-java-image.md) | 已完成 |
| 460 | [为什么生产容器不建议 root 用户运行？](460-non-root-containers.md) | 已完成 |
| 461 | [Kubernetes Deployment、Service、Ingress 分别是什么？](461-k8s-deployment-service-ingress.md) | 已完成 |
| 462 | [ConfigMap 和 Secret 有什么区别？](462-configmap-vs-secret.md) | 已完成 |
| 463 | [readinessProbe、livenessProbe、startupProbe 如何设计？](463-k8s-probe-design.md) | 已完成 |
| 464 | [为什么 liveness 不应该强依赖所有下游？](464-liveness-not-dependent-on-downstreams.md) | 已完成 |
| 465 | [HPA 根据什么指标扩缩容？](465-hpa-metrics.md) | 已完成 |
| 466 | [requests 和 limits 如何设置？](466-k8s-requests-limits.md) | 已完成 |
| 467 | [CPU limit 对 Java 服务有什么影响？](467-cpu-limit-impact-on-java.md) | 已完成 |
| 468 | [PodDisruptionBudget 解决什么问题？](468-pod-disruption-budget.md) | 已完成 |
| 469 | [滚动升级如何保证可用性？](469-rolling-upgrade-availability.md) | 已完成 |
| 470 | [优雅关闭如何配置？](470-graceful-shutdown-config.md) | 已完成 |
| 471 | [服务发现如何工作？](471-service-discovery.md) | 已完成 |
| 472 | [Ingress 和 API Gateway 有什么区别？](472-ingress-vs-api-gateway.md) | 已完成 |
| 473 | [NetworkPolicy 解决什么问题？](473-network-policy.md) | 已完成 |
| 474 | [多可用区部署要考虑什么？](474-multi-az-deployment.md) | 已完成 |
| 475 | [蓝绿发布和金丝雀发布有什么区别？](475-blue-green-vs-canary.md) | 已完成 |
| 476 | [灰度发布观察哪些指标？](476-canary-metrics.md) | 已完成 |
| 477 | [回滚前为什么要考虑数据库兼容？](477-rollback-db-compatibility.md) | 已完成 |
| 478 | [数据库迁移如何配合应用发布？](478-db-migration-with-release.md) | 已完成 |
| 479 | [什么是 expand-contract 发布模式？](479-expand-contract-release.md) | 已完成 |
| 480 | [配置错误导致故障如何快速回滚？](480-config-rollback.md) | 已完成 |
| 481 | [如何设计生产 Secret 管理？](481-production-secret-management.md) | 已完成 |
| 482 | [Service Mesh 解决什么问题？](482-service-mesh.md) | 已完成 |
| 483 | [mTLS 在服务间调用中有什么价值？](483-mtls-value.md) | 已完成 |
| 484 | [什么时候不应该引入 Service Mesh？](484-when-not-to-use-service-mesh.md) | 已完成 |
| 485 | [单元测试、集成测试、端到端测试有什么区别？](485-unit-integration-e2e-tests.md) | 已完成 |
| 486 | [哪些逻辑必须有单元测试？](486-logic-needs-unit-tests.md) | 已完成 |
| 487 | [哪些逻辑必须有集成测试？](487-logic-needs-integration-tests.md) | 已完成 |
| 488 | [Mock 的优点和风险是什么？](488-mock-benefits-risks.md) | 已完成 |
| 489 | [Testcontainers 适合验证什么？](489-testcontainers-use-cases.md) | 已完成 |
| 490 | [为什么只测 happy path 不够？](490-happy-path-not-enough.md) | 已完成 |
| 491 | [如何测试幂等？](491-test-idempotency.md) | 已完成 |
| 492 | [如何测试库存防超卖？](492-test-inventory-oversell.md) | 已完成 |
| 493 | [如何测试支付重复回调？](493-test-payment-duplicate-callback.md) | 已完成 |
| 494 | [如何测试 Outbox 重试？](494-test-outbox-retry.md) | 已完成 |
| 495 | [如何测试 MQ 重复消费？](495-test-mq-duplicate-consumption.md) | 已完成 |
| 496 | [如何测试补偿任务？](496-test-compensation-job.md) | 已完成 |
| 497 | [如何测试限流和熔断？](497-test-rate-limit-circuit-breaker.md) | 已完成 |
| 498 | [如何测试降级逻辑？](498-test-degradation.md) | 已完成 |
| 499 | [Smoke 测试应该覆盖哪些链路？](499-smoke-test-chains.md) | 已完成 |
| 500 | [压测前要准备什么？](500-before-load-test.md) | 已完成 |
| 501 | [压测结果如何分析？](501-analyze-load-test-results.md) | 已完成 |
| 502 | [如何定位压测瓶颈？](502-find-load-test-bottleneck.md) | 已完成 |
| 503 | [回归测试如何分层？](503-layered-regression-tests.md) | 已完成 |
| 504 | [CI 流水线应该包含哪些阶段？](504-ci-pipeline-stages.md) | 已完成 |
| 505 | [Checkstyle、SpotBugs、PMD 分别解决什么问题？](505-checkstyle-spotbugs-pmd.md) | 已完成 |
| 506 | [代码覆盖率高是否代表质量高？](506-code-coverage-quality.md) | 已完成 |
| 507 | [如何做代码评审？](507-code-review.md) | 已完成 |
| 508 | [如何评审分布式一致性相关代码？](508-review-distributed-consistency-code.md) | 已完成 |
| 509 | [如何设计公共模块，避免 common 变成垃圾桶？](509-common-module-design.md) | 已完成 |
| 510 | [如何管理依赖版本？](510-dependency-version-management.md) | 已完成 |
| 511 | [如何处理依赖漏洞？](511-handle-dependency-vulnerabilities.md) | 已完成 |
| 512 | [如何设计兼容性测试？](512-compatibility-tests.md) | 已完成 |
| 513 | [如何保证文档和代码一致？](513-docs-code-consistency.md) | 已完成 |
| 514 | [设计一个京东/Amazon 类电商系统。](514-design-jd-amazon-ecommerce.md) | 已完成 |
| 515 | [设计下单系统。](515-design-order-system.md) | 已完成 |
| 516 | [设计库存系统。](516-design-inventory-system.md) | 已完成 |
| 517 | [设计支付系统。](517-design-payment-system.md) | 已完成 |
| 518 | [设计购物车系统。](518-design-cart-system.md) | 已完成 |
| 519 | [设计商品详情系统。](519-design-product-detail-system.md) | 已完成 |
| 520 | [设计商品搜索系统。](520-design-product-search-system.md) | 已完成 |
| 521 | [设计秒杀系统。](521-design-flash-sale-system.md) | 已完成 |
| 522 | [设计优惠券系统。](522-design-coupon-system.md) | 已完成 |
| 523 | [设计价格服务。](523-design-pricing-system.md) | 已完成 |
| 524 | [设计履约系统。](524-design-fulfillment-system.md) | 已完成 |
| 525 | [设计售后退款系统。](525-design-after-sales-refund.md) | 已完成 |
| 526 | [设计商家入驻和商品审核系统。](526-design-merchant-onboarding-review.md) | 已完成 |
| 527 | [设计开放平台 API。](527-design-openapi.md) | 已完成 |
| 528 | [设计风控系统。](528-design-risk-system.md) | 已完成 |
| 529 | [设计推荐系统的在线服务部分。](529-design-recommendation-online.md) | 已完成 |
| 530 | [设计广告投放系统的核心链路。](530-design-ad-delivery.md) | 已完成 |
| 531 | [设计订单对账系统。](531-design-order-reconciliation.md) | 已完成 |
| 532 | [设计支付渠道对账系统。](532-design-payment-channel-reconciliation.md) | 已完成 |
| 533 | [设计消息重放平台。](533-design-message-replay-platform.md) | 已完成 |
| 534 | [设计内部运维补偿平台。](534-design-internal-compensation-platform.md) | 已完成 |
| 535 | [设计分布式 ID 服务。](535-design-distributed-id.md) | 已完成 |
| 536 | [设计配置中心。](536-design-config-center.md) | 已完成 |
| 537 | [设计限流系统。](537-design-rate-limit-system.md) | 已完成 |
| 538 | [设计熔断降级平台。](538-design-circuit-degrade-platform.md) | 已完成 |
| 539 | [设计灰度发布平台。](539-design-canary-release-platform.md) | 已完成 |
| 540 | [设计日志采集和查询平台。](540-design-log-collection-query.md) | 已完成 |
| 541 | [设计指标和告警平台。](541-design-metrics-alerting.md) | 已完成 |

## 补充题目

| 序号 | 问题 | 状态 |
| --- | --- | --- |
| 542 | [设计链路追踪系统。](542-system-answer.md) | 已完成 |
| 543 | [设计多区域电商系统。](543-system-answer.md) | 已完成 |
| 544 | [设计异地多活订单系统。](544-system-answer.md) | 已完成 |
| 545 | [设计数据归档系统。](545-system-answer.md) | 已完成 |
| 546 | [设计大促容量保障方案。](546-system-answer.md) | 已完成 |
| 547 | [设计热点 SKU 保护方案。](547-system-answer.md) | 已完成 |
| 548 | [设计商品缓存系统。](548-system-answer.md) | 已完成 |
| 549 | [设计 Kafka Outbox 可靠事件系统。](549-system-answer.md) | 已完成 |
| 550 | [设计面向 10 亿用户的用户中心。](550-system-answer.md) | 已完成 |
| 551 | [设计支持 100 万峰值并发的网关。](551-system-answer.md) | 已完成 |
| 552 | [设计支持 10 万下单 QPS 的交易链路。](552-system-answer.md) | 已完成 |
| 553 | [设计一次数据库扩容和迁移方案。](553-system-answer.md) | 已完成 |
| 554 | [设计一次从单体到微服务的演进方案。](554-system-answer.md) | 已完成 |
| 555 | [下单成功率突然下降，你怎么排查？](555-incident-answer.md) | 已完成 |
| 556 | [支付成功但订单未变成已支付，怎么处理？](556-incident-answer.md) | 已完成 |
| 557 | [库存出现少量超卖，怎么定位和修复？](557-incident-answer.md) | 已完成 |
| 558 | [库存预占记录大量过期未释放，怎么恢复？](558-incident-answer.md) | 已完成 |
| 559 | [Outbox 表积压大量待发送事件，怎么处理？](559-incident-answer.md) | 已完成 |
| 560 | [Kafka 某个 topic lag 快速增长，怎么排查？](560-incident-answer.md) | 已完成 |
| 561 | [Redis 集群抖动，商品详情接口 P99 升高，怎么处理？](561-incident-answer.md) | 已完成 |
| 562 | [数据库 CPU 100%，你先看什么？](562-incident-answer.md) | 已完成 |
| 563 | [数据库连接池耗尽，如何止血？](563-incident-answer.md) | 已完成 |
| 564 | [某个新版本发布后错误率上升，如何判断是否回滚？](564-incident-answer.md) | 已完成 |
| 565 | [灰度 5% 正常，放量 50% 后异常，可能是什么原因？](565-incident-answer.md) | 已完成 |
| 566 | [一个 Pod 频繁重启，如何排查？](566-incident-answer.md) | 已完成 |
| 567 | [JVM Full GC 频繁，如何处理？](567-incident-answer.md) | 已完成 |
| 568 | [线程数暴涨，如何定位？](568-incident-answer.md) | 已完成 |
| 569 | [线上死锁如何处理？](569-incident-answer.md) | 已完成 |
| 570 | [支付渠道重复回调导致大量冲突日志，是否是事故？](570-incident-answer.md) | 已完成 |
| 571 | [对账发现渠道成功本地失败，怎么修复？](571-incident-answer.md) | 已完成 |
| 572 | [用户投诉重复扣款，如何排查？](572-incident-answer.md) | 已完成 |
| 573 | [秒杀开始后普通下单也变慢，怎么止血？](573-incident-answer.md) | 已完成 |
| 574 | [搜索结果大量缺商品，怎么恢复？](574-incident-answer.md) | 已完成 |
| 575 | [商品价格显示旧值，如何排查缓存一致性问题？](575-incident-answer.md) | 已完成 |
| 576 | [配置中心推错配置，如何回滚？](576-incident-answer.md) | 已完成 |
| 577 | [Secret 泄露后如何应急？](577-incident-answer.md) | 已完成 |
| 578 | [日志系统故障是否会影响交易链路？](578-incident-answer.md) | 已完成 |
| 579 | [监控告警误报太多，如何治理？](579-incident-answer.md) | 已完成 |
| 580 | [生产数据库误删数据，如何恢复？](580-incident-answer.md) | 已完成 |
| 581 | [消费者 bug 导致错误写入大量数据，如何修复？](581-incident-answer.md) | 已完成 |
| 582 | [下游恢复后流量全部放开又被打挂，如何避免？](582-incident-answer.md) | 已完成 |
| 583 | [大促前你会做哪些检查？](583-incident-answer.md) | 已完成 |
| 584 | [大促中核心指标异常，你如何决策降级？](584-incident-answer.md) | 已完成 |
| 585 | [你为什么选择微服务而不是单体？](585-tradeoff-answer.md) | 已完成 |
| 586 | [你为什么选择本地事务加 Outbox，而不是 TCC？](586-tradeoff-answer.md) | 已完成 |
| 587 | [你为什么不使用全局分布式事务？](587-tradeoff-answer.md) | 已完成 |
| 588 | [你为什么用 Kafka，而不是 RabbitMQ？](588-tradeoff-answer.md) | 已完成 |
| 589 | [你为什么用 Redis 缓存，而不是只靠数据库？](589-tradeoff-answer.md) | 已完成 |
| 590 | [你为什么用 OpenSearch，而不是 MySQL like 查询？](590-tradeoff-answer.md) | 已完成 |
| 591 | [你为什么要做库存预占，而不是支付时再扣库存？](591-tradeoff-answer.md) | 已完成 |
| 592 | [你为什么要保存价格快照？](592-tradeoff-answer.md) | 已完成 |
| 593 | [你为什么要做支付流水，而不是只更新支付状态？](593-tradeoff-answer.md) | 已完成 |
| 594 | [你为什么要做对账？](594-tradeoff-answer.md) | 已完成 |
| 595 | [你为什么要做补偿任务，而不是人工处理所有异常？](595-tradeoff-answer.md) | 已完成 |
| 596 | [你为什么要做幂等表，而不是只在代码里判断状态？](596-tradeoff-answer.md) | 已完成 |
| 597 | [你为什么要引入 Kubernetes？](597-tradeoff-answer.md) | 已完成 |
| 598 | [你为什么要拆 common？](598-tradeoff-answer.md) | 已完成 |
| 599 | [你如何避免 common 变成强耦合中心？](599-tradeoff-answer.md) | 已完成 |
| 600 | [你如何判断一个方案过度设计？](600-tradeoff-answer.md) | 已完成 |
| 601 | [你如何在交付速度和架构质量之间取舍？](601-tradeoff-answer.md) | 已完成 |
| 602 | [你如何在一致性和可用性之间取舍？](602-tradeoff-answer.md) | 已完成 |
| 603 | [你如何在成本和性能之间取舍？](603-tradeoff-answer.md) | 已完成 |
| 604 | [如果只能做三件事提升稳定性，你做什么？](604-tradeoff-answer.md) | 已完成 |
| 605 | [如果只能做三件事提升吞吐，你做什么？](605-tradeoff-answer.md) | 已完成 |
| 606 | [如果只能做三件事降低成本，你做什么？](606-tradeoff-answer.md) | 已完成 |
| 607 | [如果让你重构当前系统，你优先改什么？](607-tradeoff-answer.md) | 已完成 |
| 608 | [当前系统最大的技术债是什么？](608-tradeoff-answer.md) | 已完成 |
| 609 | [当前系统最大的生产风险是什么？](609-tradeoff-answer.md) | 已完成 |
| 610 | [你如何证明这个系统不是玩具项目？](610-tradeoff-answer.md) | 已完成 |
| 611 | [讲一次你主导复杂系统设计的经历。](611-behavior-answer.md) | 已完成 |
| 612 | [讲一次你在需求模糊时如何拆解问题。](612-behavior-answer.md) | 已完成 |
| 613 | [讲一次你发现并修复深层技术问题的经历。](613-behavior-answer.md) | 已完成 |
| 614 | [讲一次你推动团队采用更好工程实践的经历。](614-behavior-answer.md) | 已完成 |
| 615 | [讲一次你和别人技术意见不一致时如何处理。](615-behavior-answer.md) | 已完成 |
| 616 | [讲一次你做出技术取舍的经历。](616-behavior-answer.md) | 已完成 |
| 617 | [讲一次你为了长期质量牺牲短期速度的经历。](617-behavior-answer.md) | 已完成 |
| 618 | [讲一次你为了业务交付接受技术债的经历。](618-behavior-answer.md) | 已完成 |
| 619 | [讲一次你处理线上事故的经历。](619-behavior-answer.md) | 已完成 |
| 620 | [讲一次你在事故后推动系统性改进的经历。](620-behavior-answer.md) | 已完成 |
| 621 | [讲一次你用数据证明方案有效的经历。](621-behavior-answer.md) | 已完成 |
| 622 | [讲一次你降低系统成本的经历。](622-behavior-answer.md) | 已完成 |
| 623 | [讲一次你提升系统可用性的经历。](623-behavior-answer.md) | 已完成 |
| 624 | [讲一次你提升性能或容量的经历。](624-behavior-answer.md) | 已完成 |
| 625 | [讲一次你 mentor 其他工程师的经历。](625-behavior-answer.md) | 已完成 |
| 626 | [讲一次你影响多个团队的经历。](626-behavior-answer.md) | 已完成 |
| 627 | [讲一次你面对失败项目如何复盘。](627-behavior-answer.md) | 已完成 |
| 628 | [讲一次你主动承担超出职责范围工作的经历。](628-behavior-answer.md) | 已完成 |
| 629 | [讲一次你拒绝不合理方案的经历。](629-behavior-answer.md) | 已完成 |
| 630 | [讲一次你把复杂问题讲清楚给非技术人员的经历。](630-behavior-answer.md) | 已完成 |
| 631 | [讲一次你坚持高标准的经历。](631-behavior-answer.md) | 已完成 |
| 632 | [讲一次你快速学习陌生领域并交付的经历。](632-behavior-answer.md) | 已完成 |
| 633 | [讲一次你处理安全或合规风险的经历。](633-behavior-answer.md) | 已完成 |
| 634 | [讲一次你没有足够资源但仍然交付结果的经历。](634-behavior-answer.md) | 已完成 |
| 635 | [讲一次你做长期架构演进规划的经历。](635-behavior-answer.md) | 已完成 |
| 636 | [手写 LRU Cache。](636-coding-answer.md) | 已完成 |
| 637 | [手写令牌桶限流器。](637-coding-answer.md) | 已完成 |
| 638 | [手写滑动窗口限流器。](638-coding-answer.md) | 已完成 |
| 639 | [手写线程安全单例。](639-coding-answer.md) | 已完成 |
| 640 | [手写阻塞队列。](640-coding-answer.md) | 已完成 |
| 641 | [手写生产者消费者模型。](641-coding-answer.md) | 已完成 |
| 642 | [手写简化线程池。](642-coding-answer.md) | 已完成 |
| 643 | [手写延迟队列。](643-coding-answer.md) | 已完成 |
| 644 | [手写重试工具，支持指数退避和 jitter。](644-coding-answer.md) | 已完成 |
| 645 | [手写熔断器状态机。](645-coding-answer.md) | 已完成 |
| 646 | [手写幂等处理器。](646-coding-answer.md) | 已完成 |
| 647 | [手写订单状态机。](647-coding-answer.md) | 已完成 |
| 648 | [手写库存条件扣减逻辑。](648-coding-answer.md) | 已完成 |
| 649 | [手写 Outbox Relay 核心逻辑。](649-coding-answer.md) | 已完成 |
| 650 | [手写 MQ 消费端去重逻辑。](650-coding-answer.md) | 已完成 |
| 651 | [手写 HMAC 签名校验。](651-coding-answer.md) | 已完成 |
| 652 | [手写敏感字段脱敏工具。](652-coding-answer.md) | 已完成 |
| 653 | [手写统一异常处理。](653-coding-answer.md) | 已完成 |
| 654 | [手写分页查询接口。](654-coding-answer.md) | 已完成 |
| 655 | [手写分布式 ID 生成器简化版。](655-coding-answer.md) | 已完成 |
| 656 | [手写一致性 hash。](656-coding-answer.md) | 已完成 |
| 657 | [手写 Top K 统计。](657-coding-answer.md) | 已完成 |
| 658 | [手写限时任务执行器。](658-coding-answer.md) | 已完成 |
| 659 | [手写一个可关闭的后台 worker。](659-coding-answer.md) | 已完成 |
| 660 | [手写 SQL 查询最近 30 天每天下单量。](660-coding-answer.md) | 已完成 |
| 661 | [手写 SQL 查询支付对账差异。](661-coding-answer.md) | 已完成 |
| 662 | [手写 SQL 找出重复 request_id。](662-coding-answer.md) | 已完成 |
| 663 | [手写 Java 代码解析并聚合日志错误码。](663-coding-answer.md) | 已完成 |
| 664 | [这个团队负责的核心业务指标是什么？](664-reverse-answer.md) | 已完成 |
| 665 | [当前系统最大的稳定性挑战是什么？](665-reverse-answer.md) | 已完成 |
| 666 | [团队如何做 on-call 和事故复盘？](666-reverse-answer.md) | 已完成 |
| 667 | [服务规模大概是多少 QPS、数据量和实例数？](667-reverse-answer.md) | 已完成 |
| 668 | [团队使用哪些技术栈和部署平台？](668-reverse-answer.md) | 已完成 |
| 669 | [当前系统是一体化架构还是微服务架构？](669-reverse-answer.md) | 已完成 |
| 670 | [团队如何做灰度、回滚和容量评估？](670-reverse-answer.md) | 已完成 |
| 671 | [团队如何衡量 Senior/L6 工程师的影响力？](671-reverse-answer.md) | 已完成 |
| 672 | [新人加入后前三个月通常会负责什么？](672-reverse-answer.md) | 已完成 |
| 673 | [团队当前最需要解决的技术债是什么？](673-reverse-answer.md) | 已完成 |
