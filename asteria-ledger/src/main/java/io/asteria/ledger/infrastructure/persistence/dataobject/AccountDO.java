package io.asteria.ledger.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 账本账户表
 * @TableName ledger_account
 */
@TableName(value ="ledger_account")
@Data
public class AccountDO {
    /**
     * 账本账户ID，由应用层雪花算法生成
     */
    @TableId
    private Long id;

    /**
     * 账户业务编码，全局唯一
     */
    private String accountCode;

    /**
     * 账户所属主体类型：USER、MERCHANT、PLATFORM、CHANNEL、BANK、SYSTEM
     */
    private String ownerType;

    /**
     * 账户所属主体ID
     */
    private Long ownerId;

    /**
     * 会计分类：ASSET、LIABILITY、EQUITY、REVENUE、EXPENSE
     */
    private String category;

    /**
     * 账户币种，例如 CNY、USD、EUR
     */
    private String currency;

    /**
     * 账户状态：ACTIVE、FROZEN、CLOSED
     */
    private String status;

    /**
     * 是否允许出现负余额
     */
    private Boolean allowNegativeBalance;

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
    @Version
    private Long version;
}