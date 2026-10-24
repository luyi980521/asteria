package io.asteria.reconciliation;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.common.util.JsonUtils;
import io.asteria.reconciliation.domain.entity.*;
import io.asteria.reconciliation.domain.enums.*;
import io.asteria.reconciliation.domain.valueobject.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 固定业务样本；所有 ID 来自测试输入，不申请或修改生产数据。
 */
final class ReconciliationFixtures {
    static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    static final Instant END = NOW.minusSeconds(60);
    static final Instant START = END.minusSeconds(3600);
    static final String DETAILS = JsonUtils.toJson(Map.of("说明", "支付尚未入账"));

    private ReconciliationFixtures() { }

    static ReconciliationRule rule() {
        return ReconciliationRule.create(ReconciliationRuleId.of(1L), "PAYMENT_LEDGER_V1", "支付账本核对",
                ReconciliationType.PAYMENT_LEDGER, 60L, DETAILS, NOW);
    }

    static ReconciliationSourceRecord source(long id, long version) {
        return ReconciliationSourceRecord.builder().recordId(ReconciliationSourceRecordId.of(id))
                .sourceType(ReconciliationSourceType.PAYMENT).sourceRecordId("channel-a:payment-1")
                .sourceVersion(version).businessKey("merchant-1:payment-1").amount(money("0", "USD"))
                .sourceStatus("CAPTURED").postingStatus("NOT_POSTED").occurredAt(START)
                .payload(JsonUtils.toJson(Map.of("paymentId", 1L))).createdAt(NOW).build();
    }

    static Money money(String amount, String currency) {
        return Money.ofNonNegative(new BigDecimal(amount), CurrencyCode.of(currency));
    }

    static ReconciliationRun run() {
        return ReconciliationRun.create(ReconciliationRunId.of(3L), rule(), START, END, NOW, NOW);
    }

    static ReconciliationMatchResult result(long id, ReconciliationMatchStatus status,
                                              ReconciliationDifferenceType difference) {
        return ReconciliationMatchResult.builder().matchResultId(ReconciliationMatchResultId.of(id))
                .runId(ReconciliationRunId.of(3L)).ruleCode("PAYMENT_LEDGER_V1").businessKey("merchant-1:payment-1")
                .status(status).differenceType(difference).expectedAmount(money("10", "USD"))
                .actualAmount(money("10", "JPY")).sourceRecordIds(JsonUtils.toJson(List.of(2L)))
                .details(DETAILS).createdAt(NOW).build();
    }

    static ReconciliationCase reconciliationCase(long id) {
        return ReconciliationCase.open(ReconciliationCaseId.of(id),
                result(4L, ReconciliationMatchStatus.MISSING, ReconciliationDifferenceType.MISSING_LEDGER), NOW);
    }

    static ReconciliationCaseAction action() {
        return ReconciliationCaseAction.builder().caseActionId(ReconciliationCaseActionId.of(6L))
                .caseId(ReconciliationCaseId.of(5L)).actionType(ReconciliationCaseActionType.OPEN)
                .operatorType(ReconciliationOperatorType.SYSTEM).operatorId("reconciliation")
                .fromStatus(null).toStatus(ReconciliationCaseStatus.OPEN).reason("发现缺少账本记录")
                .details(DETAILS).createdAt(NOW).build();
    }
}
