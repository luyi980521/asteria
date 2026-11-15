package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.entity.JournalEntry;
import java.time.Instant;
import lombok.Builder;

/**
 * 凭证公开查询快照，保留数据库版本及审计时间，供文件采集使用而不暴露 DO。
 */
@Builder
public record JournalEntrySnapshot(
        /* 完整凭证与有序分录。 */
        JournalEntry journalEntry,

        /* 数据库乐观锁版本，冲正等更新后递增。 */
        Long version,

        /* 创建时间。 */
        Instant createdAt,

        /* 最近更新时间。 */
        Instant updatedAt
) { }
