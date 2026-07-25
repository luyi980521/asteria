package io.asteria.ledger.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 记账凭证表
 * @TableName ledger_journal_entry
 */
@TableName(value ="ledger_journal_entry")
@Data
public class JournalEntryDO {
    /**
     * 记账凭证ID
     */
    @TableId
    private String id;

    /**
     * 来源业务对象类型，例如 PAYMENT、REFUND、SETTLEMENT
     */
    private String sourceType;

    /**
     * 来源业务对象ID
     */
    private String sourceId;

    /**
     * 触发记账的业务事件类型，例如 PAYMENT_CAPTURED
     */
    private String eventType;

    /**
     * 业务事件唯一ID，用于幂等控制
     */
    private String eventId;

    /**
     * 记账凭证状态：DRAFT-草稿，POSTED-已入账
     */
    private String status;

    /**
     * 正式入账时间，草稿状态时为空
     */
    private Date postedAt;

    /**
     * 原记账凭证ID；当前凭证为冲正凭证时，指向被冲正的原凭证
     */
    private String originalJournalEntryId;

    /**
     * 冲正凭证ID；当前凭证已被冲正时，指向对应的冲正凭证
     */
    private String reversingJournalEntryId;

    /**
     * 冲正原因
     */
    private String reversalReason;

    /**
     * 原凭证被冲正的时间
     */
    private Date reversedAt;

    /**
     * 记录创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /**
     * 记录最后更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /**
     * 乐观锁版本号
     */
    private Long version;
}
