package io.asteria.ledger.domain.entity;

import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.ledger.domain.valueobject.Money;
import io.asteria.ledger.domain.valueobject.PostingId;
import lombok.*;

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

    public Posting create(LedgerAccountId ledgerAccountId,
                          Money money, DebitCredit direction) {

        return Posting.builder()
                .postingId(PostingId.generate())
                .ledgerAccountId(ledgerAccountId)
                .money(money)
                .direction(direction)
                .build();
    }

    public Posting reverse() {
        return Posting.builder()
                .postingId(PostingId.generate())
                .ledgerAccountId(this.ledgerAccountId)
                .money(this.money)
                .direction(this.direction.reverse())
                .build();
    }
}
