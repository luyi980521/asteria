package io.asteria.reconciliation;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.asteria.common.util.JsonUtils;
import io.asteria.reconciliation.domain.enums.*;
import io.asteria.reconciliation.domain.valueobject.*;
import io.asteria.reconciliation.infrastructure.persistence.converter.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static io.asteria.reconciliation.ReconciliationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 独立于数据库连接的持久化契约回归，验证完整 ResultMap 和双向 Converter。
 */
class ReconciliationPersistenceContractTest {
    /** 模块内全部持久化类型，不按 SQL 文件名推断新增交付表 */
    static final List<String> TYPES = List.of("Rule", "SourceRecord", "Run", "MatchResult", "Case", "CaseAction", "FileBatch", "DeliveryBatch");

    /**
     * 验证显式 ResultMap 覆盖每个持久化属性，关闭自动映射以避免配置掩盖遗漏。
     *
     * @throws Exception Mapper 解析或类型加载失败
     */
    @Test
    void everyMapperParsesAndExplicitlyMapsEveryPersistentProperty() throws Exception {
        var configuration = new MybatisConfiguration();
        configuration.setAutoMappingBehavior(AutoMappingBehavior.NONE);
        configuration.setMapUnderscoreToCamelCase(true);
        for (String type : TYPES) {
            String namespace = "io.asteria.reconciliation.infrastructure.persistence.mapper.Reconciliation" + type + "Mapper";
            String resource = "mapper/Reconciliation" + type + "Mapper.xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
            var doType = Class.forName("io.asteria.reconciliation.infrastructure.persistence.dataobject.Reconciliation" + type + "DO");
            var table = TableInfoHelper.getTableInfo(doType);
            assertNotNull(table, type);
            Map<String, String> expected = new TreeMap<>();
            expected.put(table.getKeyProperty(), table.getKeyColumn());
            table.getFieldList().forEach(field -> expected.put(field.getProperty(), field.getColumn()));
            var mappings = configuration.getResultMap(namespace + ".BaseResultMap").getResultMappings();
            var actual = mappings.stream().collect(Collectors.toMap(mapping -> mapping.getProperty(), mapping -> mapping.getColumn()));
            assertEquals(expected, actual, type + " ResultMap must match MyBatis-Plus column mapping");
            assertEquals(expected.size(), mappings.size(), type + " must not map a property twice");
            List<String> propertyOrder = new ArrayList<>();
            propertyOrder.add(table.getKeyProperty());
            table.getFieldList().forEach(field -> propertyOrder.add(field.getProperty()));
            assertEquals(propertyOrder, mappings.stream().map(mapping -> mapping.getProperty()).toList(), type + " ResultMap declaration order");
            assertTrue(configuration.hasStatement(namespace + ".findById"));
            assertEquals(com.baomidou.mybatisplus.annotation.IdType.INPUT, table.getIdType(), type);
        }
    }

    /**
     * 对所有领域类型比较完整 JSON 结构，覆盖 ID、Money、枚举、TEXT、可空字段和各类版本。
     *
     * @throws Exception Converter 调用失败
     */
    @Test
    void allConvertersRoundTripEveryDomainFieldIncludingNullableValues() throws Exception {
        assertRoundTrip(rule(), new ReconciliationRulePersistenceConverter());
        var disabled = rule();
        disabled.configure(false, 123L, null, NOW.plusSeconds(1));
        assertRoundTrip(disabled, new ReconciliationRulePersistenceConverter());
        assertRoundTrip(source(2L, 987L), new ReconciliationSourceRecordPersistenceConverter());
        var sourceConverter = new ReconciliationSourceRecordPersistenceConverter();
        var unknownAmount = sourceConverter.toDataObject(source(3L, 988L));
        unknownAmount.setAmount(null);
        unknownAmount.setCurrency(null);
        unknownAmount.setPostingStatus(null);
        assertRoundTrip(sourceConverter.toDomain(unknownAmount), sourceConverter);
        assertRoundTrip(run(), new ReconciliationRunPersistenceConverter());
        var failed = run();
        failed.start(NOW.plusSeconds(1));
        failed.fail("持久化验证\n包含引号\"与逗号,", NOW.plusSeconds(2));
        assertRoundTrip(failed, new ReconciliationRunPersistenceConverter());
        var runConverter = new ReconciliationRunPersistenceConverter();
        var versionedRun = runConverter.toDataObject(run());
        versionedRun.setRuleVersion(31L);
        versionedRun.setVersion(7L);
        var restoredRun = runConverter.toDomain(versionedRun);
        assertEquals(31L, restoredRun.getRuleVersion());
        assertEquals(7L, restoredRun.getVersion());
        assertRoundTrip(restoredRun, runConverter);
        assertRoundTrip(result(4L, ReconciliationMatchStatus.MISMATCHED, ReconciliationDifferenceType.CURRENCY_MISMATCH),
                new ReconciliationMatchResultPersistenceConverter());
        assertRoundTrip(result(5L, ReconciliationMatchStatus.PENDING, null), new ReconciliationMatchResultPersistenceConverter());
        var resultConverter = new ReconciliationMatchResultPersistenceConverter();
        var missingAmounts = resultConverter.toDataObject(result(7L, ReconciliationMatchStatus.PENDING, null));
        missingAmounts.setExpectedAmount(null);
        missingAmounts.setExpectedCurrency(null);
        missingAmounts.setActualAmount(null);
        missingAmounts.setActualCurrency(null);
        missingAmounts.setDetails(null);
        assertRoundTrip(resultConverter.toDomain(missingAmounts), resultConverter);
        var caseRecord = reconciliationCase(6L);
        caseRecord.investigate("operator-1", NOW.plusSeconds(1));
        caseRecord.resolve("核查说明\n\"中文\",", NOW.plusSeconds(2));
        assertRoundTrip(caseRecord, new ReconciliationCasePersistenceConverter());
        assertRoundTrip(action(), new ReconciliationCaseActionPersistenceConverter());
        var actionConverter = new ReconciliationCaseActionPersistenceConverter();
        var comment = actionConverter.toDataObject(action());
        comment.setActionType("COMMENT");
        comment.setFromStatus("OPEN");
        comment.setOperatorType("HUMAN");
        assertRoundTrip(actionConverter.toDomain(comment), actionConverter);
        assertRoundTrip(file(ReconciliationFileBatchStatus.QUARANTINED, "原始失败说明", 17L), new ReconciliationFileBatchPersistenceConverter());
        assertRoundTrip(file(ReconciliationFileBatchStatus.CREATED, null, null), new ReconciliationFileBatchPersistenceConverter());
        assertRoundTrip(delivery(ReconciliationFileBatchStatus.IMPORTED, null, 17L), new ReconciliationDeliveryBatchPersistenceConverter());
        assertRoundTrip(delivery(ReconciliationFileBatchStatus.CREATED, null, null), new ReconciliationDeliveryBatchPersistenceConverter());
    }

    /**
     * 验证可空关联字段与业务版本保持独立，未知字段不会在恢复时被伪造。
     */
    @Test
    void fileConverterPreservesOptionalFieldsAndDistinctVersionMeanings() {
        var converter = new ReconciliationFileBatchPersistenceConverter();
        var original = file(ReconciliationFileBatchStatus.COMPLETED, null, null);
        var row = converter.toDataObject(original);
        row.setBatchId(null);
        row.setBusinessDate(null);
        row.setCutoffAt(null);
        var restored = converter.toDomain(row);
        assertNull(restored.batchId());
        assertNull(restored.businessDate());
        assertNull(restored.cutoffAt());
        assertNull(restored.actualRecordCount());
        assertEquals(45L, restored.fileVersion());
        assertEquals(1, restored.schemaVersion());
        assertEquals(ReconciliationFileBatchStatus.COMPLETED, restored.status());
        assertEquals(JsonUtils.mapper().valueToTree(row), JsonUtils.mapper().valueToTree(converter.toDataObject(restored)));
    }

    /**
     * 提供完整文件元数据样本，时间精度按 PostgreSQL 微秒精度设置。
     *
     * @param status 文件状态
     * @param reason 失败说明
     * @param actual 实际数量
     * @return 文件快照
     */
    static ReconciliationFileBatch file(ReconciliationFileBatchStatus status, String reason, Long actual) {
        var time = NOW.plusNanos(123456000);
        return ReconciliationFileBatch.builder()
                .fileId(101L)
                .objectKey("ledger/中文-journal.csv")
                .sourceSystem("LEDGER")
                .recordType("LEDGER_JOURNAL")
                .windowStart(START)
                .windowEnd(END)
                .fileVersion(45L)
                .schemaVersion(1)
                .sha256("a".repeat(64))
                .recordCount(17L)
                .byteSize(321L)
                .status(status)
                .failureReason(reason)
                .createdAt(time)
                .completedAt(null)
                .batchId(201L)
                .businessDate(LocalDate.of(2026, 10, 1))
                .cutoffAt(NOW)
                .actualRecordCount(actual)
                .updatedAt(time)
                .build();
    }

    /**
     * 提供完整的多文件交付头样本。
     *
     * @param status 交付状态
     * @param reason 失败说明
     * @param actual 实际数量
     * @return 交付快照
     */
    static ReconciliationDeliveryBatch delivery(ReconciliationFileBatchStatus status, String reason, Long actual) {
        var time = NOW.plusNanos(123456000);
        return ReconciliationDeliveryBatch.builder()
                .batchId(201L)
                .manifestObjectKey("ledger/manifest.json")
                .sourceSystem("LEDGER")
                .businessDate(LocalDate.of(2026, 10, 1))
                .windowStart(START)
                .windowEnd(END)
                .cutoffAt(NOW)
                .fileVersion(45L)
                .schemaVersion(1)
                .expectedRecordCount(17L)
                .actualRecordCount(actual)
                .status(status)
                .failureReason(reason)
                .createdAt(time)
                .updatedAt(time)
                .completedAt(null)
                .build();
    }

    /**
     * 比较领域对象完整结构，而不是只抽查主键或状态。
     *
     * @param original 原始领域对象
     * @param converter 对应转换器
     * @throws Exception 转换失败
     */
    private void assertRoundTrip(Object original, Object converter) throws Exception {
        Object row = converter.getClass().getMethod("toDataObject", original.getClass()).invoke(converter, original);
        Object restored = converter.getClass().getMethod("toDomain", row.getClass()).invoke(converter, row);
        assertEquals(JsonUtils.mapper().valueToTree(original), JsonUtils.mapper().valueToTree(restored), original.getClass().getSimpleName());
    }
}
