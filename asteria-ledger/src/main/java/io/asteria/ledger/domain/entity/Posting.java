package io.asteria.ledger.domain.entity;

import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.ledger.domain.valueobject.PostingId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 分录实体对象定义
 * */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Posting {

    /** 分录id */
    private PostingId postingId;

    /** 账本id */
    private LedgerAccountId ledgerAccountId;

    /** 货币 */
    private Money money;

    /** 方向 */
    private DebitCredit direction;

    public static Posting create(PostingId postingId,
                                 LedgerAccountId ledgerAccountId,
                                 Money money,
                                 DebitCredit direction) {

        if (postingId == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
        return Posting.builder()
                .postingId(postingId)
                .ledgerAccountId(ledgerAccountId)
                .money(money)
                .direction(direction)
                .build();
    }

    /**
     * 从持久化数据恢复分录。
     */
    public static Posting reconstitute(PostingId postingId,
                                       LedgerAccountId ledgerAccountId,
                                       Money money,
                                       DebitCredit direction) {

        if (postingId == null || ledgerAccountId == null || money == null || direction == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        return Posting.builder()
                .postingId(postingId)
                .ledgerAccountId(ledgerAccountId)
                .money(money)
                .direction(direction)
                .build();
    }

    public Posting reverse(PostingId postingId) {
        if (postingId == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
        return Posting.builder()
                .postingId(postingId)
                .ledgerAccountId(this.ledgerAccountId)
                .money(this.money)
                .direction(this.direction.reverse())
                .build();
    }
}
