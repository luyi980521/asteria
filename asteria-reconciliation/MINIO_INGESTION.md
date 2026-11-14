# Reconciliation 文件接入：MinIO 与多文件交付

本次扩展此前文件采集基础设施。核心 Service、Normalization、对账及恢复均未实现。
旧版说明 `FILE_INGESTION_V1.md` 保留作为上一阶段记录；本文件描述当前接口增量。

## 本地启动

现有 Compose Kafka 服务保留，MinIO 使用独立 `reconciliation` profile 和命名卷，端口仅绑定本机。
在 PowerShell 中设置开发凭据后启动：

```powershell
$env:ASTERIA_RECONCILIATION_MINIO_ACCESS_KEY = '<本地访问标识>'
$env:ASTERIA_RECONCILIATION_MINIO_SECRET_KEY = '<本地密码，至少八个字符>'
docker compose --profile reconciliation up -d minio
```

S3 API 为 `http://localhost:9000`，控制台为 `http://localhost:9001`。
应用启动时启用 `reconciliation-minio` Spring profile；该开发 profile 会创建缺失的
`asteria-reconciliation` bucket。只启动容器不启动应用时，不会创建 bucket。
桶的并发初始化只忽略 BucketAlreadyOwnedByYou，其他错误使启动失败。

```powershell
$env:SPRING_PROFILES_ACTIVE = 'reconciliation-minio'
# 然后按项目原有方式启动 asteria-bootstrap
```

可配置项均在 `asteria.reconciliation.storage` 下，与已有 S3 命名空间隔离：

| 环境变量 | 用途 |
| --- | --- |
| ASTERIA_RECONCILIATION_STORAGE_PROVIDER | local（默认）或 minio |
| ASTERIA_RECONCILIATION_STORAGE_DIRECTORY | local 对象根目录 |
| ASTERIA_RECONCILIATION_MINIO_ENDPOINT | API endpoint，默认配置为空；开发 profile 默认为 localhost:9000 |
| ASTERIA_RECONCILIATION_MINIO_ACCESS_KEY | 外部提供访问标识 |
| ASTERIA_RECONCILIATION_MINIO_SECRET_KEY | 外部提供密钥，不写入 Java 或日志 |
| ASTERIA_RECONCILIATION_MINIO_BUCKET | 专用桶名，默认 asteria-reconciliation |
| ASTERIA_RECONCILIATION_MINIO_INITIALIZE_BUCKET | 默认 false；开发 profile 显式启用初始化 |
| ASTERIA_MINIO_IMAGE | 替换 Compose 本地开发镜像 |

Compose 固定一个历史 MinIO 镜像供本地复现，未将其作为生产部署方案。
[官方仓库说明](https://github.com/minio/minio#source-only-distribution)当前社区发行方式已改变，生产配置应使用组织维护的服务或镜像，
提供 HTTPS endpoint 和专用最小权限凭据，预先创建桶，不启用本地开发 profile。
MinIO SDK 的对象操作依据[官方 Java SDK API](https://github.com/minio/minio-java/blob/8.5.17/docs/API.md)。

## 对象操作与文件结构

ObjectStorageClient 复用原有 put/get/exists，新增：

```java
ObjectStorageMetadata metadata(String objectKey) throws IOException;
ObjectStoragePage list(String prefix, String afterKey, int limit) throws IOException;
```

metadata 返回对象键、字节数、更新时间、可选内容类型和 ETag。
ETag 不是文件 SHA-256。列表使用键升序、排他 afterKey，首屏 null，页大小 1..1000；
结果 nextAfterKey 为空表示末页。分页期间发生对象变更时不保证读取快照。
Local 与 MinIO 共用路径语法检查；Local 保留临时文件和原子发布，列表不暴露临时上传。
MinIO 使用 SDK 流式分片上传，不整文件读入内存；只将 NoSuchKey/NoSuchObject 视为对象不存在，
权限、网络、桶缺失和其他存储错误继续抛 IOException。

PaymentCaptureExportRecord、LedgerJournalExportRecord、LedgerPostingExportRecord 原有业务事实保持完整。
新增 SettlementCompletedExportRecord，来源为 settlement_outbox_event 及 SettlementCompletedEvent payload，
保留 eventId、settlementBatchId、settlementReference、channelSettlementBatchId、grossAmount/feeAmount/netAmount
各自的金额和币种、settledAt、Outbox 发布状态及审计时间；不计算费用或净额。

Manifest 保留 schemaVersion/deliveryId/createdAt/files，新增 sourceSystem/businessDate/windowStart/windowEnd/
cutoffAt/fileVersion/recordCount。单个文件元数据新增 businessDate/cutoffAt，继续保留其原始字节 sha256、
recordCount、byteSize 和所有文件身份字段。多文件交付总行数是所有 CSV 数据行数之和，不按业务键去重。
Manifest 的 checksum 由各 files[].sha256 表达，避免用 Manifest 自身包含自身摘要的循环定义。

ManifestSerializer 仍复用 JsonUtils；编解码现在先通过 FileSchemaValidator 检查结构。
缺少新增必填描述的旧清单需要导出方补齐后重新交付，不能伪造业务日期或截止时间。
schemaVersion=1 当前支持 PAYMENT_CAPTURE、LEDGER_JOURNAL、LEDGER_POSTING、SETTLEMENT_COMPLETED。
FileSchemaValidator 只检查格式版本、元数据字段、对象键、清单内重复文件与计数一致性；
文件真实字节摘要仍由 FileChecksumUtils 检查，实际解析数量由导入服务比对。
CSV reader/writer 新增显式 schemaVersion 重载，不支持的版本在读写/回调前失败。
原有 UTF-8、CRLF、引号转义、空字段为 null、引号空串为空字符串的协议不变。

## 数据库发布

沿用 `resources/sql`，没有 Flyway/Liquibase。已部署旧文件表时只执行新增
`reconciliation_file_ingestion_v2.sql`；全新环境先执行旧 `reconciliation_file_batch.sql`，再执行该增量。
建议由已有发布流程用事务包裹。脚本不会由应用自动执行，本次验证不向正式表应用迁移。

- 新表 reconciliation_delivery_batch：多文件交付头，以 batch_id 为应用主键，
  UNIQUE(source_system,manifest_object_key,file_version) 保证交付登记幂等。
- 扩展 reconciliation_file_batch：仍保留独立文件描述及 UNIQUE(source_system,object_key,file_version)，
  新增逻辑 batch_id、business_date、cutoff_at、actual_record_count、updated_at。
  历史文件的业务日期/截止时间/关联批次保持空值，不从时间字段猜测；历史更新时间初值为迁移时间。
- 保留原有状态时间索引，并新增交付关联、来源日期及状态 ID 分页索引。
- 所有新增表、字段及索引有 COMMENT；没有物理外键，没有 JSONB；长文本为 TEXT。
- 九种交付状态仅限定值域；旧文件 COMPLETED 为兼容保留，不把历史记录改写为新状态。
  ReconciliationFileBatchStatus 不实现转换策略。

ReconciliationFileBatchRepository 与 ReconciliationDeliveryBatchRepository 都提供：

```java
Optional<T> findById(Long id);
Optional<T> findByDelivery(String sourceSystem, String objectKey, Long fileVersion);
List<T> findBySourceAndBusinessDate(String sourceSystem, LocalDate businessDate, long afterId, int limit);
List<T> findByStatus(ReconciliationFileBatchStatus status, long afterId, int limit);
boolean saveIfAbsent(T batch);
boolean updateMetadata(T batch, ReconciliationFileBatchStatus expectedStatus, Instant expectedUpdatedAt);
```

T 分别为对应的元数据模型。FileBatchRepository 另提供 findByBatchId(batchId,afterId,limit)，
并保留旧 updateImportStatus 接口。新代码优先使用 updateMetadata，按状态和更新时间条件保护并发写入，
只更新实际数量、状态、失败说明及处理时间，不覆盖来源身份、文件版本、校验值或预期数量。
相同交付键登记冲突返回 false，读取并比对既有描述、状态转换、事务和错误重试由用户 Service 实现。
多文件头和文件描述由调用方事务一起保存；Repository 不偷偷创建其他表中的记录。

## JavaDoc 审查方法

根 AGENTS.md 固化永久规范。工具 `tools/javadoc/JavadocFormatter.java` 用 JDK 21 AST 识别声明，
词法扫描跳过字符串、字符及文本块，不采用全仓库正则替换。
只重写现有 JavaDoc，不补造业务说明；保留普通注释、标签、链接、HTML 和代码块。
复杂字段注释可保留多行。所有编辑均验证注释以外的原始文本完全相同，并验证正文的非空白字符不变。

```powershell
javac -encoding UTF-8 -d .cache/javadoc-tools tools/javadoc/JavadocFormatter.java
java -cp .cache/javadoc-tools JavadocFormatter .          # 仅预检
java -cp .cache/javadoc-tools JavadocFormatter . --write  # 保存副本后写入
```

写入记录为 `.cache/javadoc-format-report.json`，包含每个文件和非注释内容 SHA-256；
修改前副本在 `.cache/javadoc-before/`。重复写入已有备份时会拒绝覆盖，防止丢失审查基线。
完成后的预检应显示零待格式化项。注释内复杂 Java 示例已用独立样例验证不会破坏代码、链接或字符串。

## 下一步由开发者实现

建议顺序：PaymentExportService → LedgerExportService → SettlementExportService →
ReconciliationFileImportService（交付与文件状态编排、摘要和数量比对、事务）→
PaymentSourceNormalizer/LedgerSourceNormalizer → PaymentLedgerReconciliationRule、ReconciliationMatcher、DifferenceClassifier →
ReconciliationApplicationService → ReconciliationCaseService。
自动恢复、修复和冲正不属于当前基础设施，未创建空壳或 TODO Service。

真实 MinIO 验证必须显式设置 ASTERIA_MINIO_INTEGRATION_TEST=true，以及上述 endpoint/accessKey/secretKey/bucket 环境变量。
集成测试不创建或删除桶，只清理它自己创建的随机测试前缀对象。
未配置或未启动真实 MinIO 时，该测试跳过，SDK mock 测试通过不能替代真实连通性结果。

## 本次验证结果

- 全部 12 个 Maven 模块 compile 成功。
- Common、Reconciliation、Web、Balance、Settlement、Ledger、Payment 共 215 项测试，207 项成功，8 项跳过，零失败或错误。
- Reconciliation 的 31 项中 30 项成功；真实 MinIO 测试跳过。本机 127.0.0.1:9000 不可达，且未找到可用 Docker CLI，未完成实际 MinIO 连接或容器启动验证。
- 其余 7 项跳过是未配置其独立环境变量的 Balance 数据库集成测试。
- Reconciliation 的 10 项真实 PostgreSQL 临时表测试及 Payment/Ledger 查询测试成功；只操作临时表并回滚，未部署正式迁移。
- Compose、主应用及 MinIO 开发 profile 的 YAML 语法均通过 SnakeYAML 解析；此检查不代替 docker compose 或真实服务验证。
- 扫描 392 个 Java 文件，140 个文件的 306 处 JavaDoc 完成格式化；再次预检为零待修改项。
- 140 份格式化前副本与当前文件再次逐一比较，全部非注释字符保持一致；git diff --check 通过。
- 已交付可审查的逐文件摘要清单：[JavaDoc 格式化审计报告](../docs/verification/javadoc-format-report.json)。
- 未运行 Bootstrap 上下文测试或完整 E2E。

最终编译与测试命令（IDE Maven 不在 PATH 时使用其完整路径）：

```powershell
mvn -o '-Dmaven.repo.local=D:/IdeaProjects/asteria/.cache/maven' -DskipTests compile
# 数据库测试需先按已有规范设置 ASTERIA_RECONCILIATION_TEST_JDBC_URL/DB_USER/DB_PASSWORD
mvn -o '-Dmaven.repo.local=D:/IdeaProjects/asteria/.cache/maven' `
  '-Djunit.jupiter.tempdir.basedir=D:/IdeaProjects/asteria/.cache/test-temp' `
  '-DargLine=-Djava.io.tmpdir=D:/IdeaProjects/asteria/.cache/test-temp' `
  -pl asteria-reconciliation,asteria-payment,asteria-ledger,asteria-settlement,asteria-balance -am test
```

## 新增及扩展的基础设施

本阶段新增类均在 Reconciliation 内，除格式化工具以外没有新增业务模块。

- application/model：ObjectStorageMetadata、ObjectStoragePage、SettlementCompletedExportRecord；扩展已有 Manifest 与 FileMetadata。
- infrastructure/config：ReconciliationMinioProperties、ReconciliationStorageConfiguration。
- infrastructure/file：MinioObjectStorageClient、ObjectStorageKeys、FileSchemaValidator；扩展 LocalObjectStorageClient、ManifestSerializer、CSV reader/writer，复用 FileChecksumUtils。
- domain/enums：ReconciliationFileBatchStatus。
- domain/valueobject：新增 ReconciliationDeliveryBatch，扩展既有 ReconciliationFileBatch。
- domain/repository：新增 ReconciliationDeliveryBatchRepository，扩展既有 ReconciliationFileBatchRepository。
- infrastructure/persistence：新增 DeliveryBatch 的 DO、Converter、Mapper、Repository 实现和 Mapper XML；对应扩展 FileBatch 的已有实现。
- resources/sql：新增 reconciliation_file_ingestion_v2.sql；旧文件表创建脚本与六张核心表结构均保留。
- 配置：扩展根 docker-compose.yml、Bootstrap application.yml；新增 application-reconciliation-minio.yml。
- 测试：新增 FileSchemaValidatorTest、MinioObjectStorageClientTest、MinioObjectStorageIntegrationTest；扩展文件基础设施和数据库持久化测试。
- 永久规范与工具：根 AGENTS.md、tools/javadoc/JavadocFormatter.java、docs/verification/javadoc-format-report.json。
