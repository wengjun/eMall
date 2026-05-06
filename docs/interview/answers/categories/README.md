# 面试题分类目录

[返回答案手册](../README.md) | [返回逐题精讲目录](../sequential/README.md) | [返回面试题库](../../question-bank.md)

本目录按“一级能力域 -> 二级专题 -> 具体题目”的方式组织面试题，方便按专题复习。
所有题目均已链接到逐题精讲文件；补充题会链接到最相关的已完成答案。
题号以逐题精讲目录为准；少量算法补充题会链接到最相关的逐题精讲答案。

## 01 Java 后端基础

### 01 Java 语言和工程基础

题号：001-040；进度：40/40。

001. [Java 17 相比 Java 8 有哪些重要变化？](../sequential/001-java-17-vs-java-8.md)
002. [`record` 适合哪些场景，不适合哪些场景？](../sequential/002-record-use-cases.md)
003. [`var` 会不会影响可读性，团队中如何约束？](../sequential/003-var-readability.md)
004. [`switch` 表达式相比传统 `switch` 有什么优势？](../sequential/004-switch-expression.md)
005. [`sealed class` 适合建模哪些业务场景？](../sequential/005-sealed-class.md)
006. [`Optional` 应该用在返回值、参数还是字段上？](../sequential/006-optional-usage.md)
007. [为什么金额不能用 `double`？](../sequential/007-money-double.md)
008. [`BigDecimal` 的 `equals` 和 `compareTo` 有什么区别？](../sequential/008-bigdecimal-equals-compareto.md)
009. [Java 时间 API 中 `Instant`、`LocalDateTime`、`ZonedDateTime` 怎么选？](../sequential/009-java-time-api.md)
010. [服务端为什么建议统一存储 UTC 时间？](../sequential/010-utc-storage.md)
011. [枚举适合表达哪些业务状态？](../sequential/011-enum-business-status.md)
012. [枚举状态扩展时如何保证兼容？](../sequential/012-enum-compatibility.md)
013. [面向对象中的封装在业务系统里具体体现在哪里？](../sequential/013-encapsulation.md)
014. [组合和继承如何取舍？](../sequential/014-composition-vs-inheritance.md)
015. [领域对象和 DTO 为什么要分开？](../sequential/015-domain-object-vs-dto.md)
016. [贫血模型和充血模型各有什么优缺点？](../sequential/016-anemic-rich-domain.md)
017. [如何避免所有业务逻辑堆在 Controller？](../sequential/017-avoid-fat-controller.md)
018. [如何设计清晰的包结构？](../sequential/018-package-structure.md)
019. [Java 异常分为哪些类型？](../sequential/019-java-exception-types.md)
020. [业务异常和系统异常应该如何区分？](../sequential/020-business-vs-system-exception.md)
021. [checked exception 和 unchecked exception 如何取舍？](../sequential/021-checked-vs-unchecked-exception.md)
022. [为什么不能直接把异常堆栈返回给前端？](../sequential/022-hide-stacktrace-from-client.md)
023. [如何设计统一错误码？](../sequential/023-error-code-design.md)
024. [错误码如何兼容多语言和多端？](../sequential/024-error-code-i18n.md)
025. [泛型擦除是什么？](../sequential/025-generic-type-erasure.md)
026. [泛型通配符 `extends` 和 `super` 怎么理解？](../sequential/026-generics-extends-super.md)
027. [`equals` 和 `hashCode` 的契约是什么？](../sequential/027-equals-hashcode-contract.md)
028. [为什么可变对象不适合作为 `HashMap` 的 key？](../sequential/028-mutable-hashmap-key.md)
029. [`ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？](../sequential/029-java-collections-choice.md)
030. [`ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？](../sequential/030-concurrenthashmap-vs-hashtable.md)
031. [Java 反射的成本和风险是什么？](../sequential/031-reflection-cost-risk.md)
032. [注解是如何在运行时生效的？](../sequential/032-annotation-runtime.md)
033. [SPI 机制适合解决什么扩展问题？](../sequential/033-spi-extension.md)
034. [如何设计一个可扩展的插件机制？](../sequential/034-plugin-mechanism.md)
035. [为什么工程代码要重视不可变对象？](../sequential/035-immutable-objects.md)
036. [如何判断一段代码是否可测试？](../sequential/036-testable-code.md)
037. [如何设计稳定的公共库 API？](../sequential/037-stable-public-api.md)
038. [公共库升级如何保证向后兼容？](../sequential/038-public-library-compatibility.md)
039. [为什么大型项目要限制循环依赖？](../sequential/039-cyclic-dependencies.md)
040. [如何做模块边界和依赖方向治理？](../sequential/040-module-boundary-governance.md)

### 02 JVM 和性能诊断

题号：041-077；进度：37/37。

041. [JVM 内存区域包括哪些？](../sequential/041-jvm-memory-areas.md)
042. [堆、栈、方法区、直接内存分别存什么？](../sequential/042-heap-stack-metaspace-direct-memory.md)
043. [对象从创建到回收大致经历什么过程？](../sequential/043-object-lifecycle.md)
044. [GC Roots 包括哪些？](../sequential/044-gc-roots.md)
045. [Minor GC、Major GC、Full GC 有什么区别？](../sequential/045-minor-major-full-gc.md)
046. [G1、ZGC、Shenandoah 的设计目标有什么不同？](../sequential/046-g1-zgc-shenandoah.md)
047. [为什么低延迟服务要关注 GC 暂停？](../sequential/047-low-latency-gc-pause.md)
048. [如何判断线上服务是否存在内存泄漏？](../sequential/048-detect-memory-leak.md)
049. [`OutOfMemoryError` 常见类型有哪些？](../sequential/049-oome-types.md)
050. [堆 OOM 和直接内存 OOM 如何区分？](../sequential/050-heap-vs-direct-oom.md)
051. [线程数过多会带来什么问题？](../sequential/051-too-many-threads.md)
052. [`jstack` 可以定位哪些问题？](../sequential/052-jstack-diagnostics.md)
053. [`jmap`、`jcmd`、JFR 分别适合什么场景？](../sequential/053-jmap-jcmd-jfr.md)
054. [如何分析 CPU 飙高？](../sequential/054-analyze-high-cpu.md)
055. [如何分析接口 P99 突然升高？](../sequential/055-analyze-p99-spike.md)
056. [如何判断是 GC、数据库、锁竞争还是下游慢导致延迟升高？](../sequential/056-gc-db-lock-downstream-latency.md)
057. [Java 服务启动慢可能有哪些原因？](../sequential/057-java-service-slow-startup.md)
058. [类加载机制是什么？](../sequential/058-class-loading-mechanism.md)
059. [双亲委派模型解决什么问题？](../sequential/059-parent-delegation.md)
060. [什么场景需要自定义 ClassLoader？](../sequential/060-custom-classloader.md)
061. [JIT 编译是什么？](../sequential/061-jit-compilation.md)
062. [热点代码和解释执行有什么区别？](../sequential/062-hot-code-vs-interpretation.md)
063. [逃逸分析有什么作用？](../sequential/063-escape-analysis.md)
064. [对象分配为什么通常很快？](../sequential/064-fast-object-allocation.md)
065. [为什么频繁创建短生命周期对象不一定总是坏事？](../sequential/065-short-lived-objects.md)
066. [如何减少不必要的对象分配？](../sequential/066-reduce-object-allocation.md)
067. [如何设置生产环境 JVM 参数？](../sequential/067-production-jvm-options.md)
068. [容器环境下 JVM 如何感知内存限制？](../sequential/068-jvm-container-memory.md)
069. [`-Xmx` 设置过大或过小分别有什么风险？](../sequential/069-xmx-too-large-or-small.md)
070. [线上是否应该主动调用 `System.gc()`？](../sequential/070-system-gc-production.md)
071. [如何做 JVM 指标监控？](../sequential/071-jvm-metrics-monitoring.md)
072. [需要重点监控哪些 JVM 指标？](../sequential/072-key-jvm-metrics.md)
073. [GC 日志如何阅读？](../sequential/073-read-gc-log.md)
074. [线程池队列堆积和 JVM 内存上涨有什么关系？](../sequential/074-threadpool-queue-memory.md)
075. [如何定位死锁？](../sequential/075-diagnose-deadlock.md)
076. [如何定位锁竞争？](../sequential/076-diagnose-lock-contention.md)
077. [如何设计一次 Java 服务压测和性能剖析？](../sequential/077-java-loadtest-profiling.md)

### 03 Java 并发和线程池

题号：078-114；进度：37/37。

078. [线程和进程有什么区别？](../sequential/078-thread-vs-process.md)
079. [Java 线程状态有哪些？](../sequential/079-java-thread-states.md)
080. [`synchronized` 的原理是什么？](../sequential/080-synchronized-principle.md)
081. [偏向锁、轻量级锁、重量级锁是什么？](../sequential/081-lock-upgrade.md)
082. [`ReentrantLock` 和 `synchronized` 怎么选？](../sequential/082-reentrantlock-vs-synchronized.md)
083. [公平锁和非公平锁有什么区别？](../sequential/083-fair-vs-nonfair-lock.md)
084. [`volatile` 解决什么问题，不能解决什么问题？](../sequential/084-volatile.md)
085. [happens-before 规则是什么？](../sequential/085-happens-before.md)
086. [Java 内存模型解决什么问题？](../sequential/086-java-memory-model.md)
087. [CAS 是什么？](../sequential/087-cas.md)
088. [ABA 问题是什么，如何解决？](../sequential/088-aba-problem.md)
089. [`AtomicInteger` 和 `LongAdder` 如何取舍？](../sequential/089-atomicinteger-vs-longadder.md)
090. [`CountDownLatch`、`CyclicBarrier`、`Semaphore` 分别适合什么场景？](../sequential/090-latch-barrier-semaphore.md)
091. [`CompletableFuture` 如何处理异步编排？](../sequential/091-completablefuture-async-composition.md)
092. [`CompletableFuture` 默认线程池有什么风险？](../sequential/092-completablefuture-default-pool-risk.md)
093. [为什么生产代码不能随意使用公共 ForkJoinPool？](../sequential/093-avoid-common-forkjoinpool.md)
094. [线程池核心参数如何设置？](../sequential/094-threadpool-core-parameters.md)
095. [CPU 密集型和 IO 密集型线程池如何估算大小？](../sequential/095-cpu-io-threadpool-size.md)
096. [线程池队列应该用有界还是无界？](../sequential/096-bounded-vs-unbounded-queue.md)
097. [拒绝策略怎么选？](../sequential/097-rejection-policy.md)
098. [如何避免线程池雪崩？](../sequential/098-avoid-threadpool-avalanche.md)
099. [多个下游服务是否应该共享同一个线程池？](../sequential/099-share-threadpool-downstreams.md)
100. [什么是线程池隔离？](../sequential/100-threadpool-isolation.md)
101. [什么是舱壁隔离？](../sequential/101-bulkhead-isolation.md)
102. [任务超时后线程是否真的停止？](../sequential/102-timeout-does-not-stop-thread.md)
103. [Java 中断机制如何正确使用？](../sequential/103-java-interruption.md)
104. [如何设计可取消的异步任务？](../sequential/104-cancellable-async-task.md)
105. [如何避免死锁？](../sequential/105-avoid-deadlock.md)
106. [如何减少锁粒度？](../sequential/106-reduce-lock-granularity.md)
107. [分段锁和库存桶有什么相似点？](../sequential/107-segment-lock-and-stock-bucket.md)
108. [单机锁为什么不能解决多实例并发？](../sequential/108-local-lock-not-for-multi-instance.md)
109. [分布式锁适合哪些场景？](../sequential/109-distributed-lock-use-cases.md)
110. [分布式锁有哪些风险？](../sequential/110-distributed-lock-risks.md)
111. [Redlock 争议是什么？](../sequential/111-redlock-controversy.md)
112. [为什么数据库唯一键通常比分布式锁更可靠？](../sequential/112-db-unique-key-vs-distributed-lock.md)
113. [并发下如何实现只执行一次？](../sequential/113-execute-once-concurrency.md)
114. [如何设计幂等和并发安全的组合方案？](../sequential/114-idempotency-and-concurrency.md)

### 04 Spring Boot 和 Spring Cloud

题号：115-149；进度：35/35。

115. [Spring Boot 自动配置原理是什么？](../sequential/115-spring-boot-auto-configuration.md)
116. [`@SpringBootApplication` 包含哪些注解？](../sequential/116-springbootapplication.md)
117. [Bean 的生命周期是什么？](../sequential/117-spring-bean-lifecycle.md)
118. [构造函数注入、字段注入、Setter 注入如何取舍？](../sequential/118-injection-styles.md)
119. [为什么推荐构造函数注入？](../sequential/119-why-constructor-injection.md)
120. [Spring AOP 的代理机制是什么？](../sequential/120-spring-aop-proxy.md)
121. [JDK 动态代理和 CGLIB 有什么区别？](../sequential/121-jdk-proxy-vs-cglib.md)
122. [`@Transactional` 为什么有时不生效？](../sequential/122-transactional-not-effective.md)
123. [自调用为什么绕过事务代理？](../sequential/123-self-invocation-bypass-transaction.md)
124. [事务传播行为有哪些？](../sequential/124-transaction-propagation.md)
125. [`REQUIRED`、`REQUIRES_NEW`、`NESTED` 有什么区别？](../sequential/125-required-requires-new-nested.md)
126. [事务隔离级别如何配置？](../sequential/126-transaction-isolation-config.md)
127. [事务里调用远程服务有什么风险？](../sequential/127-remote-call-in-transaction-risk.md)
128. [Controller、Service、Repository 的职责边界是什么？](../sequential/128-controller-service-repository-boundary.md)
129. [`@ControllerAdvice` 如何做统一异常处理？](../sequential/129-controller-advice.md)
130. [Bean Validation 适合做哪些校验？](../sequential/130-bean-validation.md)
131. [参数校验和业务校验如何区分？](../sequential/131-parameter-vs-business-validation.md)
132. [Spring 配置加载优先级是什么？](../sequential/132-spring-config-priority.md)
133. [profile、环境变量、配置中心如何配合？](../sequential/133-profile-env-config-center.md)
134. [如何安全管理生产密钥？](../sequential/134-production-secret-management.md)
135. [Actuator 暴露哪些端点比较合理？](../sequential/135-actuator-endpoints.md)
136. [健康检查应该包含哪些内容？](../sequential/136-health-check-design.md)
137. [readiness 和 liveness 在 Spring 中如何实现？](../sequential/137-readiness-liveness-spring.md)
138. [RestClient、WebClient、Feign 如何取舍？](../sequential/138-restclient-webclient-feign.md)
139. [阻塞式和响应式调用如何取舍？](../sequential/139-blocking-vs-reactive.md)
140. [WebFlux 是否一定比 MVC 性能更高？](../sequential/140-webflux-vs-mvc-performance.md)
141. [如何设置 HTTP 客户端连接池？](../sequential/141-http-client-connection-pool.md)
142. [如何设置连接超时、读取超时和总超时？](../sequential/142-http-timeouts.md)
143. [如何透传 trace ID？](../sequential/143-trace-id-propagation.md)
144. [如何设计统一的内部服务调用规范？](../sequential/144-internal-service-call-standard.md)
145. [Spring Cache 的使用边界是什么？](../sequential/145-spring-cache-boundary.md)
146. [Spring 事件和 MQ 事件有什么区别？](../sequential/146-spring-event-vs-mq.md)
147. [如何避免 Bean 循环依赖？](../sequential/147-avoid-bean-circular-dependency.md)
148. [如何设计 starter 或 auto-configuration？](../sequential/148-design-starter-auto-configuration.md)
149. [如何在多模块项目中复用公共配置？](../sequential/149-reuse-common-config-in-multi-module.md)

### 05 API 设计和接口治理

题号：150-183；进度：34/34。

150. [REST API 的资源建模原则是什么？](../sequential/150-rest-resource-modeling.md)
151. [`GET`、`POST`、`PUT`、`PATCH`、`DELETE` 应该如何使用？](../sequential/151-http-method-semantics.md)
152. [哪些接口应该设计成幂等？](../sequential/152-idempotent-apis.md)
153. [幂等键应该放 Header 还是 Body？](../sequential/153-idempotency-key-header-or-body.md)
154. [API 版本如何设计？](../sequential/154-api-versioning.md)
155. [什么时候需要新版本 API？](../sequential/155-when-new-api-version.md)
156. [如何设计统一响应体？](../sequential/156-unified-response-body.md)
157. [HTTP 状态码和业务错误码如何配合？](../sequential/157-error-code-vs-http-status.md)
158. [分页接口如何设计？](../sequential/158-pagination-api.md)
159. [如何防止批量查询拖垮系统？](../sequential/159-prevent-bulk-query-overload.md)
160. [API 如何保证向后兼容？](../sequential/160-api-backward-compatibility.md)
161. [如何做字段废弃和迁移？](../sequential/161-field-deprecation-migration.md)
162. [开放 API 如何做签名？](../sequential/162-open-api-signature.md)
163. [如何防止接口重放攻击？](../sequential/163-prevent-replay-attack.md)
164. [如何做请求限流和配额？](../sequential/164-api-rate-limiting.md)
165. [如何设计 API 网关层职责？](../sequential/165-api-gateway-responsibility.md)
166. [网关和业务服务分别做什么校验？](../sequential/166-gateway-vs-service-validation.md)
167. [如何设计 BFF？](../sequential/167-bff-design.md)
168. [移动端和 PC 端 API 是否应该完全共用？](../sequential/168-mobile-pc-api-sharing.md)
169. [内部 API 和开放 API 有什么区别？](../sequential/169-internal-vs-external-api.md)
170. [如何写 API 文档和契约测试？](../sequential/170-api-docs-contract-tests.md)
171. [深分页有什么问题？](../sequential/171-deep-pagination-risk.md)
172. [游标分页和 offset 分页如何取舍？](../sequential/172-cursor-vs-offset-pagination.md)
173. [查询接口如何防止过度复杂？](../sequential/173-query-api-complexity-control.md)
174. [批量接口如何设计部分成功？](../sequential/174-bulk-api-partial-success.md)
175. [文件上传接口如何做安全限制？](../sequential/175-file-upload-api-security.md)
176. [如何做 API 兼容性测试？](../sequential/176-api-compatibility-tests.md)
177. [如何设计接口超时预算？](../sequential/177-api-timeout-budget.md)
178. [如何处理客户端重试导致的重复请求？](../sequential/178-client-retry-duplicate-request.md)
179. [如何设计错误信息，既便于排障又不泄露内部实现？](../sequential/179-safe-error-message-design.md)
180. [GraphQL 和 REST 如何取舍？](../sequential/180-graphql-vs-rest.md)
181. [gRPC 和 HTTP JSON 如何取舍？](../sequential/181-grpc-vs-http-json.md)
182. [如何设计跨服务错误码映射？](../sequential/182-cross-service-error-code-mapping.md)
183. [如何治理废弃 API？](../sequential/183-deprecated-api-governance.md)

## 02 分布式架构和中间件

### 01 微服务拆分和架构治理

题号：184-210；进度：27/27。

184. [什么情况下应该拆微服务？](../sequential/184-when-to-split-microservices.md)
185. [什么情况下不应该拆微服务？](../sequential/185-when-not-to-split-microservices.md)
186. [微服务和模块化单体如何取舍？](../sequential/186-microservice-vs-modular-monolith.md)
187. [服务边界应该按业务域还是技术层拆？](../sequential/187-service-boundary-business-vs-technical.md)
188. [什么是数据所有权？](../sequential/188-data-ownership.md)
189. [为什么服务之间不应该共享数据库表？](../sequential/189-no-shared-database-tables.md)
190. [拆库后跨服务查询怎么做？](../sequential/190-cross-service-query.md)
191. [跨服务事务怎么处理？](../sequential/191-cross-service-transaction.md)
192. [同步调用和异步事件如何取舍？](../sequential/192-sync-vs-async-events.md)
193. [服务调用链过长有什么风险？](../sequential/193-long-service-call-chain-risk.md)
194. [如何避免分布式单体？](../sequential/194-avoid-distributed-monolith.md)
195. [如何识别错误的服务边界？](../sequential/195-identify-wrong-service-boundary.md)
196. [如何设计订单、库存、支付的边界？](../sequential/196-order-inventory-payment-boundary.md)
197. [搜索、推荐、广告为什么通常不放在交易主链路？](../sequential/197-search-recommendation-ads-not-main-path.md)
198. [如何处理跨团队服务契约？](../sequential/198-cross-team-service-contract.md)
199. [如何做服务依赖治理？](../sequential/199-service-dependency-governance.md)
200. [如何画服务依赖图？](../sequential/200-service-dependency-graph.md)
201. [如何评估一个服务的故障半径？](../sequential/201-service-blast-radius.md)
202. [如何做服务分级？](../sequential/202-service-tiering.md)
203. [核心链路和非核心链路如何隔离？](../sequential/203-core-noncore-isolation.md)
204. [如何设计内部运维接口？](../sequential/204-internal-ops-api-design.md)
205. [运维接口为什么不能暴露公网？](../sequential/205-ops-api-not-public.md)
206. [如何做多租户或商家隔离？](../sequential/206-multitenancy-merchant-isolation.md)
207. [如何做配置治理？](../sequential/207-configuration-governance.md)
208. [如何做灰度开关和功能开关？](../sequential/208-feature-flag-gray-switch.md)
209. [配置中心故障会带来什么影响？](../sequential/209-config-center-failure-impact.md)
210. [如何处理配置变更回滚？](../sequential/210-config-change-rollback.md)

### 02 分布式一致性和事务

题号：211-245；进度：35/35。

211. [为什么大型分布式系统通常不用全局大事务？](../sequential/211-avoid-global-distributed-transaction.md)
212. [2PC 的流程是什么？](../sequential/212-two-phase-commit-flow.md)
213. [2PC 有哪些可用性问题？](../sequential/213-two-phase-commit-availability-issues.md)
214. [3PC 解决了什么，又有什么限制？](../sequential/214-three-phase-commit-limits.md)
215. [TCC 适合哪些场景？](../sequential/215-tcc-scenarios.md)
216. [Saga 适合哪些场景？](../sequential/216-saga-scenarios.md)
217. [本地事务加 Outbox 解决什么问题？](../sequential/217-local-transaction-outbox.md)
218. [Outbox 不能解决什么问题？](../sequential/218-outbox-limitations.md)
219. [业务数据写成功但 MQ 发送失败怎么办？](../sequential/219-business-success-mq-send-failed.md)
220. [MQ 发送成功但业务事务回滚怎么办？](../sequential/220-mq-sent-business-rollback.md)
221. [消息重复投递怎么办？](../sequential/221-duplicate-message-delivery.md)
222. [消费成功但 ack 失败怎么办？](../sequential/222-consume-success-ack-failed.md)
223. [消费失败后如何重试？](../sequential/223-consume-failure-retry.md)
224. [重试多次失败如何进入死信？](../sequential/224-dead-letter-after-retry.md)
225. [死信消息如何恢复？](../sequential/225-dead-letter-recovery.md)
226. [如何保证同一订单事件顺序？](../sequential/226-order-event-ordering.md)
227. [如何处理跨 partition 乱序？](../sequential/227-cross-partition-out-of-order.md)
228. [如何设计事件版本？](../sequential/228-event-versioning.md)
229. [如何保证事件 schema 兼容？](../sequential/229-event-schema-compatibility.md)
230. [最终一致和强一致如何取舍？](../sequential/230-eventual-vs-strong-consistency.md)
231. [用户看到中间状态是否可接受？](../sequential/231-intermediate-state-user-visible.md)
232. [下单后库存预占失败怎么办？](../sequential/232-stock-reservation-failed-after-order.md)
233. [支付成功但订单更新失败怎么办？](../sequential/233-payment-success-order-update-failed.md)
234. [订单取消但库存释放失败怎么办？](../sequential/234-order-cancel-stock-release-failed.md)
235. [补偿任务如何避免重复执行？](../sequential/235-compensation-idempotency.md)
236. [补偿任务如何避免多实例并发执行？](../sequential/236-compensation-concurrency-control.md)
237. [对账和补偿有什么区别？](../sequential/237-reconciliation-vs-compensation.md)
238. [对账发现差异后如何处理？](../sequential/238-reconciliation-difference-handling.md)
239. [幂等和去重有什么区别？](../sequential/239-idempotency-vs-deduplication.md)
240. [幂等记录保留多久？](../sequential/240-idempotency-record-retention.md)
241. [幂等表无限增长怎么办？](../sequential/241-idempotency-table-growth.md)
242. [状态机如何防止非法状态跳转？](../sequential/242-state-machine-illegal-transition.md)
243. [如何设计可恢复的业务状态？](../sequential/243-recoverable-business-state.md)
244. [如何处理悬挂、空回滚和重复提交？](../sequential/244-hanging-empty-rollback-duplicate-submit.md)
245. [如何证明一致性方案是可靠的？](../sequential/245-prove-consistency-scheme-reliable.md)

### 03 高并发、限流和稳定性

题号：246-280；进度：35/35。另有题库补充题 4 道。

246. [高并发系统的瓶颈通常在哪里？](../sequential/246-high-concurrency-bottlenecks.md)
247. [QPS、TPS、并发数、响应时间之间是什么关系？](../sequential/247-qps-tps-concurrency-latency.md)
248. [如何做容量估算？](../sequential/248-capacity-estimation.md)
249. [如何估算线程池、连接池和数据库连接数？](../sequential/249-pool-size-estimation.md)
250. [如何设计限流？](../sequential/250-rate-limiting-design.md)
251. [什么是熔断？](../sequential/251-circuit-breaker.md)
252. [熔断的 CLOSED、OPEN、HALF_OPEN 如何切换？](../sequential/252-circuit-breaker-states.md)
253. [半开状态为什么要小流量探测？](../sequential/253-half-open-probing.md)
254. [熔断和限流有什么区别？](../sequential/254-circuit-breaker-vs-rate-limit.md)
255. [熔断和降级有什么区别？](../sequential/255-circuit-breaker-vs-degradation.md)
256. [哪些业务可以降级？](../sequential/256-degradable-business.md)
257. [哪些业务不能降级？](../sequential/257-non-degradable-business.md)
258. [推荐服务故障如何降级？](../sequential/258-recommendation-failure-degradation.md)
259. [价格服务故障能不能降级？](../sequential/259-price-service-failure-degradation.md)
260. [库存服务故障能不能继续下单？](../sequential/260-inventory-failure-continue-order.md)
261. [如何设置超时？](../sequential/261-timeout-design.md)
262. [如何设计重试？](../sequential/262-retry-design.md)
263. [为什么重试可能放大故障？](../sequential/263-retry-amplifies-failure.md)
264. [什么是指数退避和 jitter？](../sequential/264-exponential-backoff-jitter.md)
265. [如何做请求合并？](../sequential/265-request-coalescing.md)
266. [如何做热点隔离？](../sequential/266-hotspot-isolation.md)
267. [热点 SKU 为什么会拖垮数据库？](../sequential/267-hot-sku-database-risk.md)
268. [库存桶如何降低热点行竞争？](../sequential/268-inventory-buckets.md)
269. [秒杀系统为什么要削峰？](../sequential/269-flash-sale-traffic-shaping.md)
270. [秒杀令牌如何设计？](../sequential/270-flash-sale-token-design.md)
271. [秒杀如何防止超卖？](../sequential/271-flash-sale-oversell-prevention.md)
272. [秒杀如何防止黄牛？](../sequential/272-flash-sale-bot-prevention.md)
273. [如何保护普通下单不被秒杀拖垮？](../sequential/273-protect-normal-order-from-flash-sale.md)
274. [如何做优雅关闭？](../sequential/274-graceful-shutdown.md)
275. [服务关闭时如何处理正在执行的请求？](../sequential/275-inflight-request-on-shutdown.md)
276. [如何设计下游自动平滑恢复？](../sequential/276-downstream-smooth-recovery.md)
277. [下游恢复后为什么不能立刻放开全部流量？](../sequential/277-why-not-full-traffic-after-recovery.md)
278. [如何做预热？](../sequential/278-warmup-design.md)
279. [如何做过载保护？](../sequential/279-overload-protection.md)
280. [如何判断系统已经进入过载？](../sequential/280-overload-detection.md)

#### 题库补充题

补充 1. [固定窗口、滑动窗口、令牌桶、漏桶有什么区别？](../sequential/537-design-rate-limit-system.md)
补充 2. [网关限流和服务内限流如何配合？](../sequential/250-rate-limiting-design.md)
补充 3. [按 IP、用户、设备、SKU、商家限流有什么区别？](../sequential/537-design-rate-limit-system.md)
补充 4. [如何防止恶意用户绕过限流？](../sequential/528-design-risk-system.md)

### 04 数据库、SQL 和数据建模

题号：281-323；进度：43/43。

281. [关系型数据库适合解决什么问题？](../sequential/281-relational-database-use-cases.md)
282. [MySQL InnoDB 的索引结构是什么？](../sequential/282-innodb-index-structure.md)
283. [B+Tree 为什么适合数据库索引？](../sequential/283-bplus-tree-database-index.md)
284. [聚簇索引和二级索引有什么区别？](../sequential/284-clustered-vs-secondary-index.md)
285. [回表是什么？](../sequential/285-bookmark-lookup.md)
286. [覆盖索引是什么？](../sequential/286-covering-index.md)
287. [联合索引最左前缀是什么？](../sequential/287-leftmost-prefix.md)
288. [索引下推是什么？](../sequential/288-index-condition-pushdown.md)
289. [什么情况下索引会失效？](../sequential/289-index-invalid-cases.md)
290. [慢 SQL 如何分析？](../sequential/290-slow-sql-analysis.md)
291. [`explain` 重点看哪些字段？](../sequential/291-explain-key-fields.md)
292. [选择性低的字段是否适合建索引？](../sequential/292-low-cardinality-index.md)
293. [索引越多为什么写入越慢？](../sequential/293-too-many-indexes-slow-writes.md)
294. [如何设计订单表索引？](../sequential/294-order-table-index-design.md)
295. [如何设计支付表唯一约束？](../sequential/295-payment-table-unique-constraints.md)
296. [如何设计 Outbox 扫描索引？](../sequential/296-outbox-scan-index-design.md)
297. [事务 ACID 分别是什么？](../sequential/297-acid.md)
298. [脏读、不可重复读、幻读是什么？](../sequential/298-dirty-nonrepeatable-phantom-read.md)
299. [MVCC 是什么？](../sequential/299-mvcc.md)
300. [快照读和当前读有什么区别？](../sequential/300-snapshot-read-vs-current-read.md)
301. [间隙锁是什么？](../sequential/301-gap-lock.md)
302. [死锁如何产生？](../sequential/302-deadlock-causes.md)
303. [如何分析和避免数据库死锁？](../sequential/303-analyze-avoid-db-deadlock.md)
304. [悲观锁和乐观锁怎么选？](../sequential/304-pessimistic-vs-optimistic-lock.md)
305. [条件更新如何防止库存超卖？](../sequential/305-conditional-update-prevent-oversell.md)
306. [`select for update` 有什么风险？](../sequential/306-select-for-update-risks.md)
307. [如何设计分库分表？](../sequential/307-sharding-design.md)
308. [分片键如何选择？](../sequential/308-sharding-key-selection.md)
309. [按用户分片和按订单分片有什么区别？](../sequential/309-user-vs-order-sharding.md)
310. [分库分表后如何按用户查订单？](../sequential/310-query-orders-by-user-after-sharding.md)
311. [分库分表后如何按订单号查订单？](../sequential/311-query-order-by-order-no-after-sharding.md)
312. [全局唯一 ID 如何生成？](../sequential/312-global-unique-id.md)
313. [Snowflake ID 的结构和风险是什么？](../sequential/313-snowflake-id.md)
314. [时钟回拨如何处理？](../sequential/314-clock-backward-handling.md)
315. [数据归档如何设计？](../sequential/315-data-archiving-design.md)
316. [冷热分离如何设计？](../sequential/316-hot-cold-data-separation.md)
317. [在线 DDL 有什么风险？](../sequential/317-online-ddl-risks.md)
318. [数据库扩容如何做？](../sequential/318-database-capacity-expansion.md)
319. [读写分离有什么一致性问题？](../sequential/319-read-write-splitting-consistency.md)
320. [主从延迟如何影响业务？](../sequential/320-replication-lag-business-impact.md)
321. [如何做数据库备份和恢复演练？](../sequential/321-database-backup-restore-drill.md)
322. [ORM 能解决哪些问题，不能解决哪些问题？](../sequential/322-orm-boundaries.md)
323. [MyBatis Plus 和手写 SQL 如何取舍？](../sequential/323-mybatis-plus-vs-handwritten-sql.md)

### 05 Redis 和缓存体系

题号：324-357；进度：34/34。

324. [Redis 常用数据结构有哪些？](../sequential/324-redis-data-structures.md)
325. [String、Hash、List、Set、ZSet 分别适合什么场景？](../sequential/325-redis-structure-use-cases.md)
326. [Redis 单线程为什么还能很快？](../sequential/326-redis-single-thread-fast.md)
327. [Redis IO 多路复用是什么？](../sequential/327-redis-io-multiplexing.md)
328. [Redis 持久化 RDB 和 AOF 有什么区别？](../sequential/328-redis-rdb-vs-aof.md)
329. [Redis 主从复制如何工作？](../sequential/329-redis-replication.md)
330. [Redis Sentinel 解决什么问题？](../sequential/330-redis-sentinel.md)
331. [Redis Cluster 如何分片？](../sequential/331-redis-cluster-sharding.md)
332. [一致性哈希和 Redis Cluster slot 有什么区别？](../sequential/332-consistent-hash-vs-redis-slot.md)
333. [缓存穿透是什么？](../sequential/333-cache-penetration.md)
334. [缓存击穿是什么？](../sequential/334-cache-breakdown.md)
335. [缓存雪崩是什么？](../sequential/335-cache-avalanche.md)
336. [如何设计商品详情缓存？](../sequential/336-product-detail-cache-design.md)
337. [缓存 key 如何命名？](../sequential/337-cache-key-naming.md)
338. [TTL 如何设置？](../sequential/338-cache-ttl-design.md)
339. [TTL 为什么要加随机抖动？](../sequential/339-cache-ttl-jitter.md)
340. [更新数据库和删除缓存顺序怎么选？](../sequential/340-cache-db-update-order.md)
341. [延迟双删解决什么问题？](../sequential/341-delayed-double-delete.md)
342. [缓存和数据库短暂不一致如何接受？](../sequential/342-cache-db-temporary-inconsistency.md)
343. [热 key 如何发现？](../sequential/343-hot-key-discovery.md)
344. [热 key 如何治理？](../sequential/344-hot-key-governance.md)
345. [大 key 有什么危害？](../sequential/345-big-key-risks.md)
346. [如何拆分大 key？](../sequential/346-big-key-splitting.md)
347. [Redis 分布式锁如何实现？](../sequential/347-redis-distributed-lock.md)
348. [`SET NX PX` 有什么注意点？](../sequential/348-set-nx-px-notes.md)
349. [Lua 脚本为什么能保证原子性？](../sequential/349-lua-atomicity.md)
350. [Redisson 看门狗解决什么问题？](../sequential/350-redisson-watchdog.md)
351. [Redis 限流如何实现？](../sequential/351-redis-rate-limiting.md)
352. [Redis 计数器如何避免过期窗口问题？](../sequential/352-redis-counter-expiry-window.md)
353. [布隆过滤器适合什么场景？](../sequential/353-bloom-filter-use-cases.md)
354. [Redis 故障时系统如何降级？](../sequential/354-redis-failure-degradation.md)
355. [缓存命中率下降如何排查？](../sequential/355-cache-hit-rate-drop.md)
356. [Redis 内存淘汰策略有哪些？](../sequential/356-redis-eviction-policies.md)
357. [Redis 是否适合作为订单状态最终数据源？](../sequential/357-redis-as-order-source.md)

### 06 Kafka 和消息系统

题号：358-387；进度：30/30。

358. [Kafka 的 Topic、Partition、Replica、Broker 是什么？](../sequential/358-kafka-topic-partition-replica-broker.md)
359. [Producer 如何选择 partition？](../sequential/359-kafka-producer-partition-selection.md)
360. [Consumer Group 如何工作？](../sequential/360-kafka-consumer-group.md)
361. [offset 是什么？](../sequential/361-kafka-offset.md)
362. [消息提交 offset 的时机如何选择？](../sequential/362-offset-commit-timing.md)
363. [至少一次、至多一次、恰好一次分别是什么意思？](../sequential/363-kafka-delivery-semantics.md)
364. [Kafka 的 exactly-once 为什么不等于业务 exactly-once？](../sequential/364-kafka-eos-vs-business-eos.md)
365. [ISR 是什么？](../sequential/365-kafka-isr.md)
366. [ack=0、ack=1、ack=all 有什么区别？](../sequential/366-kafka-ack-modes.md)
367. [Producer 幂等解决什么问题？](../sequential/367-kafka-producer-idempotence.md)
368. [Kafka 事务适合什么场景？](../sequential/368-kafka-transactions.md)
369. [消息顺序如何保证？](../sequential/369-message-ordering.md)
370. [为什么顺序通常只能保证同一个 partition 内？](../sequential/370-ordering-within-partition.md)
371. [如何设计订单事件 topic？](../sequential/371-order-event-topic-design.md)
372. [事件应该携带全量数据还是只携带 ID？](../sequential/372-event-full-data-or-id.md)
373. [消息体过大有什么问题？](../sequential/373-large-message-problems.md)
374. [消费者处理慢怎么办？](../sequential/374-slow-consumer-handling.md)
375. [consumer lag 如何监控？](../sequential/375-consumer-lag-monitoring.md)
376. [消息积压如何排查？](../sequential/376-message-backlog-troubleshooting.md)
377. [增加消费者为什么不一定能解决积压？](../sequential/377-adding-consumers-not-enough.md)
378. [单分区热点如何处理？](../sequential/378-hot-partition-handling.md)
379. [重试 topic 和死信 topic 如何设计？](../sequential/379-retry-and-dead-letter-topic.md)
380. [消费者幂等表如何设计？](../sequential/380-consumer-idempotency-table.md)
381. [消费端如何保证业务写入和去重记录原子性？](../sequential/381-consumer-write-dedup-atomicity.md)
382. [消息 schema 如何演进？](../sequential/382-message-schema-evolution.md)
383. [如何处理消息反序列化失败？](../sequential/383-message-deserialization-failure.md)
384. [Kafka 和 RabbitMQ 如何取舍？](../sequential/384-kafka-vs-rabbitmq.md)
385. [MQ 故障时核心链路怎么办？](../sequential/385-mq-failure-core-flow.md)
386. [Outbox Relay 多实例如何避免重复抢事件？](../sequential/386-outbox-relay-multi-instance.md)
387. [Outbox 历史数据如何清理？](../sequential/387-outbox-history-cleanup.md)

### 07 搜索、读模型和数据同步

题号：388-403；进度：16/16。

388. [为什么电商搜索通常不用 MySQL 直接实现？](../sequential/388-why-not-mysql-search.md)
389. [倒排索引是什么？](../sequential/389-inverted-index.md)
390. [分词器如何影响搜索结果？](../sequential/390-tokenizer-impact.md)
391. [商品搜索文档应该包含哪些字段？](../sequential/391-product-search-document-fields.md)
392. [商品上下架如何同步到搜索索引？](../sequential/392-product-status-sync-to-search.md)
393. [搜索索引和商品库不一致怎么办？](../sequential/393-search-index-inconsistency.md)
394. [搜索读模型可以重建吗？](../sequential/394-search-read-model-rebuildable.md)
395. [如何设计索引重建流程？](../sequential/395-index-rebuild-flow.md)
396. [OpenSearch 分片和副本如何设置？](../sequential/396-opensearch-shards-replicas.md)
397. [搜索结果排序考虑哪些因素？](../sequential/397-search-ranking-factors.md)
398. [相关性、销量、价格、广告如何混排？](../sequential/398-relevance-sales-price-ads-mix.md)
399. [搜索服务故障如何降级？](../sequential/399-search-degradation.md)
400. [搜索延迟升高如何排查？](../sequential/400-search-latency-troubleshooting.md)
401. [商品详情缓存和搜索索引是什么关系？](../sequential/401-product-cache-vs-search-index.md)
402. [CQRS 在电商系统中如何体现？](../sequential/402-cqrs-in-ecommerce.md)
403. [读模型最终一致对用户有什么影响？](../sequential/403-read-model-eventual-consistency-impact.md)

## 03 生产工程和平台治理

### 01 安全、身份和风控

题号：404-430；进度：27/27。

404. [认证和授权有什么区别？](../sequential/404-authentication-vs-authorization.md)
405. [Session、JWT、OAuth2 如何取舍？](../sequential/405-session-jwt-oauth2-tradeoff.md)
406. [JWT 有什么风险？](../sequential/406-jwt-risks.md)
407. [token 泄露如何处理？](../sequential/407-token-leakage-handling.md)
408. [RBAC 和 ABAC 有什么区别？](../sequential/408-rbac-vs-abac.md)
409. [最小权限原则如何落地？](../sequential/409-least-privilege.md)
410. [内部运维接口如何鉴权？](../sequential/410-internal-ops-auth.md)
411. [HMAC 签名如何防篡改？](../sequential/411-hmac-signature-tamper-proof.md)
412. [nonce 如何防重放？](../sequential/412-nonce-anti-replay.md)
413. [timestamp 窗口如何设置？](../sequential/413-timestamp-window.md)
414. [签名为什么要覆盖 method、path、body hash？](../sequential/414-sign-method-path-body-hash.md)
415. [常量时间比较解决什么问题？](../sequential/415-constant-time-comparison.md)
416. [AES-GCM 适合什么场景？](../sequential/416-aes-gcm-use-cases.md)
417. [HMAC hash 为什么适合精确查询？](../sequential/417-hmac-hash-exact-query.md)
418. [手机号为什么不能明文存储？](../sequential/418-phone-not-plaintext.md)
419. [加密字段如何做密钥轮换？](../sequential/419-encrypted-field-key-rotation.md)
420. [日志脱敏如何实现？](../sequential/420-log-masking.md)
421. [SQL 注入如何防止？](../sequential/421-sql-injection-prevention.md)
422. [XSS 和 CSRF 在前后端系统中如何防护？](../sequential/422-xss-csrf-protection.md)
423. [SSRF 风险在哪里？](../sequential/423-ssrf-risk.md)
424. [开放平台如何做 appKey 和 secret 管理？](../sequential/424-appkey-secret-management.md)
425. [如何做接口权限审计？](../sequential/425-api-permission-audit.md)
426. [风控规则如何灰度上线？](../sequential/426-risk-rule-gray-release.md)
427. [账号冻结对下单链路有什么影响？](../sequential/427-account-freeze-order-impact.md)
428. [支付风控应在什么环节介入？](../sequential/428-payment-risk-intervention.md)
429. [如何设计高危操作双人审批？](../sequential/429-high-risk-dual-approval.md)
430. [如何处理数据合规删除和历史订单保留？](../sequential/430-compliance-delete-vs-order-retention.md)

### 02 可观测性、告警和故障处理

题号：431-457；进度：27/27。

431. [日志、指标、Trace 分别解决什么问题？](../sequential/431-logs-metrics-traces.md)
432. [为什么三者都需要？](../sequential/432-why-need-logs-metrics-traces.md)
433. [trace ID 如何生成和透传？](../sequential/433-trace-id-generation-propagation.md)
434. [日志中必须包含哪些业务字段？](../sequential/434-log-business-fields.md)
435. [结构化日志有什么好处？](../sequential/435-structured-logging-benefits.md)
436. [什么信息不能写入日志？](../sequential/436-what-not-to-log.md)
437. [RED 指标是什么？](../sequential/437-red-metrics.md)
438. [USE 指标是什么？](../sequential/438-use-metrics.md)
439. [核心交易链路要监控哪些业务指标？](../sequential/439-core-transaction-business-metrics.md)
440. [订单成功率下降如何排查？](../sequential/440-order-success-rate-drop.md)
441. [支付回调延迟升高如何排查？](../sequential/441-payment-callback-latency.md)
442. [库存预占失败率升高如何排查？](../sequential/442-inventory-reservation-failure-rate.md)
443. [Outbox 积压如何排查？](../sequential/443-outbox-backlog-troubleshooting.md)
444. [Kafka lag 增长如何排查？](../sequential/444-kafka-lag-growth.md)
445. [Redis 命中率下降如何排查？](../sequential/445-redis-hit-rate-drop-troubleshooting.md)
446. [数据库连接池耗尽如何排查？](../sequential/446-db-connection-pool-exhaustion.md)
447. [P99 升高但平均延迟正常说明什么？](../sequential/447-p99-high-average-normal.md)
448. [告警为什么不能太多？](../sequential/448-alert-fatigue.md)
449. [如何设计告警级别？](../sequential/449-alert-severity-design.md)
450. [SLO、SLI、SLA 有什么区别？](../sequential/450-slo-sli-sla.md)
451. [错误预算是什么？](../sequential/451-error-budget.md)
452. [burn rate 告警如何理解？](../sequential/452-burn-rate-alerting.md)
453. [Runbook 应该包含什么？](../sequential/453-runbook-content.md)
454. [事故复盘应该关注什么？](../sequential/454-incident-review-focus.md)
455. [如何区分根因和触发因素？](../sequential/455-root-cause-vs-trigger.md)
456. [如何设计一次故障演练？](../sequential/456-failure-drill-design.md)
457. [如何证明系统具备可恢复能力？](../sequential/457-prove-recoverability.md)

### 03 Kubernetes、容器和发布

题号：458-484；进度：27/27。

458. [Docker 镜像分层是什么？](../sequential/458-docker-image-layers.md)
459. [如何构建小而安全的 Java 镜像？](../sequential/459-small-secure-java-image.md)
460. [为什么生产容器不建议 root 用户运行？](../sequential/460-non-root-containers.md)
461. [Kubernetes Deployment、Service、Ingress 分别是什么？](../sequential/461-k8s-deployment-service-ingress.md)
462. [ConfigMap 和 Secret 有什么区别？](../sequential/462-configmap-vs-secret.md)
463. [readinessProbe、livenessProbe、startupProbe 如何设计？](../sequential/463-k8s-probe-design.md)
464. [为什么 liveness 不应该强依赖所有下游？](../sequential/464-liveness-not-dependent-on-downstreams.md)
465. [HPA 根据什么指标扩缩容？](../sequential/465-hpa-metrics.md)
466. [requests 和 limits 如何设置？](../sequential/466-k8s-requests-limits.md)
467. [CPU limit 对 Java 服务有什么影响？](../sequential/467-cpu-limit-impact-on-java.md)
468. [PodDisruptionBudget 解决什么问题？](../sequential/468-pod-disruption-budget.md)
469. [滚动升级如何保证可用性？](../sequential/469-rolling-upgrade-availability.md)
470. [优雅关闭如何配置？](../sequential/470-graceful-shutdown-config.md)
471. [服务发现如何工作？](../sequential/471-service-discovery.md)
472. [Ingress 和 API Gateway 有什么区别？](../sequential/472-ingress-vs-api-gateway.md)
473. [NetworkPolicy 解决什么问题？](../sequential/473-network-policy.md)
474. [多可用区部署要考虑什么？](../sequential/474-multi-az-deployment.md)
475. [蓝绿发布和金丝雀发布有什么区别？](../sequential/475-blue-green-vs-canary.md)
476. [灰度发布观察哪些指标？](../sequential/476-canary-metrics.md)
477. [回滚前为什么要考虑数据库兼容？](../sequential/477-rollback-db-compatibility.md)
478. [数据库迁移如何配合应用发布？](../sequential/478-db-migration-with-release.md)
479. [什么是 expand-contract 发布模式？](../sequential/479-expand-contract-release.md)
480. [配置错误导致故障如何快速回滚？](../sequential/480-config-rollback.md)
481. [如何设计生产 Secret 管理？](../sequential/481-production-secret-management.md)
482. [Service Mesh 解决什么问题？](../sequential/482-service-mesh.md)
483. [mTLS 在服务间调用中有什么价值？](../sequential/483-mtls-value.md)
484. [什么时候不应该引入 Service Mesh？](../sequential/484-when-not-to-use-service-mesh.md)

### 04 测试、质量和工程治理

题号：485-513；进度：29/29。

485. [单元测试、集成测试、端到端测试有什么区别？](../sequential/485-unit-integration-e2e-tests.md)
486. [哪些逻辑必须有单元测试？](../sequential/486-logic-needs-unit-tests.md)
487. [哪些逻辑必须有集成测试？](../sequential/487-logic-needs-integration-tests.md)
488. [Mock 的优点和风险是什么？](../sequential/488-mock-benefits-risks.md)
489. [Testcontainers 适合验证什么？](../sequential/489-testcontainers-use-cases.md)
490. [为什么只测 happy path 不够？](../sequential/490-happy-path-not-enough.md)
491. [如何测试幂等？](../sequential/491-test-idempotency.md)
492. [如何测试库存防超卖？](../sequential/492-test-inventory-oversell.md)
493. [如何测试支付重复回调？](../sequential/493-test-payment-duplicate-callback.md)
494. [如何测试 Outbox 重试？](../sequential/494-test-outbox-retry.md)
495. [如何测试 MQ 重复消费？](../sequential/495-test-mq-duplicate-consumption.md)
496. [如何测试补偿任务？](../sequential/496-test-compensation-job.md)
497. [如何测试限流和熔断？](../sequential/497-test-rate-limit-circuit-breaker.md)
498. [如何测试降级逻辑？](../sequential/498-test-degradation.md)
499. [Smoke 测试应该覆盖哪些链路？](../sequential/499-smoke-test-chains.md)
500. [压测前要准备什么？](../sequential/500-before-load-test.md)
501. [压测结果如何分析？](../sequential/501-analyze-load-test-results.md)
502. [如何定位压测瓶颈？](../sequential/502-find-load-test-bottleneck.md)
503. [回归测试如何分层？](../sequential/503-layered-regression-tests.md)
504. [CI 流水线应该包含哪些阶段？](../sequential/504-ci-pipeline-stages.md)
505. [Checkstyle、SpotBugs、PMD 分别解决什么问题？](../sequential/505-checkstyle-spotbugs-pmd.md)
506. [代码覆盖率高是否代表质量高？](../sequential/506-code-coverage-quality.md)
507. [如何做代码评审？](../sequential/507-code-review.md)
508. [如何评审分布式一致性相关代码？](../sequential/508-review-distributed-consistency-code.md)
509. [如何设计公共模块，避免 common 变成垃圾桶？](../sequential/509-common-module-design.md)
510. [如何管理依赖版本？](../sequential/510-dependency-version-management.md)
511. [如何处理依赖漏洞？](../sequential/511-handle-dependency-vulnerabilities.md)
512. [如何设计兼容性测试？](../sequential/512-compatibility-tests.md)
513. [如何保证文档和代码一致？](../sequential/513-docs-code-consistency.md)

## 04 系统设计

### 01 电商核心系统设计

题号：514-530；进度：17/17。

514. [设计一个京东/Amazon 类电商系统。](../sequential/514-design-jd-amazon-ecommerce.md)
515. [设计下单系统。](../sequential/515-design-order-system.md)
516. [设计库存系统。](../sequential/516-design-inventory-system.md)
517. [设计支付系统。](../sequential/517-design-payment-system.md)
518. [设计购物车系统。](../sequential/518-design-cart-system.md)
519. [设计商品详情系统。](../sequential/519-design-product-detail-system.md)
520. [设计商品搜索系统。](../sequential/520-design-product-search-system.md)
521. [设计秒杀系统。](../sequential/521-design-flash-sale-system.md)
522. [设计优惠券系统。](../sequential/522-design-coupon-system.md)
523. [设计价格服务。](../sequential/523-design-pricing-system.md)
524. [设计履约系统。](../sequential/524-design-fulfillment-system.md)
525. [设计售后退款系统。](../sequential/525-design-after-sales-refund.md)
526. [设计商家入驻和商品审核系统。](../sequential/526-design-merchant-onboarding-review.md)
527. [设计开放平台 API。](../sequential/527-design-openapi.md)
528. [设计风控系统。](../sequential/528-design-risk-system.md)
529. [设计推荐系统的在线服务部分。](../sequential/529-design-recommendation-online.md)
530. [设计广告投放系统的核心链路。](../sequential/530-design-ad-delivery.md)

### 02 平台和基础设施设计

题号：531-541；进度：11/11。

531. [设计订单对账系统。](../sequential/531-design-order-reconciliation.md)
532. [设计支付渠道对账系统。](../sequential/532-design-payment-channel-reconciliation.md)
533. [设计消息重放平台。](../sequential/533-design-message-replay-platform.md)
534. [设计内部运维补偿平台。](../sequential/534-design-internal-compensation-platform.md)
535. [设计分布式 ID 服务。](../sequential/535-design-distributed-id.md)
536. [设计配置中心。](../sequential/536-design-config-center.md)
537. [设计限流系统。](../sequential/537-design-rate-limit-system.md)
538. [设计熔断降级平台。](../sequential/538-design-circuit-degrade-platform.md)
539. [设计灰度发布平台。](../sequential/539-design-canary-release-platform.md)
540. [设计日志采集和查询平台。](../sequential/540-design-log-collection-query.md)
541. [设计指标和告警平台。](../sequential/541-design-metrics-alerting.md)

### 03 大规模容量和多活设计

题号：542-554；进度：13/13。

542. [设计链路追踪系统。](../sequential/542-system-answer.md)
543. [设计多区域电商系统。](../sequential/543-system-answer.md)
544. [设计异地多活订单系统。](../sequential/544-system-answer.md)
545. [设计数据归档系统。](../sequential/545-system-answer.md)
546. [设计大促容量保障方案。](../sequential/546-system-answer.md)
547. [设计热点 SKU 保护方案。](../sequential/547-system-answer.md)
548. [设计商品缓存系统。](../sequential/548-system-answer.md)
549. [设计 Kafka Outbox 可靠事件系统。](../sequential/549-system-answer.md)
550. [设计面向 10 亿用户的用户中心。](../sequential/550-system-answer.md)
551. [设计支持 100 万峰值并发的网关。](../sequential/551-system-answer.md)
552. [设计支持 10 万下单 QPS 的交易链路。](../sequential/552-system-answer.md)
553. [设计一次数据库扩容和迁移方案。](../sequential/553-system-answer.md)
554. [设计一次从单体到微服务的演进方案。](../sequential/554-system-answer.md)

## 05 生产事故和排障

### 01 核心交易事故

题号：555-563；进度：9/9。

555. [下单成功率突然下降，你怎么排查？](../sequential/555-incident-answer.md)
556. [支付成功但订单未变成已支付，怎么处理？](../sequential/556-incident-answer.md)
557. [库存出现少量超卖，怎么定位和修复？](../sequential/557-incident-answer.md)
558. [库存预占记录大量过期未释放，怎么恢复？](../sequential/558-incident-answer.md)
559. [Outbox 表积压大量待发送事件，怎么处理？](../sequential/559-incident-answer.md)
560. [Kafka 某个 topic lag 快速增长，怎么排查？](../sequential/560-incident-answer.md)
561. [Redis 集群抖动，商品详情接口 P99 升高，怎么处理？](../sequential/561-incident-answer.md)
562. [数据库 CPU 100%，你先看什么？](../sequential/562-incident-answer.md)
563. [数据库连接池耗尽，如何止血？](../sequential/563-incident-answer.md)

### 02 发布、中间件和运行时事故

题号：564-572；进度：9/9。

564. [某个新版本发布后错误率上升，如何判断是否回滚？](../sequential/564-incident-answer.md)
565. [灰度 5% 正常，放量 50% 后异常，可能是什么原因？](../sequential/565-incident-answer.md)
566. [一个 Pod 频繁重启，如何排查？](../sequential/566-incident-answer.md)
567. [JVM Full GC 频繁，如何处理？](../sequential/567-incident-answer.md)
568. [线程数暴涨，如何定位？](../sequential/568-incident-answer.md)
569. [线上死锁如何处理？](../sequential/569-incident-answer.md)
570. [支付渠道重复回调导致大量冲突日志，是否是事故？](../sequential/570-incident-answer.md)
571. [对账发现渠道成功本地失败，怎么修复？](../sequential/571-incident-answer.md)
572. [用户投诉重复扣款，如何排查？](../sequential/572-incident-answer.md)

### 03 配置、安全、观测和数据修复

题号：573-579；进度：7/7。

573. [秒杀开始后普通下单也变慢，怎么止血？](../sequential/573-incident-answer.md)
574. [搜索结果大量缺商品，怎么恢复？](../sequential/574-incident-answer.md)
575. [商品价格显示旧值，如何排查缓存一致性问题？](../sequential/575-incident-answer.md)
576. [配置中心推错配置，如何回滚？](../sequential/576-incident-answer.md)
577. [Secret 泄露后如何应急？](../sequential/577-incident-answer.md)
578. [日志系统故障是否会影响交易链路？](../sequential/578-incident-answer.md)
579. [监控告警误报太多，如何治理？](../sequential/579-incident-answer.md)

### 04 大促、容量和恢复决策

题号：580-584；进度：5/5。

580. [生产数据库误删数据，如何恢复？](../sequential/580-incident-answer.md)
581. [消费者 bug 导致错误写入大量数据，如何修复？](../sequential/581-incident-answer.md)
582. [下游恢复后流量全部放开又被打挂，如何避免？](../sequential/582-incident-answer.md)
583. [大促前你会做哪些检查？](../sequential/583-incident-answer.md)
584. [大促中核心指标异常，你如何决策降级？](../sequential/584-incident-answer.md)

## 06 架构取舍和高级追问

### 01 架构演进和技术选型

题号：585-594；进度：10/10。

585. [你为什么选择微服务而不是单体？](../sequential/585-tradeoff-answer.md)
586. [你为什么选择本地事务加 Outbox，而不是 TCC？](../sequential/586-tradeoff-answer.md)
587. [你为什么不使用全局分布式事务？](../sequential/587-tradeoff-answer.md)
588. [你为什么用 Kafka，而不是 RabbitMQ？](../sequential/588-tradeoff-answer.md)
589. [你为什么用 Redis 缓存，而不是只靠数据库？](../sequential/589-tradeoff-answer.md)
590. [你为什么用 OpenSearch，而不是 MySQL like 查询？](../sequential/590-tradeoff-answer.md)
591. [你为什么要做库存预占，而不是支付时再扣库存？](../sequential/591-tradeoff-answer.md)
592. [你为什么要保存价格快照？](../sequential/592-tradeoff-answer.md)
593. [你为什么要做支付流水，而不是只更新支付状态？](../sequential/593-tradeoff-answer.md)
594. [你为什么要做对账？](../sequential/594-tradeoff-answer.md)

### 02 一致性、成本和技术债

题号：595-604；进度：10/10。

595. [你为什么要做补偿任务，而不是人工处理所有异常？](../sequential/595-tradeoff-answer.md)
596. [你为什么要做幂等表，而不是只在代码里判断状态？](../sequential/596-tradeoff-answer.md)
597. [你为什么要引入 Kubernetes？](../sequential/597-tradeoff-answer.md)
598. [你为什么要拆 common？](../sequential/598-tradeoff-answer.md)
599. [你如何避免 common 变成强耦合中心？](../sequential/599-tradeoff-answer.md)
600. [你如何判断一个方案过度设计？](../sequential/600-tradeoff-answer.md)
601. [你如何在交付速度和架构质量之间取舍？](../sequential/601-tradeoff-answer.md)
602. [你如何在一致性和可用性之间取舍？](../sequential/602-tradeoff-answer.md)
603. [你如何在成本和性能之间取舍？](../sequential/603-tradeoff-answer.md)
604. [如果只能做三件事提升稳定性，你做什么？](../sequential/604-tradeoff-answer.md)

### 03 团队协作和工程质量

题号：605-610；进度：6/6。

605. [如果只能做三件事提升吞吐，你做什么？](../sequential/605-tradeoff-answer.md)
606. [如果只能做三件事降低成本，你做什么？](../sequential/606-tradeoff-answer.md)
607. [如果让你重构当前系统，你优先改什么？](../sequential/607-tradeoff-answer.md)
608. [当前系统最大的技术债是什么？](../sequential/608-tradeoff-answer.md)
609. [当前系统最大的生产风险是什么？](../sequential/609-tradeoff-answer.md)
610. [你如何证明这个系统不是玩具项目？](../sequential/610-tradeoff-answer.md)

## 07 Amazon L6 行为面试

### 01 STAR 和 Leadership Principles

题号：611-622；进度：12/12。

611. [讲一次你主导复杂系统设计的经历。](../sequential/611-behavior-answer.md)
612. [讲一次你在需求模糊时如何拆解问题。](../sequential/612-behavior-answer.md)
613. [讲一次你发现并修复深层技术问题的经历。](../sequential/613-behavior-answer.md)
614. [讲一次你推动团队采用更好工程实践的经历。](../sequential/614-behavior-answer.md)
615. [讲一次你和别人技术意见不一致时如何处理。](../sequential/615-behavior-answer.md)
616. [讲一次你做出技术取舍的经历。](../sequential/616-behavior-answer.md)
617. [讲一次你为了长期质量牺牲短期速度的经历。](../sequential/617-behavior-answer.md)
618. [讲一次你为了业务交付接受技术债的经历。](../sequential/618-behavior-answer.md)
619. [讲一次你处理线上事故的经历。](../sequential/619-behavior-answer.md)
620. [讲一次你在事故后推动系统性改进的经历。](../sequential/620-behavior-answer.md)
621. [讲一次你用数据证明方案有效的经历。](../sequential/621-behavior-answer.md)
622. [讲一次你降低系统成本的经历。](../sequential/622-behavior-answer.md)

### 02 影响力、取舍和跨团队推动

题号：623-635；进度：13/13。

623. [讲一次你提升系统可用性的经历。](../sequential/623-behavior-answer.md)
624. [讲一次你提升性能或容量的经历。](../sequential/624-behavior-answer.md)
625. [讲一次你 mentor 其他工程师的经历。](../sequential/625-behavior-answer.md)
626. [讲一次你影响多个团队的经历。](../sequential/626-behavior-answer.md)
627. [讲一次你面对失败项目如何复盘。](../sequential/627-behavior-answer.md)
628. [讲一次你主动承担超出职责范围工作的经历。](../sequential/628-behavior-answer.md)
629. [讲一次你拒绝不合理方案的经历。](../sequential/629-behavior-answer.md)
630. [讲一次你把复杂问题讲清楚给非技术人员的经历。](../sequential/630-behavior-answer.md)
631. [讲一次你坚持高标准的经历。](../sequential/631-behavior-answer.md)
632. [讲一次你快速学习陌生领域并交付的经历。](../sequential/632-behavior-answer.md)
633. [讲一次你处理安全或合规风险的经历。](../sequential/633-behavior-answer.md)
634. [讲一次你没有足够资源但仍然交付结果的经历。](../sequential/634-behavior-answer.md)
635. [讲一次你做长期架构演进规划的经历。](../sequential/635-behavior-answer.md)

## 08 现场编码和设计实现

### 01 基础数据结构和算法

题号：636-644；进度：9/9。

636. [手写 LRU Cache。](../sequential/636-coding-answer.md)
637. [手写令牌桶限流器。](../sequential/637-coding-answer.md)
638. [手写滑动窗口限流器。](../sequential/638-coding-answer.md)
639. [手写线程安全单例。](../sequential/639-coding-answer.md)
640. [手写阻塞队列。](../sequential/640-coding-answer.md)
641. [手写生产者消费者模型。](../sequential/641-coding-answer.md)
642. [手写简化线程池。](../sequential/642-coding-answer.md)
643. [手写延迟队列。](../sequential/643-coding-answer.md)
644. [手写重试工具，支持指数退避和 jitter。](../sequential/644-coding-answer.md)

### 02 后端基础组件实现

题号：645-654；进度：10/10。

645. [手写熔断器状态机。](../sequential/645-coding-answer.md)
646. [手写幂等处理器。](../sequential/646-coding-answer.md)
647. [手写订单状态机。](../sequential/647-coding-answer.md)
648. [手写库存条件扣减逻辑。](../sequential/648-coding-answer.md)
649. [手写 Outbox Relay 核心逻辑。](../sequential/649-coding-answer.md)
650. [手写 MQ 消费端去重逻辑。](../sequential/650-coding-answer.md)
651. [手写 HMAC 签名校验。](../sequential/651-coding-answer.md)
652. [手写敏感字段脱敏工具。](../sequential/652-coding-answer.md)
653. [手写统一异常处理。](../sequential/653-coding-answer.md)
654. [手写分页查询接口。](../sequential/654-coding-answer.md)

### 03 分布式可靠性组件实现

题号：655-663；进度：9/9。

655. [手写分布式 ID 生成器简化版。](../sequential/655-coding-answer.md)
656. [手写一致性 hash。](../sequential/656-coding-answer.md)
657. [手写 Top K 统计。](../sequential/657-coding-answer.md)
658. [手写限时任务执行器。](../sequential/658-coding-answer.md)
659. [手写一个可关闭的后台 worker。](../sequential/659-coding-answer.md)
660. [手写 SQL 查询最近 30 天每天下单量。](../sequential/660-coding-answer.md)
661. [手写 SQL 查询支付对账差异。](../sequential/661-coding-answer.md)
662. [手写 SQL 找出重复 request_id。](../sequential/662-coding-answer.md)
663. [手写 Java 代码解析并聚合日志错误码。](../sequential/663-coding-answer.md)

## 09 反问面试官

### 01 团队质量判断

题号：664-668；进度：5/5。

664. [这个团队负责的核心业务指标是什么？](../sequential/664-reverse-answer.md)
665. [当前系统最大的稳定性挑战是什么？](../sequential/665-reverse-answer.md)
666. [团队如何做 on-call 和事故复盘？](../sequential/666-reverse-answer.md)
667. [服务规模大概是多少 QPS、数据量和实例数？](../sequential/667-reverse-answer.md)
668. [团队使用哪些技术栈和部署平台？](../sequential/668-reverse-answer.md)

### 02 业务和成长空间判断

题号：669-673；进度：5/5。

669. [当前系统是一体化架构还是微服务架构？](../sequential/669-reverse-answer.md)
670. [团队如何做灰度、回滚和容量评估？](../sequential/670-reverse-answer.md)
671. [团队如何衡量 Senior/L6 工程师的影响力？](../sequential/671-reverse-answer.md)
672. [新人加入后前三个月通常会负责什么？](../sequential/672-reverse-answer.md)
673. [团队当前最需要解决的技术债是什么？](../sequential/673-reverse-answer.md)

## 使用建议

- 基础不稳时先读 01、02、03，补齐 Java 后端和分布式底层能力。
- 面系统设计时重点读 04，并把每题按容量、数据流、一致性、降级和观测来讲。
- 面生产经验时重点读 05，把回答组织成止血、定位、恢复、复盘四段。
- 面高级岗位时重点读 06 和 07，体现判断力、影响力和跨团队推动能力。
- 面现场编码时重点读 08，先实现小而正确的核心逻辑，再补并发和异常场景。
