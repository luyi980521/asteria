package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.entity.Posting;
import lombok.Builder;

/**
 * 分录公开查询快照，保留领域 Posting 未携带的凭证关联及原始序号。
 */
@Builder
public record PostingSnapshot(
        /* 所属凭证。 */
        JournalEntryId journalEntryId,

        /* 数据库中的凭证内序号，不根据集合下标重建。 */
        Integer sequenceNo,

        /* 分录事实。 */
        Posting posting
) { }
