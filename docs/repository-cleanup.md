# 仓库整理说明

本文记录文档、配置和本地文件的整理原则，避免仓库越来越乱。

## 总结

当前仓库已经按以下原则整理：

- 文档集中在 `docs`。
- 运维和部署配置集中在 `ops`。
- 本地运行配置保留在根目录和 `ops/env`，例如 `docker-compose.yml`、`.env.example` 和
  `ops/env/local.env`。
- Maven 模块目录保持扁平，逻辑分组通过 Maven profile 和文档表达。

## 目录结构原则

当前仓库不把 42 个 Maven 模块物理移动到 `services`、`platform`、`tools` 这类多级目录。模块平铺虽然让根目录看起来较多，
但能保持 Maven、Docker、CI、IDE 和文档路径稳定，降低学习和调试时的路径跳转成本。

模块治理采用“物理目录扁平、逻辑分组清晰”的方式：

- 根目录只放 Maven 模块、项目入口文件和少量本地运行配置。
- 模块分组以 `docs/modules.md`、README 表格和 Maven profile 表达。
- 运行部署材料进入 `ops`。
- 架构、学习、测试、路线图和面试材料进入 `docs`。
- 只有当模块数量继续大幅增加，且 CI、Docker、IDE 和文档迁移成本可控时，才考虑物理目录搬迁。

## 本地文件

- `.idea/`：本地 IDE 配置，可以保留在个人工作区，用于看代码。
- `target/`：Maven 生成目录，可以保留在本地，但不应作为源码发布内容。
- 日志、临时文件、构建产物不应提交。

## 应保留的文件

- `pom.xml`：根 Maven 聚合工程。
- `README.md`：项目入口。
- `docs/`：架构、路线图、测试、运维和学习文档。
- `ops/`：部署、观测、MySQL、混沌、压测等配置。
- `.github/workflows/ci.yml`：CI 校验。
- `.editorconfig`、Checkstyle 等格式配置。

## 主文档

建议把这些作为主要学习入口：

- `README.md`
- `docs/README.md`
- `docs/design-deep-dive.md`
- `docs/architecture.md`
- `docs/modules.md`
- `docs/roadmap.md`
- `docs/integration-testing.md`
- `docs/production-checklist.md`

## 已合并的文档类型

之前较分散的内容已经合并：

- trace 验证合并到可观测性文档。
- 密钥轮换和风控合并到安全加固文档。
- 数据生命周期和同步合并到数据平台文档。
- 容量和可靠性合并到 SLO 与故障手册。
- P4 到 P8 阶段说明合并到阶段说明文档。

## 暂时不要删除

- `target/`：用户明确表示本地可以保留。
- `.idea/`：用户明确表示用于看代码。
- `ops/`：虽然文件多，但属于生产部署和验证资料。
- `docs/`：当前是学习和面试讲解的主要资料。

## 推荐整理顺序

1. 先保持文档中文化，方便学习。
2. 再检查文档是否重复。
3. 再清理真正无引用的旧文件。
4. 最后再决定是否调整模块目录结构。
