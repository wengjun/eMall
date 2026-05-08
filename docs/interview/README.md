# 按分类学习面试题

这是面试题的唯一学习入口。本目录按“一级能力域 -> 二级专题 -> 具体题目”的方式组织面试题，方便按专题复习。
所有题目均已链接到逐题精讲文件；补充题会链接到最相关的已完成答案。
题号以逐题精讲目录为准；少量算法补充题会链接到最相关的逐题精讲答案。

## 01 Java 后端基础

### 01 Java 语言和工程基础

题号：001-040；进度：40/40。

001. [Java 17 相比 Java 8 有哪些重要变化？](answers/001-java-17-vs-java-8.md)
002. [`record` 适合哪些场景，不适合哪些场景？](answers/002-record-use-cases.md)
003. [`var` 会不会影响可读性，团队中如何约束？](answers/003-var-readability.md)
004. [`switch` 表达式相比传统 `switch` 有什么优势？](answers/004-switch-expression.md)
005. [`sealed class` 适合建模哪些业务场景？](answers/005-sealed-class.md)
006. [`Optional` 应该用在返回值、参数还是字段上？](answers/006-optional-usage.md)
007. [为什么金额不能用 `double`？](answers/007-money-double.md)
008. [`BigDecimal` 的 `equals` 和 `compareTo` 有什么区别？](answers/008-bigdecimal-equals-compareto.md)
009. [Java 时间 API 中 `Instant`、`LocalDateTime`、`ZonedDateTime` 怎么选？](answers/009-java-time-api.md)
010. [服务端为什么建议统一存储 UTC 时间？](answers/010-utc-storage.md)
011. [枚举适合表达哪些业务状态？](answers/011-enum-business-status.md)
012. [枚举状态扩展时如何保证兼容？](answers/012-enum-compatibility.md)
013. [面向对象中的封装在业务系统里具体体现在哪里？](answers/013-encapsulation.md)
014. [组合和继承如何取舍？](answers/014-composition-vs-inheritance.md)
015. [领域对象和 DTO 为什么要分开？](answers/015-domain-object-vs-dto.md)
016. [贫血模型和充血模型各有什么优缺点？](answers/016-anemic-rich-domain.md)
017. [如何避免所有业务逻辑堆在 Controller？](answers/017-avoid-fat-controller.md)
018. [如何设计清晰的包结构？](answers/018-package-structure.md)
019. [Java 异常分为哪些类型？](answers/019-java-exception-types.md)
020. [业务异常和系统异常应该如何区分？](answers/020-business-vs-system-exception.md)
021. [checked exception 和 unchecked exception 如何取舍？](answers/021-checked-vs-unchecked-exception.md)
022. [为什么不能直接把异常堆栈返回给前端？](answers/022-hide-stacktrace-from-client.md)
023. [如何设计统一错误码？](answers/023-error-code-design.md)
024. [错误码如何兼容多语言和多端？](answers/024-error-code-i18n.md)
025. [泛型擦除是什么？](answers/025-generic-type-erasure.md)
026. [泛型通配符 `extends` 和 `super` 怎么理解？](answers/026-generics-extends-super.md)
027. [`equals` 和 `hashCode` 的契约是什么？](answers/027-equals-hashcode-contract.md)
028. [为什么可变对象不适合作为 `HashMap` 的 key？](answers/028-mutable-hashmap-key.md)
029. [`ArrayList`、`LinkedList`、`HashMap`、`TreeMap` 分别适合什么场景？](answers/029-java-collections-choice.md)
030. [`ConcurrentHashMap` 为什么比 `Hashtable` 更适合并发场景？](answers/030-concurrenthashmap-vs-hashtable.md)
031. [Java 反射的成本和风险是什么？](answers/031-reflection-cost-risk.md)
032. [注解是如何在运行时生效的？](answers/032-annotation-runtime.md)
033. [SPI 机制适合解决什么扩展问题？](answers/033-spi-extension.md)
034. [如何设计一个可扩展的插件机制？](answers/034-plugin-mechanism.md)
035. [为什么工程代码要重视不可变对象？](answers/035-immutable-objects.md)
036. [如何判断一段代码是否可测试？](answers/036-testable-code.md)
037. [如何设计稳定的公共库 API？](answers/037-stable-public-api.md)
038. [公共库升级如何保证向后兼容？](answers/038-public-library-compatibility.md)
039. [为什么大型项目要限制循环依赖？](answers/039-cyclic-dependencies.md)
040. [如何做模块边界和依赖方向治理？](answers/040-module-boundary-governance.md)

### 02 JVM 和性能诊断

题号：041-077；进度：37/37。

041. [JVM 内存区域包括哪些？](answers/041-jvm-memory-areas.md)
042. [堆、栈、方法区、直接内存分别存什么？](answers/042-heap-stack-metaspace-direct-memory.md)
043. [对象从创建到回收大致经历什么过程？](answers/043-object-lifecycle.md)
044. [GC Roots 包括哪些？](answers/044-gc-roots.md)
045. [Minor GC、Major GC、Full GC 有什么区别？](answers/045-minor-major-full-gc.md)
046. [G1、ZGC、Shenandoah 的设计目标有什么不同？](answers/046-g1-zgc-shenandoah.md)
047. [为什么低延迟服务要关注 GC 暂停？](answers/047-low-latency-gc-pause.md)
048. [如何判断线上服务是否存在内存泄漏？](answers/048-detect-memory-leak.md)
049. [`OutOfMemoryError` 常见类型有哪些？](answers/049-oome-types.md)
050. [堆 OOM 和直接内存 OOM 如何区分？](answers/050-heap-vs-direct-oom.md)
051. [线程数过多会带来什么问题？](answers/051-too-many-threads.md)
052. [`jstack` 可以定位哪些问题？](answers/052-jstack-diagnostics.md)
053. [`jmap`、`jcmd`、JFR 分别适合什么场景？](answers/053-jmap-jcmd-jfr.md)
054. [如何分析 CPU 飙高？](answers/054-analyze-high-cpu.md)
055. [如何分析接口 P99 突然升高？](answers/055-analyze-p99-spike.md)
056. [如何判断是 GC、数据库、锁竞争还是下游慢导致延迟升高？](answers/056-gc-db-lock-downstream-latency.md)
057. [Java 服务启动慢可能有哪些原因？](answers/057-java-service-slow-startup.md)
058. [类加载机制是什么？](answers/058-class-loading-mechanism.md)
059. [双亲委派模型解决什么问题？](answers/059-parent-delegation.md)
060. [什么场景需要自定义 ClassLoader？](answers/060-custom-classloader.md)
061. [JIT 编译是什么？](answers/061-jit-compilation.md)
062. [热点代码和解释执行有什么区别？](answers/062-hot-code-vs-interpretation.md)
063. [逃逸分析有什么作用？](answers/063-escape-analysis.md)
064. [对象分配为什么通常很快？](answers/064-fast-object-allocation.md)
065. [为什么频繁创建短生命周期对象不一定总是坏事？](answers/065-short-lived-objects.md)
066. [如何减少不必要的对象分配？](answers/066-reduce-object-allocation.md)
067. [如何设置生产环境 JVM 参数？](answers/067-production-jvm-options.md)
068. [容器环境下 JVM 如何感知内存限制？](answers/068-jvm-container-memory.md)
069. [`-Xmx` 设置过大或过小分别有什么风险？](answers/069-xmx-too-large-or-small.md)
070. [线上是否应该主动调用 `System.gc()`？](answers/070-system-gc-production.md)
071. [如何做 JVM 指标监控？](answers/071-jvm-metrics-monitoring.md)
072. [需要重点监控哪些 JVM 指标？](answers/072-key-jvm-metrics.md)
073. [GC 日志如何阅读？](answers/073-read-gc-log.md)
074. [线程池队列堆积和 JVM 内存上涨有什么关系？](answers/074-threadpool-queue-memory.md)
075. [如何定位死锁？](answers/075-diagnose-deadlock.md)
076. [如何定位锁竞争？](answers/076-diagnose-lock-contention.md)
077. [如何设计一次 Java 服务压测和性能剖析？](answers/077-java-loadtest-profiling.md)

### 03 Java 并发和线程池

题号：078-114；进度：37/37。

078. [线程和进程有什么区别？](answers/078-thread-vs-process.md)
079. [Java 线程状态有哪些？](answers/079-java-thread-states.md)
080. [`synchronized` 的原理是什么？](answers/080-synchronized-principle.md)
081. [偏向锁、轻量级锁、重量级锁是什么？](answers/081-lock-upgrade.md)
082. [`ReentrantLock` 和 `synchronized` 怎么选？](answers/082-reentrantlock-vs-synchronized.md)
083. [公平锁和非公平锁有什么区别？](answers/083-fair-vs-nonfair-lock.md)
084. [`volatile` 解决什么问题，不能解决什么问题？](answers/084-volatile.md)
085. [happens-before 规则是什么？](answers/085-happens-before.md)
086. [Java 内存模型解决什么问题？](answers/086-java-memory-model.md)
087. [CAS 是什么？](answers/087-cas.md)
088. [ABA 问题是什么，如何解决？](answers/088-aba-problem.md)
089. [`AtomicInteger` 和 `LongAdder` 如何取舍？](answers/089-atomicinteger-vs-longadder.md)
090. [`CountDownLatch`、`CyclicBarrier`、`Semaphore` 分别适合什么场景？](answers/090-latch-barrier-semaphore.md)
091. [`CompletableFuture` 如何处理异步编排？](answers/091-completablefuture-async-composition.md)
092. [`CompletableFuture` 默认线程池有什么风险？](answers/092-completablefuture-default-pool-risk.md)
093. [为什么生产代码不能随意使用公共 ForkJoinPool？](answers/093-avoid-common-forkjoinpool.md)
094. [线程池核心参数如何设置？](answers/094-threadpool-core-parameters.md)
095. [CPU 密集型和 IO 密集型线程池如何估算大小？](answers/095-cpu-io-threadpool-size.md)
096. [线程池队列应该用有界还是无界？](answers/096-bounded-vs-unbounded-queue.md)
097. [拒绝策略怎么选？](answers/097-rejection-policy.md)
098. [如何避免线程池雪崩？](answers/098-avoid-threadpool-avalanche.md)
099. [多个下游服务是否应该共享同一个线程池？](answers/099-share-threadpool-downstreams.md)
100. [什么是线程池隔离？](answers/100-threadpool-isolation.md)
101. [什么是舱壁隔离？](answers/101-bulkhead-isolation.md)
102. [任务超时后线程是否真的停止？](answers/102-timeout-does-not-stop-thread.md)
103. [Java 中断机制如何正确使用？](answers/103-java-interruption.md)
104. [如何设计可取消的异步任务？](answers/104-cancellable-async-task.md)
105. [如何避免死锁？](answers/105-avoid-deadlock.md)
106. [如何减少锁粒度？](answers/106-reduce-lock-granularity.md)
107. [分段锁和库存桶有什么相似点？](answers/107-segment-lock-and-stock-bucket.md)
108. [单机锁为什么不能解决多实例并发？](answers/108-local-lock-not-for-multi-instance.md)
109. [分布式锁适合哪些场景？](answers/109-distributed-lock-use-cases.md)
110. [分布式锁有哪些风险？](answers/110-distributed-lock-risks.md)
111. [Redlock 争议是什么？](answers/111-redlock-controversy.md)
112. [为什么数据库唯一键通常比分布式锁更可靠？](answers/112-db-unique-key-vs-distributed-lock.md)
113. [并发下如何实现只执行一次？](answers/113-execute-once-concurrency.md)
114. [如何设计幂等和并发安全的组合方案？](answers/114-idempotency-and-concurrency.md)

### 04 Spring Boot 和 Spring Cloud

题号：115-149；进度：35/35。

115. [Spring Boot 自动配置原理是什么？](answers/115-spring-boot-auto-configuration.md)
116. [`@SpringBootApplication` 包含哪些注解？](answers/116-springbootapplication.md)
117. [Bean 的生命周期是什么？](answers/117-spring-bean-lifecycle.md)
118. [构造函数注入、字段注入、Setter 注入如何取舍？](answers/118-injection-styles.md)
119. [为什么推荐构造函数注入？](answers/119-why-constructor-injection.md)
120. [Spring AOP 的代理机制是什么？](answers/120-spring-aop-proxy.md)
121. [JDK 动态代理和 CGLIB 有什么区别？](answers/121-jdk-proxy-vs-cglib.md)
122. [`@Transactional` 为什么有时不生效？](answers/122-transactional-not-effective.md)
123. [自调用为什么绕过事务代理？](answers/123-self-invocation-bypass-transaction.md)
124. [事务传播行为有哪些？](answers/124-transaction-propagation.md)
125. [`REQUIRED`、`REQUIRES_NEW`、`NESTED` 有什么区别？](answers/125-required-requires-new-nested.md)
126. [事务隔离级别如何配置？](answers/126-transaction-isolation-config.md)
127. [事务里调用远程服务有什么风险？](answers/127-remote-call-in-transaction-risk.md)
128. [Controller、Service、Repository 的职责边界是什么？](answers/128-controller-service-repository-boundary.md)
129. [`@ControllerAdvice` 如何做统一异常处理？](answers/129-controller-advice.md)
130. [Bean Validation 适合做哪些校验？](answers/130-bean-validation.md)
131. [参数校验和业务校验如何区分？](answers/131-parameter-vs-business-validation.md)
132. [Spring 配置加载优先级是什么？](answers/132-spring-config-priority.md)
133. [profile、环境变量、配置中心如何配合？](answers/133-profile-env-config-center.md)
134. [如何安全管理生产密钥？](answers/134-production-secret-management.md)
135. [Actuator 暴露哪些端点比较合理？](answers/135-actuator-endpoints.md)
136. [健康检查应该包含哪些内容？](answers/136-health-check-design.md)
137. [readiness 和 liveness 在 Spring 中如何实现？](answers/137-readiness-liveness-spring.md)
138. [RestClient、WebClient、Feign 如何取舍？](answers/138-restclient-webclient-feign.md)
139. [阻塞式和响应式调用如何取舍？](answers/139-blocking-vs-reactive.md)
140. [WebFlux 是否一定比 MVC 性能更高？](answers/140-webflux-vs-mvc-performance.md)
141. [如何设置 HTTP 客户端连接池？](answers/141-http-client-connection-pool.md)
142. [如何设置连接超时、读取超时和总超时？](answers/142-http-timeouts.md)
143. [如何透传 trace ID？](answers/143-trace-id-propagation.md)
144. [如何设计统一的内部服务调用规范？](answers/144-internal-service-call-standard.md)
145. [Spring Cache 的使用边界是什么？](answers/145-spring-cache-boundary.md)
146. [Spring 事件和 MQ 事件有什么区别？](answers/146-spring-event-vs-mq.md)
147. [如何避免 Bean 循环依赖？](answers/147-avoid-bean-circular-dependency.md)
148. [如何设计 starter 或 auto-configuration？](answers/148-design-starter-auto-configuration.md)
149. [如何在多模块项目中复用公共配置？](answers/149-reuse-common-config-in-multi-module.md)

### 05 API 设计和接口治理

题号：150-183；进度：34/34。

150. [REST API 的资源建模原则是什么？](answers/150-rest-resource-modeling.md)
151. [`GET`、`POST`、`PUT`、`PATCH`、`DELETE` 应该如何使用？](answers/151-http-method-semantics.md)
152. [哪些接口应该设计成幂等？](answers/152-idempotent-apis.md)
153. [幂等键应该放 Header 还是 Body？](answers/153-idempotency-key-header-or-body.md)
154. [API 版本如何设计？](answers/154-api-versioning.md)
155. [什么时候需要新版本 API？](answers/155-when-new-api-version.md)
156. [如何设计统一响应体？](answers/156-unified-response-body.md)
157. [HTTP 状态码和业务错误码如何配合？](answers/157-error-code-vs-http-status.md)
158. [分页接口如何设计？](answers/158-pagination-api.md)
159. [如何防止批量查询拖垮系统？](answers/159-prevent-bulk-query-overload.md)
160. [API 如何保证向后兼容？](answers/160-api-backward-compatibility.md)
161. [如何做字段废弃和迁移？](answers/161-field-deprecation-migration.md)
162. [开放 API 如何做签名？](answers/162-open-api-signature.md)
163. [如何防止接口重放攻击？](answers/163-prevent-replay-attack.md)
164. [如何做请求限流和配额？](answers/164-api-rate-limiting.md)
165. [如何设计 API 网关层职责？](answers/165-api-gateway-responsibility.md)
166. [网关和业务服务分别做什么校验？](answers/166-gateway-vs-service-validation.md)
167. [如何设计 BFF？](answers/167-bff-design.md)
168. [移动端和 PC 端 API 是否应该完全共用？](answers/168-mobile-pc-api-sharing.md)
169. [内部 API 和开放 API 有什么区别？](answers/169-internal-vs-external-api.md)
170. [如何写 API 文档和契约测试？](answers/170-api-docs-contract-tests.md)
171. [深分页有什么问题？](answers/171-deep-pagination-risk.md)
172. [游标分页和 offset 分页如何取舍？](answers/172-cursor-vs-offset-pagination.md)
173. [查询接口如何防止过度复杂？](answers/173-query-api-complexity-control.md)
174. [批量接口如何设计部分成功？](answers/174-bulk-api-partial-success.md)
175. [文件上传接口如何做安全限制？](answers/175-file-upload-api-security.md)
176. [如何做 API 兼容性测试？](answers/176-api-compatibility-tests.md)
177. [如何设计接口超时预算？](answers/177-api-timeout-budget.md)
178. [如何处理客户端重试导致的重复请求？](answers/178-client-retry-duplicate-request.md)
179. [如何设计错误信息，既便于排障又不泄露内部实现？](answers/179-safe-error-message-design.md)
180. [GraphQL 和 REST 如何取舍？](answers/180-graphql-vs-rest.md)
181. [gRPC 和 HTTP JSON 如何取舍？](answers/181-grpc-vs-http-json.md)
182. [如何设计跨服务错误码映射？](answers/182-cross-service-error-code-mapping.md)
183. [如何治理废弃 API？](answers/183-deprecated-api-governance.md)

## 02 分布式架构和中间件

### 01 微服务拆分和架构治理

题号：184-210；进度：27/27。

184. [什么情况下应该拆微服务？](answers/184-when-to-split-microservices.md)
185. [什么情况下不应该拆微服务？](answers/185-when-not-to-split-microservices.md)
186. [微服务和模块化单体如何取舍？](answers/186-microservice-vs-modular-monolith.md)
187. [服务边界应该按业务域还是技术层拆？](answers/187-service-boundary-business-vs-technical.md)
188. [什么是数据所有权？](answers/188-data-ownership.md)
189. [为什么服务之间不应该共享数据库表？](answers/189-no-shared-database-tables.md)
190. [拆库后跨服务查询怎么做？](answers/190-cross-service-query.md)
191. [跨服务事务怎么处理？](answers/191-cross-service-transaction.md)
192. [同步调用和异步事件如何取舍？](answers/192-sync-vs-async-events.md)
193. [服务调用链过长有什么风险？](answers/193-long-service-call-chain-risk.md)
194. [如何避免分布式单体？](answers/194-avoid-distributed-monolith.md)
195. [如何识别错误的服务边界？](answers/195-identify-wrong-service-boundary.md)
196. [如何设计订单、库存、支付的边界？](answers/196-order-inventory-payment-boundary.md)
197. [搜索、推荐、广告为什么通常不放在交易主链路？](answers/197-search-recommendation-ads-not-main-path.md)
198. [如何处理跨团队服务契约？](answers/198-cross-team-service-contract.md)
199. [如何做服务依赖治理？](answers/199-service-dependency-governance.md)
200. [如何画服务依赖图？](answers/200-service-dependency-graph.md)
201. [如何评估一个服务的故障半径？](answers/201-service-blast-radius.md)
202. [如何做服务分级？](answers/202-service-tiering.md)
203. [核心链路和非核心链路如何隔离？](answers/203-core-noncore-isolation.md)
204. [如何设计内部运维接口？](answers/204-internal-ops-api-design.md)
205. [运维接口为什么不能暴露公网？](answers/205-ops-api-not-public.md)
206. [如何做多租户或商家隔离？](answers/206-multitenancy-merchant-isolation.md)
207. [如何做配置治理？](answers/207-configuration-governance.md)
208. [如何做灰度开关和功能开关？](answers/208-feature-flag-gray-switch.md)
209. [配置中心故障会带来什么影响？](answers/209-config-center-failure-impact.md)
210. [如何处理配置变更回滚？](answers/210-config-change-rollback.md)

### 02 分布式一致性和事务

题号：211-245；进度：35/35。

211. [为什么大型分布式系统通常不用全局大事务？](answers/211-avoid-global-distributed-transaction.md)
212. [2PC 的流程是什么？](answers/212-two-phase-commit-flow.md)
213. [2PC 有哪些可用性问题？](answers/213-two-phase-commit-availability-issues.md)
214. [3PC 解决了什么，又有什么限制？](answers/214-three-phase-commit-limits.md)
215. [TCC 适合哪些场景？](answers/215-tcc-scenarios.md)
216. [Saga 适合哪些场景？](answers/216-saga-scenarios.md)
217. [本地事务加 Outbox 解决什么问题？](answers/217-local-transaction-outbox.md)
218. [Outbox 不能解决什么问题？](answers/218-outbox-limitations.md)
219. [业务数据写成功但 MQ 发送失败怎么办？](answers/219-business-success-mq-send-failed.md)
220. [MQ 发送成功但业务事务回滚怎么办？](answers/220-mq-sent-business-rollback.md)
221. [消息重复投递怎么办？](answers/221-duplicate-message-delivery.md)
222. [消费成功但 ack 失败怎么办？](answers/222-consume-success-ack-failed.md)
223. [消费失败后如何重试？](answers/223-consume-failure-retry.md)
224. [重试多次失败如何进入死信？](answers/224-dead-letter-after-retry.md)
225. [死信消息如何恢复？](answers/225-dead-letter-recovery.md)
226. [如何保证同一订单事件顺序？](answers/226-order-event-ordering.md)
227. [如何处理跨 partition 乱序？](answers/227-cross-partition-out-of-order.md)
228. [如何设计事件版本？](answers/228-event-versioning.md)
229. [如何保证事件 schema 兼容？](answers/229-event-schema-compatibility.md)
230. [最终一致和强一致如何取舍？](answers/230-eventual-vs-strong-consistency.md)
231. [用户看到中间状态是否可接受？](answers/231-intermediate-state-user-visible.md)
232. [下单后库存预占失败怎么办？](answers/232-stock-reservation-failed-after-order.md)
233. [支付成功但订单更新失败怎么办？](answers/233-payment-success-order-update-failed.md)
234. [订单取消但库存释放失败怎么办？](answers/234-order-cancel-stock-release-failed.md)
235. [补偿任务如何避免重复执行？](answers/235-compensation-idempotency.md)
236. [补偿任务如何避免多实例并发执行？](answers/236-compensation-concurrency-control.md)
237. [对账和补偿有什么区别？](answers/237-reconciliation-vs-compensation.md)
238. [对账发现差异后如何处理？](answers/238-reconciliation-difference-handling.md)
239. [幂等和去重有什么区别？](answers/239-idempotency-vs-deduplication.md)
240. [幂等记录保留多久？](answers/240-idempotency-record-retention.md)
241. [幂等表无限增长怎么办？](answers/241-idempotency-table-growth.md)
242. [状态机如何防止非法状态跳转？](answers/242-state-machine-illegal-transition.md)
243. [如何设计可恢复的业务状态？](answers/243-recoverable-business-state.md)
244. [如何处理悬挂、空回滚和重复提交？](answers/244-hanging-empty-rollback-duplicate-submit.md)
245. [如何证明一致性方案是可靠的？](answers/245-prove-consistency-scheme-reliable.md)

### 03 高并发、限流和稳定性

题号：246-280；进度：35/35。另有题库补充题 4 道。

246. [高并发系统的瓶颈通常在哪里？](answers/246-high-concurrency-bottlenecks.md)
247. [QPS、TPS、并发数、响应时间之间是什么关系？](answers/247-qps-tps-concurrency-latency.md)
248. [如何做容量估算？](answers/248-capacity-estimation.md)
249. [如何估算线程池、连接池和数据库连接数？](answers/249-pool-size-estimation.md)
250. [如何设计限流？](answers/250-rate-limiting-design.md)
251. [什么是熔断？](answers/251-circuit-breaker.md)
252. [熔断的 CLOSED、OPEN、HALF_OPEN 如何切换？](answers/252-circuit-breaker-states.md)
253. [半开状态为什么要小流量探测？](answers/253-half-open-probing.md)
254. [熔断和限流有什么区别？](answers/254-circuit-breaker-vs-rate-limit.md)
255. [熔断和降级有什么区别？](answers/255-circuit-breaker-vs-degradation.md)
256. [哪些业务可以降级？](answers/256-degradable-business.md)
257. [哪些业务不能降级？](answers/257-non-degradable-business.md)
258. [推荐服务故障如何降级？](answers/258-recommendation-failure-degradation.md)
259. [价格服务故障能不能降级？](answers/259-price-service-failure-degradation.md)
260. [库存服务故障能不能继续下单？](answers/260-inventory-failure-continue-order.md)
261. [如何设置超时？](answers/261-timeout-design.md)
262. [如何设计重试？](answers/262-retry-design.md)
263. [为什么重试可能放大故障？](answers/263-retry-amplifies-failure.md)
264. [什么是指数退避和 jitter？](answers/264-exponential-backoff-jitter.md)
265. [如何做请求合并？](answers/265-request-coalescing.md)
266. [如何做热点隔离？](answers/266-hotspot-isolation.md)
267. [热点 SKU 为什么会拖垮数据库？](answers/267-hot-sku-database-risk.md)
268. [库存桶如何降低热点行竞争？](answers/268-inventory-buckets.md)
269. [秒杀系统为什么要削峰？](answers/269-flash-sale-traffic-shaping.md)
270. [秒杀令牌如何设计？](answers/270-flash-sale-token-design.md)
271. [秒杀如何防止超卖？](answers/271-flash-sale-oversell-prevention.md)
272. [秒杀如何防止黄牛？](answers/272-flash-sale-bot-prevention.md)
273. [如何保护普通下单不被秒杀拖垮？](answers/273-protect-normal-order-from-flash-sale.md)
274. [如何做优雅关闭？](answers/274-graceful-shutdown.md)
275. [服务关闭时如何处理正在执行的请求？](answers/275-inflight-request-on-shutdown.md)
276. [如何设计下游自动平滑恢复？](answers/276-downstream-smooth-recovery.md)
277. [下游恢复后为什么不能立刻放开全部流量？](answers/277-why-not-full-traffic-after-recovery.md)
278. [如何做预热？](answers/278-warmup-design.md)
279. [如何做过载保护？](answers/279-overload-protection.md)
280. [如何判断系统已经进入过载？](answers/280-overload-detection.md)

#### 题库补充题

补充 1. [固定窗口、滑动窗口、令牌桶、漏桶有什么区别？](answers/537-rate-limit-system.md)
补充 2. [网关限流和服务内限流如何配合？](answers/250-rate-limiting-design.md)
补充 3. [按 IP、用户、设备、SKU、商家限流有什么区别？](answers/537-rate-limit-system.md)
补充 4. [如何防止恶意用户绕过限流？](answers/528-risk-system.md)

### 04 数据库、SQL 和数据建模

题号：281-323；进度：43/43。

281. [关系型数据库适合解决什么问题？](answers/281-relational-database-use-cases.md)
282. [MySQL InnoDB 的索引结构是什么？](answers/282-innodb-index-structure.md)
283. [B+Tree 为什么适合数据库索引？](answers/283-bplus-tree-database-index.md)
284. [聚簇索引和二级索引有什么区别？](answers/284-clustered-vs-secondary-index.md)
285. [回表是什么？](answers/285-bookmark-lookup.md)
286. [覆盖索引是什么？](answers/286-covering-index.md)
287. [联合索引最左前缀是什么？](answers/287-leftmost-prefix.md)
288. [索引下推是什么？](answers/288-index-condition-pushdown.md)
289. [什么情况下索引会失效？](answers/289-index-invalid-cases.md)
290. [慢 SQL 如何分析？](answers/290-slow-sql-analysis.md)
291. [`explain` 重点看哪些字段？](answers/291-explain-key-fields.md)
292. [选择性低的字段是否适合建索引？](answers/292-low-cardinality-index.md)
293. [索引越多为什么写入越慢？](answers/293-too-many-indexes-slow-writes.md)
294. [如何设计订单表索引？](answers/294-order-table-index-design.md)
295. [如何设计支付表唯一约束？](answers/295-payment-table-unique-constraints.md)
296. [如何设计 Outbox 扫描索引？](answers/296-outbox-scan-index-design.md)
297. [事务 ACID 分别是什么？](answers/297-acid.md)
298. [脏读、不可重复读、幻读是什么？](answers/298-dirty-nonrepeatable-phantom-read.md)
299. [MVCC 是什么？](answers/299-mvcc.md)
300. [快照读和当前读有什么区别？](answers/300-snapshot-read-vs-current-read.md)
301. [间隙锁是什么？](answers/301-gap-lock.md)
302. [死锁如何产生？](answers/302-deadlock-causes.md)
303. [如何分析和避免数据库死锁？](answers/303-analyze-avoid-db-deadlock.md)
304. [悲观锁和乐观锁怎么选？](answers/304-pessimistic-vs-optimistic-lock.md)
305. [条件更新如何防止库存超卖？](answers/305-conditional-update-prevent-oversell.md)
306. [`select for update` 有什么风险？](answers/306-select-for-update-risks.md)
307. [如何设计分库分表？](answers/307-sharding-design.md)
308. [分片键如何选择？](answers/308-sharding-key-selection.md)
309. [按用户分片和按订单分片有什么区别？](answers/309-user-vs-order-sharding.md)
310. [分库分表后如何按用户查订单？](answers/310-query-orders-by-user-after-sharding.md)
311. [分库分表后如何按订单号查订单？](answers/311-query-order-by-order-no-after-sharding.md)
312. [全局唯一 ID 如何生成？](answers/312-global-unique-id.md)
313. [Snowflake ID 的结构和风险是什么？](answers/313-snowflake-id.md)
314. [时钟回拨如何处理？](answers/314-clock-backward-handling.md)
315. [数据归档如何设计？](answers/315-data-archiving-design.md)
316. [冷热分离如何设计？](answers/316-hot-cold-data-separation.md)
317. [在线 DDL 有什么风险？](answers/317-online-ddl-risks.md)
318. [数据库扩容如何做？](answers/318-database-capacity-expansion.md)
319. [读写分离有什么一致性问题？](answers/319-read-write-splitting-consistency.md)
320. [主从延迟如何影响业务？](answers/320-replication-lag-business-impact.md)
321. [如何做数据库备份和恢复演练？](answers/321-database-backup-restore-drill.md)
322. [ORM 能解决哪些问题，不能解决哪些问题？](answers/322-orm-boundaries.md)
323. [MyBatis Plus 和手写 SQL 如何取舍？](answers/323-mybatis-plus-vs-handwritten-sql.md)

### 05 Redis 和缓存体系

题号：324-357；进度：34/34。

324. [Redis 常用数据结构有哪些？](answers/324-redis-data-structures.md)
325. [String、Hash、List、Set、ZSet 分别适合什么场景？](answers/325-redis-structure-use-cases.md)
326. [Redis 单线程为什么还能很快？](answers/326-redis-single-thread-fast.md)
327. [Redis IO 多路复用是什么？](answers/327-redis-io-multiplexing.md)
328. [Redis 持久化 RDB 和 AOF 有什么区别？](answers/328-redis-rdb-vs-aof.md)
329. [Redis 主从复制如何工作？](answers/329-redis-replication.md)
330. [Redis Sentinel 解决什么问题？](answers/330-redis-sentinel.md)
331. [Redis Cluster 如何分片？](answers/331-redis-cluster-sharding.md)
332. [一致性哈希和 Redis Cluster slot 有什么区别？](answers/332-consistent-hash-vs-redis-slot.md)
333. [缓存穿透是什么？](answers/333-cache-penetration.md)
334. [缓存击穿是什么？](answers/334-cache-breakdown.md)
335. [缓存雪崩是什么？](answers/335-cache-avalanche.md)
336. [如何设计商品详情缓存？](answers/336-product-detail-cache-design.md)
337. [缓存 key 如何命名？](answers/337-cache-key-naming.md)
338. [TTL 如何设置？](answers/338-cache-ttl-design.md)
339. [TTL 为什么要加随机抖动？](answers/339-cache-ttl-jitter.md)
340. [更新数据库和删除缓存顺序怎么选？](answers/340-cache-db-update-order.md)
341. [延迟双删解决什么问题？](answers/341-delayed-double-delete.md)
342. [缓存和数据库短暂不一致如何接受？](answers/342-cache-db-temporary-inconsistency.md)
343. [热 key 如何发现？](answers/343-hot-key-discovery.md)
344. [热 key 如何治理？](answers/344-hot-key-governance.md)
345. [大 key 有什么危害？](answers/345-big-key-risks.md)
346. [如何拆分大 key？](answers/346-big-key-splitting.md)
347. [Redis 分布式锁如何实现？](answers/347-redis-distributed-lock.md)
348. [`SET NX PX` 有什么注意点？](answers/348-set-nx-px-notes.md)
349. [Lua 脚本为什么能保证原子性？](answers/349-lua-atomicity.md)
350. [Redisson 看门狗解决什么问题？](answers/350-redisson-watchdog.md)
351. [Redis 限流如何实现？](answers/351-redis-rate-limiting.md)
352. [Redis 计数器如何避免过期窗口问题？](answers/352-redis-counter-expiry-window.md)
353. [布隆过滤器适合什么场景？](answers/353-bloom-filter-use-cases.md)
354. [Redis 故障时系统如何降级？](answers/354-redis-failure-degradation.md)
355. [缓存命中率下降如何排查？](answers/355-cache-hit-rate-drop.md)
356. [Redis 内存淘汰策略有哪些？](answers/356-redis-eviction-policies.md)
357. [Redis 是否适合作为订单状态最终数据源？](answers/357-redis-as-order-source.md)

### 06 Kafka 和消息系统

题号：358-387；进度：30/30。

358. [Kafka 的 Topic、Partition、Replica、Broker 是什么？](answers/358-kafka-topic-partition-replica-broker.md)
359. [Producer 如何选择 partition？](answers/359-kafka-producer-partition-selection.md)
360. [Consumer Group 如何工作？](answers/360-kafka-consumer-group.md)
361. [offset 是什么？](answers/361-kafka-offset.md)
362. [消息提交 offset 的时机如何选择？](answers/362-offset-commit-timing.md)
363. [至少一次、至多一次、恰好一次分别是什么意思？](answers/363-kafka-delivery-semantics.md)
364. [Kafka 的 exactly-once 为什么不等于业务 exactly-once？](answers/364-kafka-eos-vs-business-eos.md)
365. [ISR 是什么？](answers/365-kafka-isr.md)
366. [ack=0、ack=1、ack=all 有什么区别？](answers/366-kafka-ack-modes.md)
367. [Producer 幂等解决什么问题？](answers/367-kafka-producer-idempotence.md)
368. [Kafka 事务适合什么场景？](answers/368-kafka-transactions.md)
369. [消息顺序如何保证？](answers/369-message-ordering.md)
370. [为什么顺序通常只能保证同一个 partition 内？](answers/370-ordering-within-partition.md)
371. [如何设计订单事件 topic？](answers/371-order-event-topic-design.md)
372. [事件应该携带全量数据还是只携带 ID？](answers/372-event-full-data-or-id.md)
373. [消息体过大有什么问题？](answers/373-large-message-problems.md)
374. [消费者处理慢怎么办？](answers/374-slow-consumer-handling.md)
375. [consumer lag 如何监控？](answers/375-consumer-lag-monitoring.md)
376. [消息积压如何排查？](answers/376-message-backlog-troubleshooting.md)
377. [增加消费者为什么不一定能解决积压？](answers/377-adding-consumers-not-enough.md)
378. [单分区热点如何处理？](answers/378-hot-partition-handling.md)
379. [重试 topic 和死信 topic 如何设计？](answers/379-retry-and-dead-letter-topic.md)
380. [消费者幂等表如何设计？](answers/380-consumer-idempotency-table.md)
381. [消费端如何保证业务写入和去重记录原子性？](answers/381-consumer-write-dedup-atomicity.md)
382. [消息 schema 如何演进？](answers/382-message-schema-evolution.md)
383. [如何处理消息反序列化失败？](answers/383-message-deserialization-failure.md)
384. [Kafka 和 RabbitMQ 如何取舍？](answers/384-kafka-vs-rabbitmq.md)
385. [MQ 故障时核心链路怎么办？](answers/385-mq-failure-core-flow.md)
386. [Outbox Relay 多实例如何避免重复抢事件？](answers/386-outbox-relay-multi-instance.md)
387. [Outbox 历史数据如何清理？](answers/387-outbox-history-cleanup.md)

### 07 搜索、读模型和数据同步

题号：388-403；进度：16/16。

388. [为什么电商搜索通常不用 MySQL 直接实现？](answers/388-why-not-mysql-search.md)
389. [倒排索引是什么？](answers/389-inverted-index.md)
390. [分词器如何影响搜索结果？](answers/390-tokenizer-impact.md)
391. [商品搜索文档应该包含哪些字段？](answers/391-product-search-document-fields.md)
392. [商品上下架如何同步到搜索索引？](answers/392-product-status-sync-to-search.md)
393. [搜索索引和商品库不一致怎么办？](answers/393-search-index-inconsistency.md)
394. [搜索读模型可以重建吗？](answers/394-search-read-model-rebuildable.md)
395. [如何设计索引重建流程？](answers/395-index-rebuild-flow.md)
396. [OpenSearch 分片和副本如何设置？](answers/396-opensearch-shards-replicas.md)
397. [搜索结果排序考虑哪些因素？](answers/397-search-ranking-factors.md)
398. [相关性、销量、价格、广告如何混排？](answers/398-relevance-sales-price-ads-mix.md)
399. [搜索服务故障如何降级？](answers/399-search-degradation.md)
400. [搜索延迟升高如何排查？](answers/400-search-latency-troubleshooting.md)
401. [商品详情缓存和搜索索引是什么关系？](answers/401-product-cache-vs-search-index.md)
402. [CQRS 在电商系统中如何体现？](answers/402-cqrs-in-ecommerce.md)
403. [读模型最终一致对用户有什么影响？](answers/403-read-model-eventual-consistency-impact.md)

## 03 生产工程和平台治理

### 01 安全、身份和风控

题号：404-430；进度：27/27。

404. [认证和授权有什么区别？](answers/404-authentication-vs-authorization.md)
405. [Session、JWT、OAuth2 如何取舍？](answers/405-session-jwt-oauth2-tradeoff.md)
406. [JWT 有什么风险？](answers/406-jwt-risks.md)
407. [token 泄露如何处理？](answers/407-token-leakage-handling.md)
408. [RBAC 和 ABAC 有什么区别？](answers/408-rbac-vs-abac.md)
409. [最小权限原则如何落地？](answers/409-least-privilege.md)
410. [内部运维接口如何鉴权？](answers/410-internal-ops-auth.md)
411. [HMAC 签名如何防篡改？](answers/411-hmac-signature-tamper-proof.md)
412. [nonce 如何防重放？](answers/412-nonce-anti-replay.md)
413. [timestamp 窗口如何设置？](answers/413-timestamp-window.md)
414. [签名为什么要覆盖 method、path、body hash？](answers/414-sign-method-path-body-hash.md)
415. [常量时间比较解决什么问题？](answers/415-constant-time-comparison.md)
416. [AES-GCM 适合什么场景？](answers/416-aes-gcm-use-cases.md)
417. [HMAC hash 为什么适合精确查询？](answers/417-hmac-hash-exact-query.md)
418. [手机号为什么不能明文存储？](answers/418-phone-not-plaintext.md)
419. [加密字段如何做密钥轮换？](answers/419-encrypted-field-key-rotation.md)
420. [日志脱敏如何实现？](answers/420-log-masking.md)
421. [SQL 注入如何防止？](answers/421-sql-injection-prevention.md)
422. [XSS 和 CSRF 在前后端系统中如何防护？](answers/422-xss-csrf-protection.md)
423. [SSRF 风险在哪里？](answers/423-ssrf-risk.md)
424. [开放平台如何做 appKey 和 secret 管理？](answers/424-appkey-secret-management.md)
425. [如何做接口权限审计？](answers/425-api-permission-audit.md)
426. [风控规则如何灰度上线？](answers/426-risk-rule-gray-release.md)
427. [账号冻结对下单链路有什么影响？](answers/427-account-freeze-order-impact.md)
428. [支付风控应在什么环节介入？](answers/428-payment-risk-intervention.md)
429. [如何设计高危操作双人审批？](answers/429-high-risk-dual-approval.md)
430. [如何处理数据合规删除和历史订单保留？](answers/430-compliance-delete-vs-order-retention.md)

### 02 可观测性、告警和故障处理

题号：431-457；进度：27/27。

431. [日志、指标、Trace 分别解决什么问题？](answers/431-logs-metrics-traces.md)
432. [为什么三者都需要？](answers/432-why-need-logs-metrics-traces.md)
433. [trace ID 如何生成和透传？](answers/433-trace-id-generation-propagation.md)
434. [日志中必须包含哪些业务字段？](answers/434-log-business-fields.md)
435. [结构化日志有什么好处？](answers/435-structured-logging-benefits.md)
436. [什么信息不能写入日志？](answers/436-what-not-to-log.md)
437. [RED 指标是什么？](answers/437-red-metrics.md)
438. [USE 指标是什么？](answers/438-use-metrics.md)
439. [核心交易链路要监控哪些业务指标？](answers/439-core-transaction-business-metrics.md)
440. [订单成功率下降如何排查？](answers/440-order-success-rate-drop.md)
441. [支付回调延迟升高如何排查？](answers/441-payment-callback-latency.md)
442. [库存预占失败率升高如何排查？](answers/442-inventory-reservation-failure-rate.md)
443. [Outbox 积压如何排查？](answers/443-outbox-backlog-troubleshooting.md)
444. [Kafka lag 增长如何排查？](answers/444-kafka-lag-growth.md)
445. [Redis 命中率下降如何排查？](answers/445-redis-hit-rate-drop-troubleshooting.md)
446. [数据库连接池耗尽如何排查？](answers/446-db-connection-pool-exhaustion.md)
447. [P99 升高但平均延迟正常说明什么？](answers/447-p99-high-average-normal.md)
448. [告警为什么不能太多？](answers/448-alert-fatigue.md)
449. [如何设计告警级别？](answers/449-alert-severity-design.md)
450. [SLO、SLI、SLA 有什么区别？](answers/450-slo-sli-sla.md)
451. [错误预算是什么？](answers/451-error-budget.md)
452. [burn rate 告警如何理解？](answers/452-burn-rate-alerting.md)
453. [Runbook 应该包含什么？](answers/453-runbook-content.md)
454. [事故复盘应该关注什么？](answers/454-incident-review-focus.md)
455. [如何区分根因和触发因素？](answers/455-root-cause-vs-trigger.md)
456. [如何设计一次故障演练？](answers/456-failure-drill-design.md)
457. [如何证明系统具备可恢复能力？](answers/457-prove-recoverability.md)

### 03 Kubernetes、容器和发布

题号：458-484；进度：27/27。

458. [Docker 镜像分层是什么？](answers/458-docker-image-layers.md)
459. [如何构建小而安全的 Java 镜像？](answers/459-small-secure-java-image.md)
460. [为什么生产容器不建议 root 用户运行？](answers/460-non-root-containers.md)
461. [Kubernetes Deployment、Service、Ingress 分别是什么？](answers/461-k8s-deployment-service-ingress.md)
462. [ConfigMap 和 Secret 有什么区别？](answers/462-configmap-vs-secret.md)
463. [readinessProbe、livenessProbe、startupProbe 如何设计？](answers/463-k8s-probe-design.md)
464. [为什么 liveness 不应该强依赖所有下游？](answers/464-liveness-not-dependent-on-downstreams.md)
465. [HPA 根据什么指标扩缩容？](answers/465-hpa-metrics.md)
466. [requests 和 limits 如何设置？](answers/466-k8s-requests-limits.md)
467. [CPU limit 对 Java 服务有什么影响？](answers/467-cpu-limit-impact-on-java.md)
468. [PodDisruptionBudget 解决什么问题？](answers/468-pod-disruption-budget.md)
469. [滚动升级如何保证可用性？](answers/469-rolling-upgrade-availability.md)
470. [优雅关闭如何配置？](answers/470-graceful-shutdown-config.md)
471. [服务发现如何工作？](answers/471-service-discovery.md)
472. [Ingress 和 API Gateway 有什么区别？](answers/472-ingress-vs-api-gateway.md)
473. [NetworkPolicy 解决什么问题？](answers/473-network-policy.md)
474. [多可用区部署要考虑什么？](answers/474-multi-az-deployment.md)
475. [蓝绿发布和金丝雀发布有什么区别？](answers/475-blue-green-vs-canary.md)
476. [灰度发布观察哪些指标？](answers/476-canary-metrics.md)
477. [回滚前为什么要考虑数据库兼容？](answers/477-rollback-db-compatibility.md)
478. [数据库迁移如何配合应用发布？](answers/478-db-migration-with-release.md)
479. [什么是 expand-contract 发布模式？](answers/479-expand-contract-release.md)
480. [配置错误导致故障如何快速回滚？](answers/480-config-rollback.md)
481. [如何设计生产 Secret 管理？](answers/481-production-secret-management.md)
482. [Service Mesh 解决什么问题？](answers/482-service-mesh.md)
483. [mTLS 在服务间调用中有什么价值？](answers/483-mtls-value.md)
484. [什么时候不应该引入 Service Mesh？](answers/484-when-not-to-use-service-mesh.md)

### 04 测试、质量和工程治理

题号：485-513；进度：29/29。

485. [单元测试、集成测试、端到端测试有什么区别？](answers/485-unit-integration-e2e-tests.md)
486. [哪些逻辑必须有单元测试？](answers/486-logic-needs-unit-tests.md)
487. [哪些逻辑必须有集成测试？](answers/487-logic-needs-integration-tests.md)
488. [Mock 的优点和风险是什么？](answers/488-mock-benefits-risks.md)
489. [Testcontainers 适合验证什么？](answers/489-testcontainers-use-cases.md)
490. [为什么只测 happy path 不够？](answers/490-happy-path-not-enough.md)
491. [如何测试幂等？](answers/491-test-idempotency.md)
492. [如何测试库存防超卖？](answers/492-test-inventory-oversell.md)
493. [如何测试支付重复回调？](answers/493-test-payment-duplicate-callback.md)
494. [如何测试 Outbox 重试？](answers/494-test-outbox-retry.md)
495. [如何测试 MQ 重复消费？](answers/495-test-mq-duplicate-consumption.md)
496. [如何测试补偿任务？](answers/496-test-compensation-job.md)
497. [如何测试限流和熔断？](answers/497-test-rate-limit-circuit-breaker.md)
498. [如何测试降级逻辑？](answers/498-test-degradation.md)
499. [Smoke 测试应该覆盖哪些链路？](answers/499-smoke-test-chains.md)
500. [压测前要准备什么？](answers/500-before-load-test.md)
501. [压测结果如何分析？](answers/501-analyze-load-test-results.md)
502. [如何定位压测瓶颈？](answers/502-find-load-test-bottleneck.md)
503. [回归测试如何分层？](answers/503-layered-regression-tests.md)
504. [CI 流水线应该包含哪些阶段？](answers/504-ci-pipeline-stages.md)
505. [Checkstyle、SpotBugs、PMD 分别解决什么问题？](answers/505-checkstyle-spotbugs-pmd.md)
506. [代码覆盖率高是否代表质量高？](answers/506-code-coverage-quality.md)
507. [如何做代码评审？](answers/507-code-review.md)
508. [如何评审分布式一致性相关代码？](answers/508-review-distributed-consistency-code.md)
509. [如何设计公共模块，避免 common 变成垃圾桶？](answers/509-common-module-design.md)
510. [如何管理依赖版本？](answers/510-dependency-version-management.md)
511. [如何处理依赖漏洞？](answers/511-handle-dependency-vulnerabilities.md)
512. [如何设计兼容性测试？](answers/512-compatibility-tests.md)
513. [如何保证文档和代码一致？](answers/513-docs-code-consistency.md)

## 04 系统设计

### 01 电商核心系统设计

题号：514-530；进度：17/17。

514. [设计一个京东/Amazon 类电商系统。](answers/514-jd-amazon-ecommerce.md)
515. [设计下单系统。](answers/515-order-system.md)
516. [设计库存系统。](answers/516-inventory-system.md)
517. [设计支付系统。](answers/517-payment-system.md)
518. [设计购物车系统。](answers/518-cart-system.md)
519. [设计商品详情系统。](answers/519-product-detail-system.md)
520. [设计商品搜索系统。](answers/520-product-search-system.md)
521. [设计秒杀系统。](answers/521-flash-sale-system.md)
522. [设计优惠券系统。](answers/522-coupon-system.md)
523. [设计价格服务。](answers/523-pricing-system.md)
524. [设计履约系统。](answers/524-fulfillment-system.md)
525. [设计售后退款系统。](answers/525-after-sales-refund.md)
526. [设计商家入驻和商品审核系统。](answers/526-merchant-onboarding-review.md)
527. [设计开放平台 API。](answers/527-openapi.md)
528. [设计风控系统。](answers/528-risk-system.md)
529. [设计推荐系统的在线服务部分。](answers/529-recommendation-online.md)
530. [设计广告投放系统的核心链路。](answers/530-ad-delivery.md)

### 02 平台和基础设施设计

题号：531-541；进度：11/11。

531. [设计订单对账系统。](answers/531-order-reconciliation.md)
532. [设计支付渠道对账系统。](answers/532-payment-channel-reconciliation.md)
533. [设计消息重放平台。](answers/533-message-replay-platform.md)
534. [设计内部运维补偿平台。](answers/534-internal-compensation-platform.md)
535. [设计分布式 ID 服务。](answers/535-distributed-id.md)
536. [设计配置中心。](answers/536-config-center.md)
537. [设计限流系统。](answers/537-rate-limit-system.md)
538. [设计熔断降级平台。](answers/538-circuit-degrade-platform.md)
539. [设计灰度发布平台。](answers/539-canary-release-platform.md)
540. [设计日志采集和查询平台。](answers/540-log-collection-query.md)
541. [设计指标和告警平台。](answers/541-metrics-alerting.md)

### 03 大规模容量和多活设计

题号：542-554；进度：13/13。

542. [设计链路追踪系统。](answers/542-distributed-tracing-system.md)
543. [设计多区域电商系统。](answers/543-multi-region-ecommerce.md)
544. [设计异地多活订单系统。](answers/544-active-active-order.md)
545. [设计数据归档系统。](answers/545-data-archiving.md)
546. [设计大促容量保障方案。](answers/546-promotion-capacity.md)
547. [设计热点 SKU 保护方案。](answers/547-hot-sku-protection.md)
548. [设计商品缓存系统。](answers/548-product-cache.md)
549. [设计 Kafka Outbox 可靠事件系统。](answers/549-kafka-outbox-events.md)
550. [设计面向 10 亿用户的用户中心。](answers/550-billion-user-center.md)
551. [设计支持 100 万峰值并发的网关。](answers/551-million-concurrency-gateway.md)
552. [设计支持 10 万下单 QPS 的交易链路。](answers/552-order-qps-transaction.md)
553. [设计一次数据库扩容和迁移方案。](answers/553-database-expansion-migration.md)
554. [设计一次从单体到微服务的演进方案。](answers/554-monolith-to-microservices.md)

## 05 生产事故和排障

### 01 核心交易事故

题号：555-563；进度：9/9。

555. [下单成功率突然下降，你怎么排查？](answers/555-order-success-rate-drop.md)
556. [支付成功但订单未变成已支付，怎么处理？](answers/556-payment-success-order-unpaid.md)
557. [库存出现少量超卖，怎么定位和修复？](answers/557-inventory-oversell.md)
558. [库存预占记录大量过期未释放，怎么恢复？](answers/558-stock-reservation-unreleased.md)
559. [Outbox 表积压大量待发送事件，怎么处理？](answers/559-outbox-backlog.md)
560. [Kafka 某个 topic lag 快速增长，怎么排查？](answers/560-kafka-lag-growth.md)
561. [Redis 集群抖动，商品详情接口 P99 升高，怎么处理？](answers/561-redis-p99-spike.md)
562. [数据库 CPU 100%，你先看什么？](answers/562-database-high-cpu.md)
563. [数据库连接池耗尽，如何止血？](answers/563-db-pool-exhaustion.md)

### 02 发布、中间件和运行时事故

题号：564-572；进度：9/9。

564. [某个新版本发布后错误率上升，如何判断是否回滚？](answers/564-release-error-rate.md)
565. [灰度 5% 正常，放量 50% 后异常，可能是什么原因？](answers/565-canary-scale-up.md)
566. [一个 Pod 频繁重启，如何排查？](answers/566-pod-restart.md)
567. [JVM Full GC 频繁，如何处理？](answers/567-full-gc.md)
568. [线程数暴涨，如何定位？](answers/568-thread-spike.md)
569. [线上死锁如何处理？](answers/569-deadlock.md)
570. [支付渠道重复回调导致大量冲突日志，是否是事故？](answers/570-duplicate-payment-callback.md)
571. [对账发现渠道成功本地失败，怎么修复？](answers/571-reconciliation-channel-success.md)
572. [用户投诉重复扣款，如何排查？](answers/572-duplicate-charge.md)

### 03 配置、安全、观测和数据修复

题号：573-579；进度：7/7。

573. [秒杀开始后普通下单也变慢，怎么止血？](answers/573-flash-sale-slow-normal-order.md)
574. [搜索结果大量缺商品，怎么恢复？](answers/574-search-missing-products.md)
575. [商品价格显示旧值，如何排查缓存一致性问题？](answers/575-stale-price-cache.md)
576. [配置中心推错配置，如何回滚？](answers/576-config-rollback.md)
577. [Secret 泄露后如何应急？](answers/577-secret-leak.md)
578. [日志系统故障是否会影响交易链路？](answers/578-logging-outage.md)
579. [监控告警误报太多，如何治理？](answers/579-alert-noise.md)

### 04 大促、容量和恢复决策

题号：580-584；进度：5/5。

580. [生产数据库误删数据，如何恢复？](answers/580-database-data-deletion.md)
581. [消费者 bug 导致错误写入大量数据，如何修复？](answers/581-consumer-bad-writes.md)
582. [下游恢复后流量全部放开又被打挂，如何避免？](answers/582-downstream-recovery-surge.md)
583. [大促前你会做哪些检查？](answers/583-promotion-readiness.md)
584. [大促中核心指标异常，你如何决策降级？](answers/584-promotion-degradation-decision.md)

## 06 架构取舍和高级追问

### 01 架构演进和技术选型

题号：585-594；进度：10/10。

585. [你为什么选择微服务而不是单体？](answers/585-microservices-vs-monolith.md)
586. [你为什么选择本地事务加 Outbox，而不是 TCC？](answers/586-outbox-vs-tcc.md)
587. [你为什么不使用全局分布式事务？](answers/587-avoid-global-transaction.md)
588. [你为什么用 Kafka，而不是 RabbitMQ？](answers/588-kafka-vs-rabbitmq.md)
589. [你为什么用 Redis 缓存，而不是只靠数据库？](answers/589-redis-cache.md)
590. [你为什么用 OpenSearch，而不是 MySQL like 查询？](answers/590-opensearch-vs-mysql.md)
591. [你为什么要做库存预占，而不是支付时再扣库存？](answers/591-inventory-reservation.md)
592. [你为什么要保存价格快照？](answers/592-price-snapshot.md)
593. [你为什么要做支付流水，而不是只更新支付状态？](answers/593-payment-ledger.md)
594. [你为什么要做对账？](answers/594-reconciliation.md)

### 02 一致性、成本和技术债

题号：595-604；进度：10/10。

595. [你为什么要做补偿任务，而不是人工处理所有异常？](answers/595-compensation-jobs.md)
596. [你为什么要做幂等表，而不是只在代码里判断状态？](answers/596-idempotency-table.md)
597. [你为什么要引入 Kubernetes？](answers/597-kubernetes.md)
598. [你为什么要拆 common？](answers/598-common-module.md)
599. [你如何避免 common 变成强耦合中心？](answers/599-common-coupling.md)
600. [你如何判断一个方案过度设计？](answers/600-overengineering.md)
601. [你如何在交付速度和架构质量之间取舍？](answers/601-speed-vs-quality.md)
602. [你如何在一致性和可用性之间取舍？](answers/602-consistency-vs-availability.md)
603. [你如何在成本和性能之间取舍？](answers/603-cost-vs-performance.md)
604. [如果只能做三件事提升稳定性，你做什么？](answers/604-stability-top-three.md)

### 03 团队协作和工程质量

题号：605-610；进度：6/6。

605. [如果只能做三件事提升吞吐，你做什么？](answers/605-throughput-top-three.md)
606. [如果只能做三件事降低成本，你做什么？](answers/606-cost-top-three.md)
607. [如果让你重构当前系统，你优先改什么？](answers/607-refactor-priorities.md)
608. [当前系统最大的技术债是什么？](answers/608-technical-debt.md)
609. [当前系统最大的生产风险是什么？](answers/609-production-risk.md)
610. [你如何证明这个系统不是玩具项目？](answers/610-not-toy-project.md)

## 07 Amazon L6 行为面试

### 01 STAR 和 Leadership Principles

题号：611-622；进度：12/12。

611. [讲一次你主导复杂系统设计的经历。](answers/611-complex-system-design.md)
612. [讲一次你在需求模糊时如何拆解问题。](answers/612-ambiguous-requirements.md)
613. [讲一次你发现并修复深层技术问题的经历。](answers/613-deep-technical-issue.md)
614. [讲一次你推动团队采用更好工程实践的经历。](answers/614-engineering-practices.md)
615. [讲一次你和别人技术意见不一致时如何处理。](answers/615-technical-disagreement.md)
616. [讲一次你做出技术取舍的经历。](answers/616-technical-tradeoff.md)
617. [讲一次你为了长期质量牺牲短期速度的经历。](answers/617-quality-over-speed.md)
618. [讲一次你为了业务交付接受技术债的经历。](answers/618-accepted-technical-debt.md)
619. [讲一次你处理线上事故的经历。](answers/619-production-incident.md)
620. [讲一次你在事故后推动系统性改进的经历。](answers/620-post-incident-improvement.md)
621. [讲一次你用数据证明方案有效的经历。](answers/621-data-driven-proof.md)
622. [讲一次你降低系统成本的经历。](answers/622-cost-reduction.md)

### 02 影响力、取舍和跨团队推动

题号：623-635；进度：13/13。

623. [讲一次你提升系统可用性的经历。](answers/623-availability-improvement.md)
624. [讲一次你提升性能或容量的经历。](answers/624-performance-capacity.md)
625. [讲一次你 mentor 其他工程师的经历。](answers/625-mentoring.md)
626. [讲一次你影响多个团队的经历。](answers/626-cross-team-influence.md)
627. [讲一次你面对失败项目如何复盘。](answers/627-failed-project-retro.md)
628. [讲一次你主动承担超出职责范围工作的经历。](answers/628-beyond-scope-ownership.md)
629. [讲一次你拒绝不合理方案的经历。](answers/629-reject-unreasonable-plan.md)
630. [讲一次你把复杂问题讲清楚给非技术人员的经历。](answers/630-explain-to-nontechnical.md)
631. [讲一次你坚持高标准的经历。](answers/631-high-standards.md)
632. [讲一次你快速学习陌生领域并交付的经历。](answers/632-learn-new-domain.md)
633. [讲一次你处理安全或合规风险的经历。](answers/633-security-compliance.md)
634. [讲一次你没有足够资源但仍然交付结果的经历。](answers/634-limited-resources.md)
635. [讲一次你做长期架构演进规划的经历。](answers/635-architecture-roadmap.md)

## 08 现场编码和设计实现

### 01 基础数据结构和算法

题号：636-644；进度：9/9。

636. [手写 LRU Cache。](answers/636-lru-cache.md)
637. [手写令牌桶限流器。](answers/637-token-bucket.md)
638. [手写滑动窗口限流器。](answers/638-sliding-window-limiter.md)
639. [手写线程安全单例。](answers/639-thread-safe-singleton.md)
640. [手写阻塞队列。](answers/640-blocking-queue.md)
641. [手写生产者消费者模型。](answers/641-producer-consumer.md)
642. [手写简化线程池。](answers/642-simple-thread-pool.md)
643. [手写延迟队列。](answers/643-delay-queue.md)
644. [手写重试工具，支持指数退避和 jitter。](answers/644-retry-backoff-jitter.md)

### 02 后端基础组件实现

题号：645-654；进度：10/10。

645. [手写熔断器状态机。](answers/645-circuit-breaker.md)
646. [手写幂等处理器。](answers/646-idempotency-processor.md)
647. [手写订单状态机。](answers/647-order-state-machine.md)
648. [手写库存条件扣减逻辑。](answers/648-inventory-deduction.md)
649. [手写 Outbox Relay 核心逻辑。](answers/649-outbox-relay.md)
650. [手写 MQ 消费端去重逻辑。](answers/650-mq-consumer-dedup.md)
651. [手写 HMAC 签名校验。](answers/651-hmac-verification.md)
652. [手写敏感字段脱敏工具。](answers/652-data-masking.md)
653. [手写统一异常处理。](answers/653-exception-handler.md)
654. [手写分页查询接口。](answers/654-pagination-api.md)

### 03 分布式可靠性组件实现

题号：655-663；进度：9/9。

655. [手写分布式 ID 生成器简化版。](answers/655-distributed-id.md)
656. [手写一致性 hash。](answers/656-consistent-hash.md)
657. [手写 Top K 统计。](answers/657-top-k.md)
658. [手写限时任务执行器。](answers/658-timed-task-executor.md)
659. [手写一个可关闭的后台 worker。](answers/659-closeable-worker.md)
660. [手写 SQL 查询最近 30 天每天下单量。](answers/660-sql-daily-orders.md)
661. [手写 SQL 查询支付对账差异。](answers/661-sql-reconciliation-differences.md)
662. [手写 SQL 找出重复 request_id。](answers/662-sql-duplicate-request-id.md)
663. [手写 Java 代码解析并聚合日志错误码。](answers/663-log-error-aggregation.md)

## 09 反问面试官

### 01 团队质量判断

题号：664-668；进度：5/5。

664. [这个团队负责的核心业务指标是什么？](answers/664-core-business-metrics.md)
665. [当前系统最大的稳定性挑战是什么？](answers/665-stability-challenges.md)
666. [团队如何做 on-call 和事故复盘？](answers/666-oncall-incident-review.md)
667. [服务规模大概是多少 QPS、数据量和实例数？](answers/667-service-scale.md)
668. [团队使用哪些技术栈和部署平台？](answers/668-tech-stack.md)

### 02 业务和成长空间判断

题号：669-673；进度：5/5。

669. [当前系统是一体化架构还是微服务架构？](answers/669-architecture-style.md)
670. [团队如何做灰度、回滚和容量评估？](answers/670-release-capacity.md)
671. [团队如何衡量 Senior/L6 工程师的影响力？](answers/671-senior-impact.md)
672. [新人加入后前三个月通常会负责什么？](answers/672-new-hire-ramp.md)
673. [团队当前最需要解决的技术债是什么？](answers/673-team-technical-debt.md)

## 使用建议

- 基础不稳时先读 01、02、03，补齐 Java 后端和分布式底层能力。
- 面系统设计时重点读 04，并把每题按容量、数据流、一致性、降级和观测来讲。
- 面生产经验时重点读 05，把回答组织成止血、定位、恢复、复盘四段。
- 面高级岗位时重点读 06 和 07，体现判断力、影响力和跨团队推动能力。
- 面现场编码时重点读 08，先实现小而正确的核心逻辑，再补并发和异常场景。
