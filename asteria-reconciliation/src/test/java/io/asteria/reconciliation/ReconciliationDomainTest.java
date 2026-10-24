package io.asteria.reconciliation;

import io.asteria.common.util.JsonUtils;
import io.asteria.reconciliation.domain.entity.*;
import io.asteria.reconciliation.domain.enums.*;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;
import io.asteria.reconciliation.domain.valueobject.*;
import io.asteria.reconciliation.infrastructure.persistence.converter.*;
import org.junit.jupiter.api.Test;

import static io.asteria.reconciliation.ReconciliationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 约束和状态机回归，重点覆盖终态、窗口边界、序列化与金额无损恢复。
 */
class ReconciliationDomainTest {
    @Test
    void runFreezesRuleAndUsesHalfOpenWindow() {
        var rule = rule();
        var run = ReconciliationRun.create(ReconciliationRunId.of(3L), rule, START, END, NOW, NOW);
        rule.configure(false, 120L, "{}", NOW.plusSeconds(1));
        assertTrue(JsonUtils.readTree(run.getRuleSnapshot()).path("enabled").asBoolean());
        assertEquals(60L, JsonUtils.readTree(run.getRuleSnapshot()).path("gracePeriodSeconds").asLong());
        assertTrue(run.contains(START));
        assertFalse(run.contains(START.minusNanos(1)));
        assertTrue(run.contains(END.minusNanos(1)));
        assertFalse(run.contains(END));
        assertThrows(ReconciliationDomainException.class,
                () -> ReconciliationRun.create(ReconciliationRunId.of(8L), rule, START, END, NOW, NOW));
        assertThrows(ReconciliationDomainException.class,
                () -> ReconciliationRun.create(ReconciliationRunId.of(8L), rule(), END, END, NOW, NOW));
        assertThrows(ReconciliationDomainException.class,
                () -> ReconciliationRun.create(ReconciliationRunId.of(8L), rule(), START, END, START, NOW));
    }

    @Test
    void runOnlyCompletesOnceAndValidatesCountsBeforeMutation() {
        var run = run();
        assertThrows(ReconciliationDomainException.class, () -> run.complete(0L, 0L, 0L, 0L, 0L, 0L, NOW));
        run.start(NOW);
        assertThrows(ReconciliationDomainException.class, () -> run.start(NOW));
        assertThrows(ReconciliationDomainException.class, () -> run.complete(3L, 1L, 0L, 1L, 0L, 0L, NOW));
        assertThrows(ReconciliationDomainException.class,
                () -> run.complete(Long.MAX_VALUE, Long.MAX_VALUE, 1L, 0L, 0L, 0L, NOW));
        assertEquals(ReconciliationRunStatus.RUNNING, run.getStatus());
        run.complete(3L, 1L, 0L, 1L, 1L, 0L, NOW.plusSeconds(1));
        assertEquals(ReconciliationRunStatus.COMPLETED, run.getStatus());
        assertEquals(1L, run.getPendingCount());
        assertThrows(ReconciliationDomainException.class, () -> run.fail("失败", NOW.plusSeconds(2)));
        assertThrows(ReconciliationDomainException.class, () -> run.start(NOW.plusSeconds(2)));
    }

    @Test
    void failedRunRequiresReasonAndMonotonicTime() {
        var run = run();
        run.start(NOW.plusSeconds(2));
        assertThrows(ReconciliationDomainException.class, () -> run.fail(" ", NOW.plusSeconds(3)));
        assertThrows(ReconciliationDomainException.class, () -> run.fail("timeout", NOW));
        assertEquals(ReconciliationRunStatus.RUNNING, run.getStatus());
        run.fail("timeout", NOW.plusSeconds(3));
        assertEquals("timeout", run.getFailureReason());
        assertThrows(ReconciliationDomainException.class, () -> run.start(NOW.plusSeconds(4)));
    }

    @Test
    void caseRequiresConfirmedDifferenceAndCannotReopenTerminalState() {
        assertThrows(ReconciliationDomainException.class, () -> ReconciliationCase.open(ReconciliationCaseId.of(5L),
                result(4L, ReconciliationMatchStatus.PENDING, null), NOW));
        var c = reconciliationCase(5L);
        assertThrows(ReconciliationDomainException.class, () -> c.investigate(" ", NOW));
        c.investigate("operator-1", NOW.plusSeconds(1));
        assertThrows(ReconciliationDomainException.class, () -> c.resolve("", NOW.plusSeconds(2)));
        c.resolve("核实后已入账", NOW.plusSeconds(2));
        assertEquals(ReconciliationCaseStatus.RESOLVED, c.getStatus());
        assertThrows(ReconciliationDomainException.class, () -> c.ignore("重复工单", NOW.plusSeconds(3)));
        var ignored = reconciliationCase(7L);
        ignored.ignore("测试数据", NOW);
        assertEquals(ReconciliationCaseStatus.IGNORED, ignored.getStatus());
    }

    @Test
    void matchStatusAndDifferenceMustAgree() {
        assertThrows(ReconciliationDomainException.class,
                () -> result(4L, ReconciliationMatchStatus.MATCHED, ReconciliationDifferenceType.MISSING_LEDGER));
        assertThrows(ReconciliationDomainException.class,
                () -> result(4L, ReconciliationMatchStatus.MISSING, null));
        assertThrows(ReconciliationDomainException.class,
                () -> result(4L, ReconciliationMatchStatus.DUPLICATED, ReconciliationDifferenceType.STATUS_MISMATCH));
        assertDoesNotThrow(() -> result(4L, ReconciliationMatchStatus.DUPLICATED, ReconciliationDifferenceType.DUPLICATE_RECORD));
    }

    @Test
    void builderRejectsInvalidSourceIdsAndJsonWithoutBypassingDomainChecks() {
        for (String ids : new String[]{"[]", "[1,1]", "[0]", "[-1]", "[1.5]", "[9223372036854775808]", "[\"1\"]", "{}", "invalid"}) {
            var valid = result(4L, ReconciliationMatchStatus.MISSING, ReconciliationDifferenceType.MISSING_LEDGER);
            assertThrows(ReconciliationDomainException.class, () -> ReconciliationMatchResult.builder()
                    .matchResultId(valid.getMatchResultId()).runId(valid.getRunId()).ruleCode(valid.getRuleCode())
                    .businessKey(valid.getBusinessKey()).status(valid.getStatus()).differenceType(valid.getDifferenceType())
                    .sourceRecordIds(ids).createdAt(NOW).build(), ids);
        }
        assertThrows(ReconciliationDomainException.class,
                () -> ReconciliationRule.create(ReconciliationRuleId.of(1L), "rule", "name",
                        ReconciliationType.PAYMENT_LEDGER, 0L, "[]", NOW));
        assertThrows(ReconciliationDomainException.class, () -> ReconciliationRunId.of(0L));
    }

    @Test
    void actionMustDescribeLegalTransitionAndIdentifyOperator() {
        var open = action();
        assertEquals(ReconciliationCaseActionType.OPEN, open.getActionType());
        assertThrows(ReconciliationDomainException.class, () -> ReconciliationCaseAction.builder()
                .caseActionId(ReconciliationCaseActionId.of(6L)).caseId(ReconciliationCaseId.of(5L))
                .actionType(ReconciliationCaseActionType.RESOLVE).operatorType(ReconciliationOperatorType.HUMAN)
                .operatorId("operator-1").fromStatus(ReconciliationCaseStatus.RESOLVED)
                .toStatus(ReconciliationCaseStatus.RESOLVED).reason("重复关闭").createdAt(NOW).build());
    }

    @Test
    void allSixConvertersPreserveSnapshotsStatesMoneyAndTime() {
        var ruleConverter = new ReconciliationRulePersistenceConverter();
        var sourceConverter = new ReconciliationSourceRecordPersistenceConverter();
        var runConverter = new ReconciliationRunPersistenceConverter();
        var matchConverter = new ReconciliationMatchResultPersistenceConverter();
        var caseConverter = new ReconciliationCasePersistenceConverter();
        var actionConverter = new ReconciliationCaseActionPersistenceConverter();
        var source = source(2L, 0L);
        var run = run();
        run.start(NOW);
        run.complete(1L, 0L, 1L, 0L, 0L, 0L, NOW.plusSeconds(1));
        var result = result(4L, ReconciliationMatchStatus.MISMATCHED, ReconciliationDifferenceType.CURRENCY_MISMATCH);
        var c = reconciliationCase(5L);
        c.investigate("operator-1", NOW);
        c.resolve("解决", NOW.plusSeconds(1));
        assertJsonEquals(rule(), ruleConverter.toDomain(ruleConverter.toDataObject(rule())));
        assertJsonEquals(source, sourceConverter.toDomain(sourceConverter.toDataObject(source)));
        assertJsonEquals(run, runConverter.toDomain(runConverter.toDataObject(run)));
        assertJsonEquals(result, matchConverter.toDomain(matchConverter.toDataObject(result)));
        assertJsonEquals(c, caseConverter.toDomain(caseConverter.toDataObject(c)));
        assertJsonEquals(action(), actionConverter.toDomain(actionConverter.toDataObject(action())));
        assertEquals("USD", sourceConverter.toDataObject(source).getCurrency());
        assertEquals("JPY", matchConverter.toDataObject(result).getActualCurrency());
        assertEquals(DETAILS, matchConverter.toDataObject(result).getDetails());
    }

    private static void assertJsonEquals(Object expected, Object actual) {
        assertEquals(JsonUtils.readTree(JsonUtils.toJson(expected)), JsonUtils.readTree(JsonUtils.toJson(actual)));
    }
}
