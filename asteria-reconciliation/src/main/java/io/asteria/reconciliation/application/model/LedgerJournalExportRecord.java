package io.asteria.reconciliation.application.model;

import lombok.Builder;
import java.time.Instant;

/**
 * 凭证头文件快照；金额保留在分录文件，不在 DTO 中汇总。
 */
@Builder
public record LedgerJournalExportRecord(
        /* 凭证主键。 */
        Long journalEntryId,

        /* JournalReference 来源类型。 */
        String sourceType,

        /* JournalReference 来源标识，保留原始命名空间。 */
        String sourceId,

        /* 来源事件类型。 */
        String eventType,

        /* 来源事件唯一标识。 */
        String eventId,

        /* 凭证状态。 */
        String status,

        /* 入账业务时间。 */
        Instant postedAt,

        /* 被冲正凭证主键，可空。 */
        Long originalJournalEntryId,

        /* 冲正凭证主键，可空。 */
        Long reversingJournalEntryId,

        /* 冲正原因，可空。 */
        String reversalReason,

        /* 被冲正时间，可空。 */
        Instant reversedAt,

        /* 数据库行版本，避免冲正后再次采集覆盖旧快照。 */
        Long sourceVersion,

        /* 凭证创建时间。 */
        Instant createdAt,

        /* 凭证更新时间。 */
        Instant updatedAt
) { }
