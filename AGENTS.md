# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

资源管控中心 · 数据库运维管理平台 (Resource Control Center · Database O&M Management Platform) — a Java 17 Swing desktop application for Oracle and GaussDB (Huawei's PostgreSQL-based database) operations. Built with Maven, FlatLaf UI theme, and Apache POI for Excel export.

## Build & Run

```bash
# Build the project
mvn clean compile

# Run all tests
mvn test

# Run a single test class
mvn -Dtest=DataSourceTest test

# Package as executable JAR (includes all dependencies via maven-shade-plugin)
mvn clean package

# Run the packaged JAR from project root (required — DATASOURCE.JSON and config.yaml are read from user.dir)
java -jar target/db-project-1.0-SNAPSHOT.jar

# Entry point: com.sunzh.launcher.AppLauncher
```

## Architecture

### Package Layout

| Package | Role |
|---|---|
| `com.sunzh.launcher` | Entry point (`AppLauncher`), sets up FlatLaf theme then launches `MainFrame` |
| `com.sunzh.core` | Domain objects and infrastructure: `DataSource` (entity with Base64 password crypto), `DataSourceStore` (JSON persistence to `DATASOURCE.JSON`), `ConnectionManager` (JDBC connection testing for Oracle/GaussDB), `ScriptRunner` (SQL file migration with checkpoint/resume) |
| `com.sunzh.ui` | Swing GUI. `MainFrame` is the hub with a menu bar launching feature dialogs. `BaseDialog` is the abstract modal dialog base (1200x800, auto-refresh on open). Dialogs live in `ui/dialogs/`, reusable components in `ui/components/` |
| `com.sunzh.service` | `CheckModelService` — the inspection engine: loads tasks from `config.yaml`, executes SQL queries, exports results as `.xlsx` files |
| `com.sunzh.sync` | Standalone data sync tools (`OracleToGaussDB`, `GaussDBToOracle`, `ExcelToOracle`, `ExcelToGaussDB`). These are `main()` classes **spawned as subprocesses** by `DataSyncDialog` via `ProcessBuilder` — they don't run in-process |
| `com.sunzh.comparison` | Schema comparison feature: `ComparisonService` calls stored procedures (`sp_extract_source_data`, `sp_generate_all_compare`) and queries result tables (`gk_sjdb_*`) on GaussDB. `ComparisonDialog` has tabbed panels (`ExtractPanel`, `ComparePanel`, `DetailPanel`, `TaskConfigPanel`) |
| `com.sunzh.checkmodel` | `InspectionTask` entity with `Status` enum (`PENDING/SUCCESS/NO_DATA/FAILED/SKIPPED`) |
| `com.sunzh.utils` | `CryptoUtils` (Base64 encode/decode for passwords — not real encryption, fallback to plaintext on decode failure), `ThemeUtils` (FlatLaf color/font constants), `SvgIconUtils` |

### Key Design Decisions

- **Data sources** are persisted in `DATASOURCE.JSON` at the project root (`user.dir`). Passwords are Base64-encoded (not truly encrypted). The `DataSource.getPassword()` auto-decrypts, `setPassword()` auto-encrypts.
- **Config YAML** (`config.yaml`) defines inspection tasks — each task maps a `description` + `enabled` flag to a SQL file in the `query/` directory. `CheckModelService.loadTasks()` resolves SQL files relative to a `queryDir` parameter.
- **Data sync runs as subprocesses** — `DataSyncDialog` spawns `java -cp <classpath> <sync-class> <args>` via `ProcessBuilder`. This means sync classes (`com.sunzh.sync.*`) use `System.out.println` for logging (captured by the dialog's log panel).
- **Schema comparison** requires a GaussDB connection with pre-existing stored procedures (`sp_extract_source_data`, `sp_extract_target_data`, `sp_generate_all_compare`) and result tables (`gk_sjdb_task`, `gk_sjdb_task_config`, `gk_sjdb_table_jg`, `gk_sjdb_column_jg`, etc.).
- **ScriptRunner** supports checkpoint/resume — it stores SQL file content and parsed statements in `general_app_form` / `general_app_form_parsed` tables, tracking execution status per statement.
- **Dual database support**: JDBC URLs are built dynamically by `DataSource.buildUrl()` based on the `type` field (`ORACLE` or `GAUSSDB`). Oracle uses `serviceName`, GaussDB uses `database` + `schema`.

### Runtime Dependencies

- `config.yaml` and `DATASOURCE.JSON` must exist at the working directory (`user.dir`)
- GaussDB JDBC driver (`com.huaweicloud.gaussdb:gaussdbjdbc:506.0.0.b058`) — this artifact is from Huawei's Maven repository, may need manual installation
- Oracle JDBC driver (`com.oracle.database.jdbc:ojdbc8:21.9.0.0`) with i18n support (`orai18n`)
