package io.asteria.reconciliation.domain.entity;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.common.util.JsonUtils;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;

import java.time.Instant;
import java.util.HashSet;

/**
 * 六个对账对象共用的构造校验；仅作用于本模块，不引入新的公共基础类型。
 */
final class ReconciliationValidation {
    private ReconciliationValidation() { }

    /**
     * 将无效输入统一转换为模块领域异常。
     */
    static void require(boolean valid) {
        if (!valid) { throw new ReconciliationDomainException(ReconciliationErrorCode.INVALID_PARAMS); }
    }

    /**
     * 标识、状态等短字符串必须非空且不能超过数据库列宽。
     */
    static void text(String value, int maxLength) {
        require(value != null && !value.isBlank() && value.length() <= maxLength);
    }

    /**
     * 审计时间必须存在且不能倒退。
     */
    static void times(Instant createdAt, Instant updatedAt) {
        require(createdAt != null && updatedAt != null && !updatedAt.isBefore(createdAt));
    }

    /**
     * 变更时间不能早于上次更新。
     */
    static void nextTime(Instant previous, Instant next) {
        require(next != null && !next.isBefore(previous));
    }

    /**
     * 序列化字段以 String/TEXT 保存，使用已有 JsonUtils 校验 JSON 对象。
     */
    static void json(String value, boolean required) {
        if (value == null && !required) { return; }
        try {
            if (value == null || !JsonUtils.readTree(value).isObject()) {
                throw new IllegalArgumentException("JSON object required");
            }
        } catch (IllegalArgumentException exception) {
            throw new ReconciliationDomainException(ReconciliationErrorCode.INVALID_JSON, exception);
        }
    }

    /**
     * 来源 ID 列表必须是非空、无重复的正数 Long JSON 数组，关联的是快照主键。
     */
    static void sourceIds(String value) {
        try {
            var array = JsonUtils.readTree(value);
            if (!array.isArray() || array.isEmpty()) { throw new IllegalArgumentException("Source IDs required"); }
            var ids = new HashSet<Long>();
            for (var id : array) {
                if (!id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() <= 0 || !ids.add(id.longValue())) {
                    throw new IllegalArgumentException("Invalid or duplicate source ID");
                }
            }
        } catch (IllegalArgumentException exception) {
            throw new ReconciliationDomainException(ReconciliationErrorCode.INVALID_JSON, exception);
        }
    }

    /**
     * 金额允许为零，缺少金额时整体为空；限制到现有 NUMERIC(38,18) 的无损范围。
     */
    static void money(Money value) {
        if (value != null) {
            require(value.amount().signum() >= 0 && value.amount().scale() <= 18
                    && value.amount().precision() - value.amount().scale() <= 20);
        }
    }

    /**
     * 状态机入口；终态及不允许的转换报告明确的状态错误。
     */
    static void state(boolean valid) {
        if (!valid) { throw new ReconciliationDomainException(ReconciliationErrorCode.INVALID_STATE); }
    }
}
