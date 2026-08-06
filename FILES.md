# 项目文件与功能说明

## 项目概述

资源管控中心 · 数据库运维管理平台 — Java 17 Swing 桌面应用，支持 Oracle 和 GaussDB (华为高斯数据库) 的运维操作。

## 包结构与文件说明

### `com.sunzh.inspection` — 数据库巡检模块

| 文件 | 说明 |
|---|---|
| `InspectionTask.java` | 巡检任务实体：描述、SQL、启用状态、执行状态（PENDING/SUCCESS/NO_DATA/FAILED/SKIPPED）|
| `InspectionService.java` | 巡检引擎：从 `config.yaml` 加载任务，执行 SQL 查询，将结果导出为 `.xlsx` 文件 |
| `InspectionDialog.java` | 巡检功能对话框 UI：任务列表、SQL 预览、进度条、报告浏览、输出目录选择、一键执行 |

### `com.sunzh.datasource` — 数据源配置模块

| 文件 | 说明 |
|---|---|
| `DataSourceDialog.java` | 数据源配置对话框：增删改查 Oracle/GaussDB 数据源，测试连接，保存至 `DATASOURCE.JSON` |

### `com.sunzh.core` — 公共基础设施

| 文件 | 说明 |
|---|---|
| `DataSource.java` | 数据源实体：名称、类型(ORACLE/GAUSSDB)、连接参数、Base64 密码编解码、动态构建 JDBC URL |
| `DataSourceStore.java` | 数据源 JSON 文件读写：从 `DATASOURCE.JSON` 加载 / 保存数据源列表 |
| `ConnectionManager.java` | 数据库连接测试工具：根据数据源类型注册 JDBC 驱动并测试连接 |

### `com.sunzh.sync` — 数据同步模块

| 文件 | 说明 |
|---|---|
| `OracleToGaussDB.java` | 同步工具 (main class)：Oracle 表 → GaussDB 表，支持表重命名(`OLD:NEW`)、追加/覆盖两种模式，流式读取+批量写入 |
| `GaussDBToOracle.java` | 同步工具 (main class)：GaussDB 表 → Oracle 表（反向），同上批量和追加模式 |
| `ExcelToOracle.java` | 导入工具 (main class)：Excel 文件 → Oracle 表，中文列名自动转拼音 |
| `ExcelToGaussDB.java` | 导入工具 (main class)：Excel 文件 → GaussDB 表 |
| `DataSyncDialog.java` | 数据同步对话框 UI：四个 Tab（O→G / G→O / Excel→Oracle / Excel→GaussDB），选择数据源、配置表映射、通过 `ProcessBuilder` 启动同步子进程，实时查看日志 |

### `com.sunzh.scriptrunner` — SQL 脚本执行模块

| 文件 | 说明 |
|---|---|
| `ScriptRunner.java` | SQL 迁移引擎：读取 SQL 文件 → 入库到 `general_app_form` → 解析 SQL → 入解析表 `general_app_form_parsed` → 顺序执行 DDL → 更新执行状态。支持断点续传 |
| `ScriptRunnerDialog.java` | SQL 脚本执行对话框 UI：选择数据源、浏览上传 SQL 文件、查看执行进度和日志 |

### `com.sunzh.ui` — 应用外壳与通用 UI

| 文件 | 说明 |
|---|---|
| `MainFrame.java` | 主窗口：标题栏、菜单栏（功能入口）、内容面板、底部状态栏、启动入口 |
| `BaseDialog.java` | 所有功能对话框的抽象基类：JDialog 封装，1200×800，自动居中，打开时触发 `refresh()` |
| `SchemaCompareDialog.java` | 数据库结构对比对话框（预留待开发） |
| `SettingsDialog.java` | 系统设置对话框（预留待开发） |
| `components/CustomButton.java` | 统一样式按钮组件：墨绿主题 |
| `components/StatusBar.java` | 底部状态栏组件 |

### `com.sunzh.comparison` — 数据库结构对比模块

| 文件 | 说明 |
|---|---|
| `ComparisonDialog.java` | 结构对比主对话框：数据源选择 + Tab 切换面板 |
| `ComparisonService.java` | 对比业务逻辑：调用 GaussDB 存储过程抽取/对比数据、查询 `gk_sjdb_*` 结果表、CRUD 任务/配置 |
| `model/ComparisonTask.java` | 对比任务实体，对应数据库表 `gk_sjdb_task` |
| `model/ComparisonTaskConfig.java` | 对比配置实体，对应数据库表 `gk_sjdb_task_config` |
| `panels/ExtractPanel.java` | 抽取 Tab：选择 schema 和对象类型（表/列/索引/序列/同义词），调用 `sp_extract_source_data` / `sp_extract_target_data` |
| `panels/ComparePanel.java` | 对比 Tab：任务列表、多选执行、结果汇总表（表/列/索引/序列/同义词的差异数量） |
| `panels/DetailPanel.java` | 明细 Tab：按类型 + JOB_ID 查询对比结果明细，支持导出 Excel |
| `panels/TaskConfigPanel.java` | 配置 Tab：编辑 `gk_sjdb_task_config` 和 `gk_sjdb_task` 表，初始化生成任务 |

### `com.sunzh.launcher` — 启动入口

| 文件 | 说明 |
|---|---|
| `AppLauncher.java` | `main()` 入口：应用 FlatLaf 主题 + 自定义色板，启动 `MainFrame` |

### `com.sunzh.utils` — 工具类

| 文件 | 说明 |
|---|---|
| `CryptoUtils.java` | Base64 编解码（密码存储），解码失败时回退返回明文 |
| `ThemeUtils.java` | 全局 UI 常量：颜色（墨绿主色调/浅灰背景）、字体（标题/正文/小字）、FlatLaf 主题配置方法 |
| `SvgIconUtils.java` | SVG 图标加载器：从 `resources/icons/` 读取 SVG，着色、缩放、缓存为 `ImageIcon` |

## 运行时数据文件（根目录）

| 文件/目录 | 说明 |
|---|---|
| `config.yaml` | 巡检任务配置：每个任务 = 描述 + SQL 文件 + 启用标志 |
| `DATASOURCE.JSON` | 数据源列表：名称、类型、主机、端口、数据库/Schema、用户、Base64 编码密码 |
| `query/` | 巡检 SQL 文件目录（28 个 SQL），由 `config.yaml` 引用 |
| `reports/` | 巡检结果输出目录，每次执行生成时间戳子目录，内含多个 `.xlsx` 报告 |
| `pom.xml` | Maven 项目定义：Java 17、FlatLaf、POI、SnakeYAML、Batik、Gson、pinyin4j、Oracle/GaussDB JDBC |
| `src/main/resources/` | `flatlaf.properties`（主题配置）+ `sql/`（通用 DDL 脚本） |
