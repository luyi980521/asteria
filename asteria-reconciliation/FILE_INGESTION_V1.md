# Reconciliation v1 文件采集基础设施

本阶段只提供文件协议、存储、解析和数据访问；没有新增业务 Service、Consumer、Controller 或定时任务。
沿用各模块 `resources/sql` 发布方式，不引入迁移框架。金额在 CSV 中用 BigDecimal 与币种分列，
领域侧继续使用公共 Money；DTO 不创建 Money、不判断状态、不汇总借贷。时间使用 Instant，CSV 使用 ISO-8601 UTC。

## CSV 字段与真实来源

列顺序固定为下表顺序，v1 协议由三个 record 的组件声明定义。改变列顺序或字段需要升级 schemaVersion。

| DTO | CSV 字段（按顺序） | 来源 |
| --- | --- | --- |
| PaymentCaptureExportRecord | outboxId, eventId, paymentId, referenceType, referenceId, eventType, amount, currency, publicationStatus, capturedAt, createdAt, publishedAt, traceId | payment_outbox_event：id/event_id/event_type/status/created_at/published_at/trace_id；其 payload 内 PaymentCapturedEvent：paymentId/paymentReference/amount/capturedAt |
| LedgerJournalExportRecord | journalEntryId, sourceType, sourceId, eventType, eventId, status, postedAt, originalJournalEntryId, reversingJournalEntryId, reversalReason, reversedAt, sourceVersion, createdAt, updatedAt | ledger_journal_entry：id、JournalReference 四列、状态、入账和冲正字段、version、审计时间 |
| LedgerPostingExportRecord | postingId, journalEntryId, sequenceNo, ledgerAccountId, amount, currency, direction | ledger_posting：id/journal_entry_id/sequence_no/ledger_account_id/amount/currency/direction |

支付没有独立 Capture 表或 Capture ID。`PAYMENT_CAPTURED` 事件代表已完成的捕获事实；每次捕获用 eventId 区分，
不能用 paymentId 做捕获事实主键，也不能根据 Payment 当前状态重建历史。publicationStatus 只是 Outbox 发布状态。
LedgerSourceIds.payment 生成的 sourceId 必须原样保留；Ledger 的 eventId 关联捕获事件。
Ledger 头不生成“凭证总金额”，分录金额与借贷方向完整保留在独立文件中。

Payment 捕获事件不可变，后续采集方可约定其来源版本为固定 0；Ledger 使用行 version。
fileVersion 是文件交付版本，不能直接替代 SourceRecord.sourceVersion。
业务键及来源记录命名空间的构造由用户实现的导出/导入服务负责。

## 文件接口

```java
// application.port.ObjectStorageClient
void put(String objectKey, InputStream input) throws IOException;
InputStream get(String objectKey) throws IOException;
boolean exists(String objectKey) throws IOException;

// infrastructure.file
<T> long ReconciliationCsvWriter.write(OutputStream output, Class<T> type, Iterable<T> records) throws IOException;
<T> long ReconciliationCsvReader.read(InputStream input, Class<T> type, Consumer<T> consumer) throws IOException;
String FileChecksumUtils.sha256(InputStream input) throws IOException;
void FileChecksumUtils.verify(InputStream input, String expected) throws IOException;
byte[] ManifestSerializer.serialize(ReconciliationFileManifest manifest);
ReconciliationFileManifest ManifestSerializer.deserialize(byte[] bytes);
```

这些签名是文档展示，实例方法需经实例调用。CSV 工具只接受本次三个 DTO。
Reader 逐记录回调，Writer 接受 Iterable；均不关闭调用方流，Writer 成功时刷新缓冲。
工具不自动调用 Repository，也不隐藏回调异常或部分导入失败。
严格验证 UTF-8、表头、列数、引号、数值和时间格式；支持 BOM 读取、CRLF/LF 及引号内换行。
Writer 使用 UTF-8、CRLF、非 null 字段统一加引号并将双引号转义为两个双引号；
无引号空字段表示 null，`""` 表示空字符串。摘要计算覆盖文件原始字节，包括表头、BOM 和换行。

ReconciliationFileMetadata 字段：fileId/objectKey/sourceSystem/recordType/windowStart/windowEnd/fileVersion/
schemaVersion/recordCount/byteSize/sha256/createdAt。
ReconciliationFileManifest 字段：schemaVersion/deliveryId/createdAt/files。清单复用 JsonUtils 的 JSON 时间配置，
不修改公共 ObjectMapper；ManifestSerializer 只编解码，语义、版本、记录数和摘要检查由后续服务执行。

本地存储通过 `asteria.reconciliation.storage.local-directory` 配置，Bootstrap 支持环境变量
`ASTERIA_RECONCILIATION_STORAGE_DIRECTORY`，默认 `./data/reconciliation-objects`。
对象键只接受 `/` 分隔的合法相对路径；拒绝目录穿越、绝对路径、反斜线、Windows 设备名及非法字符，
检查符号链接和真实路径。`.upload-` 前缀为内部保留名，调用者不能访问临时上传。
同目录临时文件完成后 ATOMIC_MOVE 发布，不支持原子移动时直接失败，失败清理临时文件。
已有对象支持整体替换；导出方应把文件版本编码进 objectKey，并先发布全部 CSV，最后发布 Manifest。
对象根目录须由应用独占写入，路径检查不提供对恶意外部进程并发替换目录的隔离。

## 查询与写入接口

PaymentOutboxEventRepository 新增：

```java
List<PaymentOutboxEvent> findCapturedInWindow(Instant start, Instant end, long afterId, int limit);
```

按 payload.capturedAt 的 `[start,end)` 筛选 PAYMENT/PAYMENT_CAPTURED，不限制消息发布状态或 Outbox 创建时间。
兼容 JsonUtils 实际生成的小数 epoch 秒和 ISO 字符串；payload 保持 TEXT，只在 SQL 查询时解析为 json，未使用 JSONB。
无效 payload 会使查询失败，不静默丢弃证据。本阶段不新增 JSON 时间函数索引，较大历史数据量的扫描成本需后续实测。

JournalEntryRepository 新增：

```java
List<JournalEntrySnapshot> findInPostedWindow(Instant start, Instant end, long afterId, int limit);
Map<JournalEntryId, List<PostingSnapshot>> findPostingsByJournalEntryIds(List<JournalEntryId> journalEntryIds);
```

按 postedAt 的 `[start,end)` 筛选，DRAFT 没有入账业务时间，不落入该窗口。
公开快照包含完整 JournalEntry、有序分录、version、createdAt、updatedAt，不向其他模块暴露 Mapper 或 DO。
复用现有 Ledger Converter 的 Date→Instant 恢复方式；数据库窗口参数直接使用 Instant，避免先转 Date 截断边界。
批量分录查询每 1000 个凭证一组，PostingSnapshot 保留数据库真实 sequenceNo、JournalEntryId 和 Posting，
不按集合下标重建序号，也不要求数据库序号连续。

两种分页均按 ID 升序，首屏 afterId=0，下页用最后一行原始 ID；limit 为 1..10000。
ID 顺序不代表事务提交顺序，调用方应在来源完成水位或合适的快照事务内扫描，
同一次 Ledger 导出的凭证和分录也应保持一致读取边界。本阶段没有实现该编排。
Reconciliation 模块仍只依赖 Common，不新增 Payment/Ledger 依赖。

ReconciliationSourceRecordRepository 复用 findBySource(sourceType, sourceRecordId, sourceVersion)，新增：

```java
boolean saveIfAbsent(ReconciliationSourceRecord domain);
int saveAllIfAbsent(List<ReconciliationSourceRecord> records);
```

每 500 条使用一次多行 INSERT，明确 `ON CONFLICT (source_type,source_record_id,source_version) DO NOTHING`，
不覆盖已有证据；返回新增数量。其他主键/约束冲突继续抛持久化异常。
原 save 的严格插入语义保持不变，空批次返回 0。事务边界及冲突后读取/比较由调用方负责。

## 独立文件批次表

新增 `reconciliation_file_batch.sql`，原六张核心表没有修改。
应用主键 file_id；交付唯一键 `(source_system,object_key,file_version)`；
保存文件描述、时间窗口、格式版本、摘要、行数、字节数、导入状态、失败原因、创建及结束时间。
object_key 与 failure_reason 使用 TEXT，全部表字段及索引有 COMMENT。DDL 不自动执行。

ReconciliationFileBatchRepository：

```java
Optional<ReconciliationFileBatch> findById(Long fileId);
Optional<ReconciliationFileBatch> findByDelivery(String sourceSystem, String objectKey, Long fileVersion);
boolean saveIfAbsent(ReconciliationFileBatch batch);
boolean updateImportStatus(Long fileId, String expectedStatus, String status, Instant completedAt, String failureReason);
```

登记冲突不会覆盖既有元数据；调用方必须读取并比较摘要、记录数等描述，不能把所有冲突直接视作导入成功。
状态更新仅比较 expectedStatus，不实现状态机。状态策略、事务、文件验证与 SourceRecord 分批写入由用户实现。
文件登记、完成标记与来源快照的提交边界也由该服务明确；本阶段没有自动恢复、自动重试或对账触发。

## 留给用户的实现签名建议

以下仅是设计建议，未创建接口、空壳或实现；可以按实际编排调整。

```java
// PaymentExportService
ReconciliationFileManifest exportCaptures(Instant windowStart, Instant windowEnd, long fileVersion);
// LedgerExportService：同一清单描述 Journal 与 Posting 两类 CSV
ReconciliationFileManifest exportJournals(Instant windowStart, Instant windowEnd, long fileVersion);
// ReconciliationFileImportService
void importManifest(String manifestObjectKey);

// ReconciliationMatcher
List<ReconciliationMatchResult> match(ReconciliationRun run, List<ReconciliationSourceRecord> sources);
// PaymentLedgerReconciliationRule
ReconciliationMatchResult evaluate(ReconciliationRun run, String businessKey,
        List<ReconciliationSourceRecord> payments, List<ReconciliationSourceRecord> journals);
// DifferenceClassifier
ReconciliationDifferenceType classify(List<ReconciliationSourceRecord> left, List<ReconciliationSourceRecord> right);
// ReconciliationApplicationService
ReconciliationRunId execute(ReconciliationRuleId ruleId, Instant windowStart, Instant windowEnd, Instant sourceCutoffAt);
// ReconciliationCaseService
ReconciliationCaseId openCase(ReconciliationMatchResultId matchResultId);
```

## 验证

使用 JDK 21、IDE 自带 Maven；依赖缓存复制至工作区 `.cache/maven`，避免受限环境对外部缓存真实路径访问的影响。
测试临时目录也放在工作区 `.cache/test-temp`。未增加任何项目依赖。
PostgreSQL 测试沿用 ASTERIA_RECONCILIATION_TEST_JDBC_URL/DB_USER/DB_PASSWORD 环境变量。
只创建连接私有临时表，结束时回滚并关闭连接，不运行原 Ledger 脚本中的 DROP，不写入正式业务表。

可复现命令（本机环境中 Maven 可执行文件需使用 IDE 下的 mvn.cmd）：

```powershell
mvn -o '-Dmaven.repo.local=D:/IdeaProjects/asteria/.cache/maven' -DskipTests compile
mvn -o '-Dmaven.repo.local=D:/IdeaProjects/asteria/.cache/maven' `
  '-Djunit.jupiter.tempdir.basedir=D:/IdeaProjects/asteria/.cache/test-temp' `
  '-DargLine=-Djava.io.tmpdir=D:/IdeaProjects/asteria/.cache/test-temp' `
  -pl asteria-reconciliation,asteria-payment,asteria-ledger -am `
  '-Dtest=Reconciliation*Test,PaymentCaptureExportQueryTest,JournalExportQueryTest' `
  '-Dsurefire.failIfNoSpecifiedTests=false' test
```

文件测试覆盖三个 DTO、同一 Payment 的多个事件、逗号/引号/中文/换行、null 与空字符串、
空文件数据集、BOM、无末尾换行、非法 UTF-8/表头/列数/字段/引号、SHA-256 已知向量及损坏检测、
Manifest、对象存储往返、路径拒绝、首次及覆盖上传中断、旧对象保留、临时文件清理。
真实数据库测试覆盖捕获业务时间而非创建时间、数字和字符串时间、多次捕获及不同发布状态、
Journal 分页及精细时间窗口、来源版本和分录顺序、SourceRecord 批量幂等、文件元数据幂等、条件更新和全部 SQL 注释。

## 本次验证结果

最终 12 模块 Maven compile 通过。Reconciliation 的 8 项领域测试、6 项文件基础设施测试、9 项真实 PostgreSQL 测试，
Payment 和 Ledger 各 1 项真实 PostgreSQL 查询测试通过，共 25 项，零失败、零跳过。Common 原有 21 项测试也通过。
最后调整 PostingSnapshot 以保留非连续的真实序号后，重新运行 Ledger 查询测试及全项目编译，均通过。
未运行全仓库所有测试或 E2E。

## 创建的文件（相对项目根目录）

- `asteria-ledger/src/main/java/io/asteria/ledger/domain/valueobject/JournalEntrySnapshot.java`
- `asteria-ledger/src/main/java/io/asteria/ledger/domain/valueobject/PostingSnapshot.java`
- `asteria-ledger/src/test/java/io/asteria/ledger/JournalExportQueryTest.java`
- `asteria-payment/src/test/java/io/asteria/payment/PaymentCaptureExportQueryTest.java`
- `asteria-reconciliation/FILE_INGESTION_V1.md`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/application/model/LedgerJournalExportRecord.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/application/model/LedgerPostingExportRecord.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/application/model/PaymentCaptureExportRecord.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/application/model/ReconciliationFileManifest.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/application/model/ReconciliationFileMetadata.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/application/port/ObjectStorageClient.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/domain/repository/ReconciliationFileBatchRepository.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/domain/valueobject/ReconciliationFileBatch.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/file/FileChecksumUtils.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/file/LocalObjectStorageClient.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/file/ManifestSerializer.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/file/ReconciliationCsvReader.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/file/ReconciliationCsvSchema.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/file/ReconciliationCsvWriter.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/persistence/converter/ReconciliationFileBatchPersistenceConverter.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/persistence/dataobject/ReconciliationFileBatchDO.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/ReconciliationFileBatchMapper.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/ReconciliationFileBatchRepositoryImpl.java`
- `asteria-reconciliation/src/main/resources/mapper/ReconciliationFileBatchMapper.xml`
- `asteria-reconciliation/src/main/resources/sql/reconciliation_file_batch.sql`
- `asteria-reconciliation/src/test/java/io/asteria/reconciliation/ReconciliationFileInfrastructureTest.java`

## 修改的文件（相对项目根目录）

- `asteria-bootstrap/src/main/resources/application.yml`
- `asteria-ledger/src/main/java/io/asteria/ledger/domain/repository/JournalEntryRepository.java`
- `asteria-ledger/src/main/java/io/asteria/ledger/infrastructure/persistence/repository/JournalEntryRepositoryImpl.java`
- `asteria-payment/src/main/java/io/asteria/payment/domain/repository/PaymentOutboxEventRepository.java`
- `asteria-payment/src/main/java/io/asteria/payment/infrastructure/persistence/mapper/PaymentOutboxEventMapper.java`
- `asteria-payment/src/main/java/io/asteria/payment/infrastructure/persistence/repository/PaymentOutboxEventRepositoryImpl.java`
- `asteria-payment/src/main/resources/mapper/PaymentOutboxEventMapper.xml`
- `asteria-reconciliation/README.md`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/domain/repository/ReconciliationSourceRecordRepository.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/persistence/mapper/ReconciliationSourceRecordMapper.java`
- `asteria-reconciliation/src/main/java/io/asteria/reconciliation/infrastructure/persistence/repository/ReconciliationSourceRecordRepositoryImpl.java`
- `asteria-reconciliation/src/main/resources/mapper/ReconciliationSourceRecordMapper.xml`
- `asteria-reconciliation/src/test/java/io/asteria/reconciliation/ReconciliationPersistenceTest.java`
