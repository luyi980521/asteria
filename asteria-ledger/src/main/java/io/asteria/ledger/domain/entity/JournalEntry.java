package io.asteria.ledger.domain.entity;

import io.asteria.ledger.domain.enums.JournalEntryStatus;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

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

    /**
     * 创建记账凭证
     * */
    public static JournalEntry create(List<Posting> postings,
                                      JournalEntryStatus status, String reference) {

        validatePostingCount(postings);
        validateSameCurrency(postings);
        validateBalanced(postings);

        return JournalEntry.builder()
                .journalEntryId(JournalEntryId.generate())
                .postings(postings)
                .status(status)
                .reference(reference)
                .build();
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

        // TODO 校验分录币种是否一致
    }

    /**
     * 校验分录借贷金额是否平衡
     * */
    private static void validateBalanced(List<Posting> postings) {

        // TODO 校验分录借贷是否一致
    }
}