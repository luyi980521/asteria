# Reconciliation v1

本模块只提供领域模型、基本状态转换、Repository 持久化端口及 PostgreSQL DDL。
未引入 Controller、业务应用服务、Consumer、调度任务、Matcher 或自动恢复逻辑。

## 项目规范与模块接入

检查了根 POM、Payment（含 PaymentOutboxTransactionService）、Ledger、Settlement、Balance、
Common 和 Bootstrap 的代表实现。仓库及检查到的父目录中未发现 AGENTS.md。
现有仓库没有 Flyway/Liquibase 或版本化迁移执行器；沿用各模块的
`src/main/resources/sql/*.sql`，不另建迁移框架。

包结构沿用 `domain/entity|enums|valueobject|repository|error|exception` 和
`infrastructure/persistence/dataobject|converter|mapper|repository`。
Entity 使用 Lombok Getter、私有校验构造器上的 Builder，持久化对象使用 DO 后缀；
Converter 显式转换 ID、枚举、金额、币种；Mapper 使用 BaseMapper 和同名 XML；
Repository 使用 Optional 与构造器注入。中文 JavaDoc 说明业务限制及逻辑关联。

所有六类 ID 均为正数 Long 值对象，由未来调用方通过已有
`DistributedIdGenerator.nextId()` 分配；不引入 UUID、自增序列或另一套 ID 生成器。
模块只依赖 Common、MyBatis-Plus/PostgreSQL 和测试库。
Bootstrap 增加模块依赖后，现有 `io.asteria` 组件扫描、Mapper 注解扫描和
`classpath*:mapper/**/*.xml` 已覆盖新模块，无需新扫描配置。

## 领域与表

| 领域对象 / 表 | 主要数据与约束 |
| --- | --- |
| ReconciliationRule / reconciliation_rule | 稳定编码、名称、类型、左右来源、启用状态、宽限秒数、配置说明、版本和审计时间；v1 仅允许 PAYMENT→LEDGER、SETTLEMENT→LEDGER |
| ReconciliationSourceRecord / reconciliation_source_record | 来源类型、原始 ID、非负来源版本、业务键、可选 Money、原始状态、记账状态、业务时间、payload 和采集时间；不可变且不绑定单个 Run |
| ReconciliationRun / reconciliation_run | 规则编码/版本/完整快照、业务窗口、采集截止时间、状态、六项计数、开始/结束/失败信息、版本和审计时间；不持有来源或结果集合 |
| ReconciliationMatchResult / reconciliation_match_result | Run ID、规则、业务键、结果状态、主要差异类型、两侧独立 Money、来源快照 ID 数组、details 和创建时间；不可变 |
| ReconciliationCase / reconciliation_case | 规则/业务键/差异、首次发现的 Run 与 Result、状态、责任人、发现说明、关闭结论和时间、版本和审计时间 |
| ReconciliationCaseAction / reconciliation_case_action | Case ID、动作、主体类型/标识、前后状态、原因、details 和创建时间；追加式审计，不执行资金操作 |

SourceRecord 的类型化快照主键名为 `recordId`，原始业务 ID 名为 `sourceRecordId`，
二者语义独立；数据库分别为 `id` 与 `source_record_id`。

金额沿用公共 `Money`/`CurrencyCode`，数据库采用 Payment/Ledger/Balance 已有的
`NUMERIC(38,18)` 和 `VARCHAR(16)`。标准化金额允许零，缺失时金额与币种同时为空；
两侧金额可分别保存不同币种，便于表达币种差异。Builder 拒绝超出数据库无损范围的金额。
业务借贷方向放在原始快照中；负数净头寸未来需要明确方向约定，不能直接作为 Money 传入。

全部时间采用 `Instant`/`TIMESTAMPTZ`，表示 UTC 时间点。
业务窗口为 `[windowStart, windowEnd)`；来源按 `occurred_at` 归属窗口，
`created_at <= sourceCutoffAt` 表示采集可见范围，截止点不得早于窗口终点或晚于 Run 创建。
宽限期是未来 Matcher 判定 PENDING/MISSING 的输入，本次不实现判断算法。

`payload`、`details`、`source_record_ids`、`rule_snapshot`、失败原因和工单结论均为
PostgreSQL TEXT / Java String，没有 JSONB 列或 JSON 类型处理器。
JSON 对象及 ID 数组使用 Common 的 JsonUtils 序列化、校验；Converter 原样保存字符串，
避免再次序列化成带转义的 JSON 字符串。来源 ID 数组必须非空，且均为无重复的正数 Long。
JSON 语法与数组元素约束在领域层执行，数据库 TEXT 不验证 JSON 内容。

## 关键唯一性与状态约束

- SourceRecord：`UNIQUE(source_type, source_record_id, source_version)`。
  重复采集不能覆盖原始证据；同一原始记录不同版本可以并存。
  此约束用于采集幂等，业务层重复记录仍需由未来 Matcher 检测。
- MatchResult：`UNIQUE(run_id, rule_code, business_key)`。
  同一 Run 每个业务键只保留一个结论，重跑创建新 Run 以保存历史。
- Case：部分唯一索引
  `UNIQUE(rule_code, business_key, difference_type) WHERE status IN ('OPEN','INVESTIGATING')`。
  终态释放活动键，后续差异可以再建新 Case；不同差异类型可并存。
- Rule：`UNIQUE(rule_code)`。规则配置版本由条件更新递增，Run 保存执行时完整规则 JSON，
  旧版本规则不单独建表。
- 全表采用 BIGINT 应用主键、正数 ID 检查及逻辑关联，无物理外键。
  索引沿用 `idx_`/`uk_`、检查约束沿用 `ck_` 命名；
  包含窗口、运行状态、结果状态、工单队列和审计历史访问索引。
  每张表、每个字段均有 COMMENT，关键约束及所有索引均有 SQL 说明。

Run 仅允许 CREATED→RUNNING→COMPLETED/FAILED；终态不可重启。
完成计数按业务键统计，五个互斥匹配分类之和等于 totalCount；未完成/失败时不保存完整统计。
FAILED 必须提供原因，COMPLETED 可以包含差异和 PENDING，并不表示所有业务一致。

MATCHED/PENDING 的 differenceType 必须为空；MISSING 仅使用缺少来源的分类；
DUPLICATED 仅使用 DUPLICATE_RECORD；MISMATCHED 使用金额、币种、状态、分录或非预期记录分类。
一个结果目前保存一个主要差异，其余证据可放 details；多差异优先级待 Matcher 设计明确。

Case 仅允许 OPEN→INVESTIGATING、活动状态→RESOLVED/IGNORED；
调查必须指定责任人，关闭必须说明原因，终态不可重开。
MATCHED/PENDING 不能通过 open 工厂建立 Case。
Action 的 OPEN/INVESTIGATE/RESOLVE/IGNORE/COMMENT 必须与前后状态一致；
SYSTEM 和 HUMAN 都必须提供操作主体标识。动作主键用于调用方重试幂等，重试须复用该 ID。

Rule、Run、Case 沿用 Balance 的 `WHERE id = ? AND version = ?` 条件更新；
SQL 更新后递增版本，不修改调用方旧快照的版本，成功后应重新读取再进行下次变更。
版本冲突或目标不存在抛 ReconciliationDomainException(CONCURRENT_MODIFICATION)。
唯一性冲突保留 Spring/MyBatis 的持久化异常，不把冲突自动当作成功。

## DDL 使用与验证

新增脚本仅有 CREATE/COMMENT，不含 DROP、TRUNCATE、覆盖插入或业务数据迁移。
部署时按 rule、source_record、run、match_result、case、case_action 顺序，
在项目已有的 SQL 发布流程内执行一次；脚本自身不自动运行，也不使用 IF NOT EXISTS 掩盖结构漂移。
可由发布事务包裹六份 DDL，避免部分建表成功。

```powershell
mvn -DskipTests compile
mvn -pl asteria-reconciliation -am test
```

真实 PostgreSQL 检查沿用 Balance 的环境变量开关。配置
`ASTERIA_RECONCILIATION_TEST_JDBC_URL`、
`ASTERIA_RECONCILIATION_TEST_DB_USER`、
`ASTERIA_RECONCILIATION_TEST_DB_PASSWORD` 后执行第二条命令。
不配置 URL 时仅跳过 PostgreSQL 检查，领域测试照常运行。

本次已执行整个 12 模块 reactor 编译及配置本地 PostgreSQL 的模块测试：
8 项领域检查、7 项真实 PostgreSQL 检查、21 项 Common 检查全部通过，无跳过。
数据库检查只将交付 DDL 中 CREATE TABLE 替换为 CREATE TEMPORARY TABLE，
保留其他约束、索引和注释，验证六套 Mapper/Converter/Repository。
每项检查使用独立连接，结束时回滚并关闭连接，不向现有业务表写入。
现有 bootstrap 的 spring-kafka-test POM 缓存缺失会产生编译警告；
本次未运行整个仓库的所有测试或已有 E2E。

## 后续实现边界

未来采集方需先约定带商户/渠道命名空间的 businessKey、sourceRecordId，
以及无原生版本来源的稳定单调版本映射。统一业务键是跨模块关联的前提，
不要直接把所有渠道的原始流水号当作全局唯一值。

sourceCutoffAt 是过滤条件，不能单独保证并发提交下的数据快照一致性。
后续必须约定来源采集完成水位与读取隔离级别，在截止点内选择有效来源版本，
并落实分页扫描、迟到数据与跨窗口版本变更规则。本次仅提供索引及单行/幂等键查询端口。

跨表的 Rule/Run/Result/Source/Case 对应关系由未来应用事务负责；
数据库未用物理外键或跨表触发器强制一致性。
Case 更新与 CaseAction 追加必须在同一本地事务内提交，
SQL CHECK 验证动作自身的前后状态，但不替代与 Case 当前版本的核对。
不可变对象仅通过领域 Repository 暴露读取及插入端口；
数据库写权限及绕过 Repository 的直接 SQL 管理仍需发布环境约束。

## 文件清单

修改已有文件：根 `pom.xml`（modules 与 dependencyManagement）、
`asteria-bootstrap/pom.xml`（启动模块依赖）、
根 `README.md`（模块入口）。

本模块新增文件如下（路径相对本模块，测试和本说明也在内）：

- `./`：
  - [README.md](README.md)
  - [pom.xml](pom.xml)
- `src/main/java/io/asteria/reconciliation/domain/entity/`：
  - [ReconciliationCase.java](src/main/java/io/asteria/reconciliation/domain/entity/ReconciliationCase.java)
  - [ReconciliationCaseAction.java](src/main/java/io/asteria/reconciliation/domain/entity/ReconciliationCaseAction.java)
  - [ReconciliationMatchResult.java](src/main/java/io/asteria/reconciliation/domain/entity/ReconciliationMatchResult.java)
  - [ReconciliationRule.java](src/main/java/io/asteria/reconciliation/domain/entity/ReconciliationRule.java)
  - [ReconciliationRun.java](src/main/java/io/asteria/reconciliation/domain/entity/ReconciliationRun.java)
  - [ReconciliationSourceRecord.java](src/main/java/io/asteria/reconciliation/domain/entity/ReconciliationSourceRecord.java)
  - [ReconciliationValidation.java](src/main/java/io/asteria/reconciliation/domain/entity/ReconciliationValidation.java)
- `src/main/java/io/asteria/reconciliation/domain/enums/`：
  - [ReconciliationCaseActionType.java](src/main/java/io/asteria/reconciliation/domain/enums/ReconciliationCaseActionType.java)
  - [ReconciliationCaseStatus.java](src/main/java/io/asteria/reconciliation/domain/enums/ReconciliationCaseStatus.java)
  - [ReconciliationDifferenceType.java](src/main/java/io/asteria/reconciliation/domain/enums/ReconciliationDifferenceType.java)
  - [ReconciliationMatchStatus.java](src/main/java/io/asteria/reconciliation/domain/enums/ReconciliationMatchStatus.java)
  - [ReconciliationOperatorType.java](src/main/java/io/asteria/reconciliation/domain/enums/ReconciliationOperatorType.java)
  - [ReconciliationRunStatus.java](src/main/java/io/asteria/reconciliation/domain/enums/ReconciliationRunStatus.java)
  - [ReconciliationSourceType.java](src/main/java/io/asteria/reconciliation/domain/enums/ReconciliationSourceType.java)
  - [ReconciliationType.java](src/main/java/io/asteria/reconciliation/domain/enums/ReconciliationType.java)
- `src/main/java/io/asteria/reconciliation/domain/error/`：
  - [ReconciliationErrorCode.java](src/main/java/io/asteria/reconciliation/domain/error/ReconciliationErrorCode.java)
- `src/main/java/io/asteria/reconciliation/domain/exception/`：
  - [ReconciliationDomainException.java](src/main/java/io/asteria/reconciliation/domain/exception/ReconciliationDomainException.java)
- `src/main/java/io/asteria/reconciliation/domain/repository/`：
  - [ReconciliationCaseActionRepository.java](src/main/java/io/asteria/reconciliation/domain/repository/ReconciliationCaseActionRepository.java)
  - [ReconciliationCaseRepository.java](src/main/java/io/asteria/reconciliation/domain/repository/ReconciliationCaseRepository.java)
  - [ReconciliationMatchResultRepository.java](src/main/java/io/asteria/reconciliation/domain/repository/ReconciliationMatchResultRepository.java)
  - [ReconciliationRuleRepository.java](src/main/java/io/asteria/reconciliation/domain/repository/ReconciliationRuleRepository.java)
  - [ReconciliationRunRepository.java](src/main/java/io/asteria/reconciliation/domain/repository/ReconciliationRunRepository.java)
  - [ReconciliationSourceRecordRepository.java](src/main/java/io/asteria/reconciliation/domain/repository/ReconciliationSourceRecordRepository.java)
- `src/main/java/io/asteria/reconciliation/domain/valueobject/`：
  - [ReconciliationCaseActionId.java](src/main/java/io/asteria/reconciliation/domain/valueobject/ReconciliationCaseActionId.java)
  - [ReconciliationCaseId.java](src/main/java/io/asteria/reconciliation/domain/valueobject/ReconciliationCaseId.java)
  - [ReconciliationMatchResultId.java](src/main/java/io/asteria/reconciliation/domain/valueobject/ReconciliationMatchResultId.java)
  - [ReconciliationRuleId.java](src/main/java/io/asteria/reconciliation/domain/valueobject/ReconciliationRuleId.java)
  - [ReconciliationRunId.java](src/main/java/io/asteria/reconciliation/domain/valueobject/ReconciliationRunId.java)
  - [ReconciliationSourceRecordId.java](src/main/java/io/asteria/reconciliation/domain/valueobject/ReconciliationSourceRecordId.java)
- `src/main/java/io/asteria/reconciliation/infrastructure/persistence/converter/`：
  - [ReconciliationCaseActionPersistenceConverter.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/converter/ReconciliationCaseActionPersistenceConverter.java)
  - [ReconciliationCasePersistenceConverter.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/converter/ReconciliationCasePersistenceConverter.java)
  - [ReconciliationMatchResultPersistenceConverter.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/converter/ReconciliationMatchResultPersistenceConverter.java)
  - [ReconciliationRulePersistenceConverter.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/converter/ReconciliationRulePersistenceConverter.java)
  - [ReconciliationRunPersistenceConverter.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/converter/ReconciliationRunPersistenceConverter.java)
  - [ReconciliationSourceRecordPersistenceConverter.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/converter/ReconciliationSourceRecordPersistenceConverter.java)
- `src/main/java/io/asteria/reconciliation/infrastructure/persistence/dataobject/`：
  - [ReconciliationCaseActionDO.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/dataobject/ReconciliationCaseActionDO.java)
  - [ReconciliationCaseDO.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/dataobject/ReconciliationCaseDO.java)
  - [ReconciliationMatchResultDO.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/dataobject/ReconciliationMatchResultDO.java)
  - [ReconciliationRuleDO.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/dataobject/ReconciliationRuleDO.java)
  - [ReconciliationRunDO.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/dataobject/ReconciliationRunDO.java)
  - [ReconciliationSourceRecordDO.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/dataobject/ReconciliationSourceRecordDO.java)
- `src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/`：
  - [ReconciliationCaseActionMapper.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/ReconciliationCaseActionMapper.java)
  - [ReconciliationCaseMapper.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/ReconciliationCaseMapper.java)
  - [ReconciliationMatchResultMapper.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/ReconciliationMatchResultMapper.java)
  - [ReconciliationRuleMapper.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/ReconciliationRuleMapper.java)
  - [ReconciliationRunMapper.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/ReconciliationRunMapper.java)
  - [ReconciliationSourceRecordMapper.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/ReconciliationSourceRecordMapper.java)
- `src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/`：
  - [ReconciliationCaseActionRepositoryImpl.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/ReconciliationCaseActionRepositoryImpl.java)
  - [ReconciliationCaseRepositoryImpl.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/ReconciliationCaseRepositoryImpl.java)
  - [ReconciliationMatchResultRepositoryImpl.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/ReconciliationMatchResultRepositoryImpl.java)
  - [ReconciliationRuleRepositoryImpl.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/ReconciliationRuleRepositoryImpl.java)
  - [ReconciliationRunRepositoryImpl.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/ReconciliationRunRepositoryImpl.java)
  - [ReconciliationSourceRecordRepositoryImpl.java](src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/ReconciliationSourceRecordRepositoryImpl.java)
- `src/main/resources/mapper/`：
  - [ReconciliationCaseActionMapper.xml](src/main/resources/mapper/ReconciliationCaseActionMapper.xml)
  - [ReconciliationCaseMapper.xml](src/main/resources/mapper/ReconciliationCaseMapper.xml)
  - [ReconciliationMatchResultMapper.xml](src/main/resources/mapper/ReconciliationMatchResultMapper.xml)
  - [ReconciliationRuleMapper.xml](src/main/resources/mapper/ReconciliationRuleMapper.xml)
  - [ReconciliationRunMapper.xml](src/main/resources/mapper/ReconciliationRunMapper.xml)
  - [ReconciliationSourceRecordMapper.xml](src/main/resources/mapper/ReconciliationSourceRecordMapper.xml)
- `src/main/resources/sql/`：
  - [reconciliation_case.sql](src/main/resources/sql/reconciliation_case.sql)
  - [reconciliation_case_action.sql](src/main/resources/sql/reconciliation_case_action.sql)
  - [reconciliation_match_result.sql](src/main/resources/sql/reconciliation_match_result.sql)
  - [reconciliation_rule.sql](src/main/resources/sql/reconciliation_rule.sql)
  - [reconciliation_run.sql](src/main/resources/sql/reconciliation_run.sql)
  - [reconciliation_source_record.sql](src/main/resources/sql/reconciliation_source_record.sql)
- `src/test/java/io/asteria/reconciliation/`：
  - [ReconciliationDomainTest.java](src/test/java/io/asteria/reconciliation/ReconciliationDomainTest.java)
  - [ReconciliationFixtures.java](src/test/java/io/asteria/reconciliation/ReconciliationFixtures.java)
  - [ReconciliationPersistenceTest.java](src/test/java/io/asteria/reconciliation/ReconciliationPersistenceTest.java)
