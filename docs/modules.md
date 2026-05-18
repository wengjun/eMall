# eMall 模块清单

本文列出仓库中的 Maven 模块，并按职责分组。真实构建顺序以根目录 `pom.xml` 为准。

## 概览

- 业务和工具模块：42 个。
- 根聚合工程：1 个。
- 平台基础模块：3 个。
- 运行时业务和数据模块：29 个。
- 信任和治理模块：6 个。
- 生产控制面模块：4 个。
- 验证工具模块：2 个。
- 单元测试基线：每个 Maven 模块至少有一个 `*Test.java`。
- 集成测试基线：每个 Maven 模块至少有一个 `*IntegrationTest.java`。

## 目录定位

所有 Maven 模块继续平铺在根目录，模块的业务分组不通过物理目录表达，而通过本文档和 Maven profile 表达。这样做的目的是：

- 保持 `mvn -pl 模块名 -am verify` 命令简单。
- 保持 Dockerfile、Docker Compose、CI 和 IDE 导入路径稳定。
- 避免为了目录美观引入大量路径迁移和文档链接修改。
- 让学习者可以先按本文分组理解模块，再回到根目录定位代码。

## 构建顺序

| 顺序 | 模块 | 类型 | 职责 |
| --- | --- | --- | --- |
| 1 | `common` | 共享库 | API 响应、错误码、事件、Outbox、审计、幂等、ID、安全工具 |
| 2 | `governance` | 共享治理 | 下游平滑恢复、多区域路由基础能力 |
| 3 | `gateway` | 接入服务 | 网关路由、Redis 限流、链路 ID 透传、边缘控制 |
| 4 | `user` | 核心服务 | 用户生命周期、手机号加密存储 |
| 5 | `product` | 核心服务 | 商品生命周期、价格变更、上下架、搜索事件 |
| 6 | `inventory` | 核心服务 | 库存、库存桶、预占、确认、释放、恢复 |
| 7 | `order` | 核心服务 | 幂等下单、快照、支付确认、取消、补偿 |
| 8 | `cart` | 核心服务 | 购物车操作和高频购物车流量隔离 |
| 9 | `payment` | 核心服务 | 支付、回调、退款、流水、渠道对账 |
| 10 | `pricing` | 核心服务 | 价格本、缓存读取、下单报价快照 |
| 11 | `marketing` | 核心服务 | 优惠券、核销、取消、促销报价降级 |
| 12 | `search` | 核心服务 | 商品索引、查询、下架、商品事件投影 |
| 13 | `fulfillment` | 核心服务 | 履约单、仓库分配、承运商路线、发货、签收 |
| 14 | `review` | 核心服务 | 评价提交、审核、发布、拒绝、评分汇总 |
| 15 | `after-sales` | 核心服务 | 退货、退款、换货、审批、拒绝、关闭 |
| 16 | `merchant` | 商家服务 | 商家入驻、店铺、佣金规则、结算、发票 |
| 17 | `flash-sale` | 流量服务 | 秒杀活动、库存预分配、令牌、请求排队 |
| 18 | `recommendation` | 增长服务 | 用户偏好、商品特征、行为采集、排序、实验 |
| 19 | `chaos` | 运维工具 | 带安全门禁的 Java 混沌演练目录 |
| 20 | `cost` | 治理服务 | 预算、成本风险信号、优化动作、存储层级、导出 |
| 21 | `identity` | 信任服务 | 账号、会话、RBAC、服务客户端、访问决策、子账号 |
| 22 | `risk` | 信任服务 | 风控规则、设备信誉、反机器人、风险事件 |
| 23 | `operations` | 治理服务 | 审批、运维任务、合规证据、安全事件 |
| 24 | `openapi` | 平台服务 | 商家应用、签名、配额、Webhook、投递记录 |
| 25 | `catalog` | 业务平台 | 类目、属性、品牌授权、SPU/SKU 治理、审核 |
| 26 | `promotion` | 业务平台 | 活动生命周期、叠加优惠、预算消耗、活动日历 |
| 27 | `experiment` | 业务平台 | 实验、互斥组、流量分配、护栏、报告 |
| 28 | `advertising` | 业务平台 | 广告活动、素材、定向、排序、预算扣减 |
| 29 | `supply-chain` | 业务平台 | 入库、上架、调拨、运单、异常、签收凭证 |
| 30 | `finance` | 业务平台 | 账户、流水、冻结资金、结算、发票、拒付 |
| 31 | `customer-service` | 业务平台 | 工单、分派、仲裁、补偿、知识库、服务评价 |
| 32 | `forecasting` | 业务平台 | 需求信号、预测、补货、产能、风险汇总 |
| 33 | `event-platform` | 数据平台 | 事件 schema、埋点采集、offset、指标物化 |
| 34 | `data-warehouse` | 数据平台 | ODS、DWD、DWS、ADS、分区、质量检查、血缘 |
| 35 | `intelligence` | AI 平台 | 用户画像、商品画像、在线特征、模型部署、AI 决策 |
| 36 | `analytics` | 分析平台 | 指标、看板、异常、授权、隐私请求 |
| 37 | `traffic` | 生产控制面 | 单元、分片路由、流量切换、故障半径隔离 |
| 38 | `reliability` | 生产控制面 | 容量演练、SLO、混沌审批、上线门禁 |
| 39 | `release` | 生产控制面 | 功能开关、灰度、Topic 治理、延迟预算、回放计划 |
| 40 | `platform-ops` | 生产控制面 | 备份、数据库操作、FinOps、安全操作 |
| 41 | `smoke` | 验证工具 | Java smoke 测试客户端，验证核心下单和支付链路 |
| 42 | `loadtest` | 验证工具 | Java 压测客户端，验证下单和流量尖峰场景 |

## 职责分组

### 平台基础

- `common`
- `governance`
- `gateway`

### 核心电商

- `user`
- `product`
- `inventory`
- `order`
- `cart`
- `payment`
- `pricing`
- `marketing`
- `search`
- `fulfillment`
- `review`
- `after-sales`

### 商家和增长

- `merchant`
- `flash-sale`
- `recommendation`
- `catalog`
- `promotion`
- `experiment`
- `advertising`

### 供应、财务和客服

- `supply-chain`
- `finance`
- `customer-service`
- `forecasting`

### 数据、AI 和分析

- `event-platform`
- `data-warehouse`
- `intelligence`
- `analytics`

### 信任、风险和治理

- `identity`
- `risk`
- `operations`
- `openapi`
- `chaos`
- `cost`

### 生产规模控制面

- `traffic`
- `reliability`
- `release`
- `platform-ops`

### 验证工具

- `smoke`
- `loadtest`

## Maven Profile

根 `pom.xml` 保持模块目录扁平，但通过 profile 提供逻辑构建组。默认 `all-modules` profile 会在没有选择
其他 profile 时启用，所以 `mvn verify` 会构建整个仓库。

| Profile | 模块范围 | 典型用途 |
| --- | --- | --- |
| `all-modules` | 所有模块 | 全仓库验证 |
| `common-libraries` | `common`、`governance` | 共享库验证 |
| `core-services` | 网关和核心交易服务 | 核心运行时验证 |
| `business-platforms` | 商家、增长、供应、财务、客服 | 业务平台验证 |
| `data-platforms` | 事件、数仓、智能、分析 | 数据和 AI 平台验证 |
| `trust-governance` | 身份、风控、运营、OpenAPI、混沌、成本 | 治理能力验证 |
| `production-control` | 流量、可靠性、发布、平台运维 | 生产控制面验证 |
| `tools` | `smoke`、`loadtest` | 工具验证 |
| `stable-runtime` | 核心服务和 `smoke` | 最小稳定运行时验证 |

示例：

```powershell
mvn -Pcore-services verify
mvn -Pbusiness-platforms verify
mvn -Pproduction-control verify
mvn -Pstable-runtime verify
```
