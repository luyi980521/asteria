package io.asteria.ledger.domain.entity;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.enums.JournalEntryStatus;
import io.asteria.ledger.domain.enums.JournalReferenceEventType;
import io.asteria.ledger.domain.enums.JournalReferenceSourceType;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.EventId;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import io.asteria.ledger.domain.valueobject.JournalReference;
import io.asteria.ledger.domain.valueobject.PostingId;
import io.asteria.common.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
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
    private JournalReference reference;

    /** 被冲正凭证ID：当前凭证是冲正凭证时使用 */
    private JournalEntryId originalJournalEntryId;

    /** 冲正凭证ID：当前凭证已被冲正时使用 */
    private JournalEntryId reversingJournalEntryId;

    /** 冲正原因 */
    private String reversalReason;

    /** 入账时间 */
    private Instant postedAt;

    /** 被冲正时间 */
    private Instant reversedAt;

    /**
     * 创建记账凭证
     * */
    public static JournalEntry create(JournalEntryId journalEntryId,
                                      List<Posting> postings,
                                      JournalReference reference) {

        if (journalEntryId == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
        validatePostingCount(postings);
        validateSameCurrency(postings);
        validateBalanced(postings);
        validatePositiveAmounts(postings);

        return JournalEntry.builder()
                .journalEntryId(journalEntryId)
                .postings(postings)
                .status(JournalEntryStatus.DRAFT)
                .reference(reference)
                .postedAt(null)
                .build();
    }

    /**
     * 从持久化数据恢复记账凭证聚合。
     *
     * 此方法不生成 ID，也不执行创建凭证流程，仅用于恢复已存在的聚合。
     */
    public static JournalEntry reconstitute(JournalEntryId journalEntryId,
                                            List<Posting> postings,
                                            JournalEntryStatus status,
                                            JournalReference reference,
                                            Instant postedAt,
                                            JournalEntryId originalJournalEntryId,
                                            JournalEntryId reversingJournalEntryId,
                                            String reversalReason,
                                            Instant reversedAt) {

        if (journalEntryId == null || reference == null || status == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        if (CollectionUtils.isEmpty(postings)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        if (status == JournalEntryStatus.DRAFT && postedAt != null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        if (status == JournalEntryStatus.POSTED && postedAt == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        return JournalEntry.builder()
                .journalEntryId(journalEntryId)
                .postings(List.copyOf(postings))
                .status(status)
                .reference(reference)
                .postedAt(postedAt)
                .originalJournalEntryId(originalJournalEntryId)
                .reversingJournalEntryId(reversingJournalEntryId)
                .reversalReason(reversalReason)
                .reversedAt(reversedAt)
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
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        validatePostingCount(postings);
        validateSameCurrency(postings);
        validateBalanced(postings);
        validatePositiveAmounts(postings);

        this.status = JournalEntryStatus.POSTED;
        this.postedAt = postedAt;
    }

    /**
     * 冲正
     * */
    public JournalEntry reverse(JournalEntryId reversalJournalEntryId,
                                List<PostingId> reversalPostingIds,
                                Instant reversedAt,
                                String reason) {

        validateReversible(reversedAt, reason);
        validateReversalIds(reversalJournalEntryId, reversalPostingIds);

        // 生成冲正分录数据
        List<Posting> reversedPostings = java.util.stream.IntStream.range(0, this.postings.size())
                .mapToObj(index -> this.postings.get(index).reverse(reversalPostingIds.get(index)))
                .toList();
        JournalEntry reversalEntry = createReversal(
                reversalJournalEntryId,
                reversedPostings,
                journalEntryId,
                reason);
        reversalEntry.post(reversedAt);

        this.reversingJournalEntryId = reversalEntry.getJournalEntryId();
        this.reversedAt = reversedAt;

        return reversalEntry;
    }

    private JournalEntry createReversal(JournalEntryId reversalJournalEntryId,
                                        List<Posting> reversedPostings,
                                        JournalEntryId originalJournalEntryId, String reason) {

        validatePostingCount(postings);
        validateSameCurrency(postings);
        validateBalanced(postings);
        validatePositiveAmounts(postings);

        JournalReference journalReference = new JournalReference(
                JournalReferenceSourceType.JOURNAL_ENTRY.name(),
                originalJournalEntryId.value().toString(),
                JournalReferenceEventType.JOURNAL_ENTRY_REVERSED.name(),
                EventId.generate().value()
        );

        return JournalEntry.builder()
                .journalEntryId(reversalJournalEntryId)
                .postings(reversedPostings)
                .status(JournalEntryStatus.DRAFT)
                .reference(journalReference)
                .originalJournalEntryId(originalJournalEntryId)
                .reversalReason(reason)
                .build();
    }

    private void validateReversalIds(JournalEntryId reversalJournalEntryId,
                                     List<PostingId> reversalPostingIds) {

        if (reversalJournalEntryId == null || CollectionUtils.isEmpty(reversalPostingIds)
                || reversalPostingIds.size() != this.postings.size()) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        if (reversalJournalEntryId.equals(this.journalEntryId)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        for (int index = 0; index < this.postings.size(); index++) {
            PostingId reversalPostingId = reversalPostingIds.get(index);
            if (reversalPostingId == null
                    || reversalPostingId.equals(this.postings.get(index).getPostingId())) {
                throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
            }
        }
    }

    /**
     * 校验分录记录数量是否达到标准
     * */
    private static void validatePostingCount(List<Posting> postings) {

        if (CollectionUtils.isEmpty(postings) || postings.size() < 2) {
            throw new LedgerDomainException(LedgerErrorCode.INSUFFICIENT_POSTINGS);
        }
    }

    /**
     * 校验分录币种是否一致
     * */
    private static void validateSameCurrency(List<Posting> postings) {

        List<Money> monies = postings.stream().map(Posting::getMoney).toList();
        Set<CurrencyCode> currencySet = monies.stream().map(Money::currency).collect(Collectors.toSet());
        if (currencySet.size() != 1) {
            throw new LedgerDomainException(LedgerErrorCode.MULTIPLE_CURRENCIES_NOT_SUPPORTED);
        }
    }

    /**
     * 校验分录借贷金额是否平衡
     * */
    private static void validateBalanced(List<Posting> postings) {

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
    private static void validatePositiveAmounts(List<Posting> postings) {

        boolean hasIllegalAmount = postings.stream()
                .map(Posting::getMoney)
                .map(Money::amount)
                .anyMatch(amount -> amount.compareTo(BigDecimal.ZERO) <= 0);

        if (hasIllegalAmount) {
            throw new LedgerDomainException(LedgerErrorCode.POSTING_AMOUNT_MUST_BE_POSITIVE);
        }
    }

    /**
     * 校验是否可冲正
     *
     * 冲正规则:
     * 1. DRAFT 不能冲正
     * 2. 只有 POSTED 可以冲正
     * 3. 原凭证不能重复冲正
     * 4. 冲正时间不能早于原入账时间
     * 5. 原因不能为空
     * 6. 反向凭证必须自动正式入账
     * 7. 原凭证历史分录不能被修改
     * */
    private void validateReversible(Instant reversedAt, String reason) {

        if (reversedAt == null || StringUtils.isBlank(reason)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        if (this.status != JournalEntryStatus.POSTED) {
            throw new LedgerDomainException(LedgerErrorCode.ONLY_POSTED_ENTRY_CAN_BE_REVERSED);
        }

        if (this.reversingJournalEntryId != null) {
            throw new LedgerDomainException(LedgerErrorCode.JOURNAL_ENTRY_ALREADY_REVERSED);
        }

        if (reversedAt.isBefore(this.postedAt)) {
            throw new LedgerDomainException(LedgerErrorCode.REVERSED_AT_BEFORE_POSTED_AT);
        }
    }
}
