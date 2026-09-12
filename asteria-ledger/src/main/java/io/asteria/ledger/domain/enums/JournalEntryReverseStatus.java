package io.asteria.ledger.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记账凭证冲正状态枚举定义
 * */
@Getter
@AllArgsConstructor
public enum JournalEntryReverseStatus {

    REVERSED,
    ALREADY_REVERSED,
    FAILED,

    ;
}
