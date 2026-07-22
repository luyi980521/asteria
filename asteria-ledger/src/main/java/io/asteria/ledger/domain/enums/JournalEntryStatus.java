package io.asteria.ledger.domain.enums;

public enum JournalEntryStatus {

    /** 草案(已经生成了凭证，但还没有真正过账) */
    DRAFT,

    /** 入账 */
    POSTED
}
