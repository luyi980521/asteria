package io.asteria.reconciliation.application.model;

import lombok.Builder;
import java.math.BigDecimal;

/**
 * 分录文件快照，按 journalEntryId 关联凭证头。
 */
@Builder
public record LedgerPostingExportRecord(
        /* 分录主键。 */
        Long postingId,

        /* 所属凭证主键。 */
        Long journalEntryId,

        /* 凭证内序号。 */
        Integer sequenceNo,

        /* 账本账户主键。 */
        Long ledgerAccountId,

        /* 原始金额，不在传输层判断借贷。 */
        BigDecimal amount,

        /* 原始币种。 */
        String currency,

        /* 借贷方向。 */
        String direction
) { }
