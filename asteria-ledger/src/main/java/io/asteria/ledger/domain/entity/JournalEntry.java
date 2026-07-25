package io.asteria.ledger.domain.entity;

import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.enums.JournalEntryStatus;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import io.asteria.ledger.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 记账凭证实体定义
 * */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    /** 记账凭证id */
    private JournalEntryId journalEntryId;

    /** 分录数据集合 */
    private List<Posting> postings;

    /** 记账凭证状态 */
    private JournalEntryStatus status;

    /** 外部业务编号 */
    private String reference;

    /** 入账时间 */
    private Instant postedAt;

    /**
     * 创建记账凭证
     * */
    public JournalEntry create(List<Posting> postings, String reference) {

        validatePostingCount(postings);
        validateSameCurrency(postings);
        validateBalanced(postings);
        validatePositiveAmounts(postings);

        return JournalEntry.builder()
                .journalEntryId(JournalEntryId.generate())
                .postings(postings)
                .status(JournalEntryStatus.DRAFT)
                .reference(reference)
                .postedAt(null)
                .build();
    }

    /**
     * 记账凭证入账
     * */
    public void post(Instant postedAt) {

        if (this.status != JournalEntryStatus.DRAFT) {
            throw new LedgerDomainException(LedgerErrorCode.JOURNAL_ENTRY_CANNOT_BE_POSTED);
        }

        if (postedAt == null) {
            throw new LedgerDomainException(LedgerErrorCode.POSTED_AT_REQUIRED);
        }

        validatePostingCount(postings);
        validateSameCurrency(postings);
        validateBalanced(postings);
        validatePositiveAmounts(postings);

        this.status = JournalEntryStatus.POSTED;
        this.postedAt = postedAt;
    }

    /**
     * 校验分录记录数量是否达到标准
     * */
    private void validatePostingCount(List<Posting> postings) {

        if (CollectionUtils.isEmpty(postings) || postings.size() < 2) {
            throw new LedgerDomainException(LedgerErrorCode.INSUFFICIENT_POSTINGS);
        }
    }

    /**
     * 校验分录币种是否一致
     * */
    private void validateSameCurrency(List<Posting> postings) {

        List<Money> monies = postings.stream().map(Posting::getMoney).toList();
        Set<Currency> currencySet = monies.stream().map(Money::currency).collect(Collectors.toSet());
        if (currencySet.size() != 1) {
            throw new LedgerDomainException(LedgerErrorCode.MULTIPLE_CURRENCIES_NOT_SUPPORTED);
        }
    }

    /**
     * 校验分录借贷金额是否平衡
     * */
    private void validateBalanced(List<Posting> postings) {

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (Posting posting : postings) {

            if (posting.getDirection() == DebitCredit.DEBIT) {
                debitTotal = debitTotal.add(posting.getMoney().amount());
            } else if (posting.getDirection() == DebitCredit.CREDIT) {
                creditTotal = creditTotal.add(posting.getMoney().amount());
            } else {
                throw new LedgerDomainException(LedgerErrorCode.DEBIT_CREDIT_DIRECTION_ILLEGAL);
            }
        }

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new LedgerDomainException(LedgerErrorCode.JOURNAL_ENTRY_NOT_BALANCED);
        }
    }

    /**
     * 校验分录金额是否为正数
     * */
    private void validatePositiveAmounts(List<Posting> postings) {

        boolean hasIllegalAmount = postings.stream()
                .map(Posting::getMoney)
                .map(Money::amount)
                .anyMatch(amount -> amount.compareTo(BigDecimal.ZERO) <= 0);

        if (hasIllegalAmount) {
            throw new LedgerDomainException(LedgerErrorCode.POSTING_AMOUNT_MUST_BE_POSITIVE);
        }
    }
}