package io.asteria.ledger.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 记账分录表
 * @TableName ledger_posting
 */
@TableName(value ="ledger_posting")
@Data
public class PostingDO {
    /**
     * 分录ID
     */
    @TableId
    private String id;

    /**
     * 所属记账凭证ID
     */
    private String journalEntryId;

    /**
     * 账本账户ID
     */
    private String ledgerAccountId;

    /**
     * 分录金额，必须大于0；借贷方向由direction字段表达
     */
    private BigDecimal amount;

    /**
     * 币种代码，例如 CNY、USD、EUR
     */
    private String currency;

    /**
     * 借贷方向：DEBIT-借方，CREDIT-贷方
     */
    private String direction;

    /**
     * 分录在记账凭证中的顺序号
     */
    private Integer sequenceNo;

    /**
     * 记录创建时间
     */
    private Date createdAt;
}