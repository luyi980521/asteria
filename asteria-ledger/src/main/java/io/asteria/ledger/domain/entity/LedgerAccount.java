package io.asteria.ledger.domain.entity;

import io.asteria.ledger.domain.enums.LedgerAccountCategory;
import io.asteria.ledger.domain.enums.LedgerAccountOwnerType;
import io.asteria.ledger.domain.enums.LedgerAccountStatus;
import io.asteria.ledger.domain.enums.NormalBalance;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.common.domain.valueobject.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Currency;
import java.util.Objects;

/**
 * 账本账户
 * */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerAccount {

    /** 账本账户ID */
    private LedgerAccountId ledgerAccountId;

    /** 账户编码 */
    private String accountCode;

    /** 账户所属主体类型 */
    private LedgerAccountOwnerType ownerType;

    /** 账户所属主体ID */
    private Long ownerId;

    /** 会计分类 */
    private LedgerAccountCategory category;

    /** 账户币种 */
    private Currency currency;

    /** 账户状态 */
    private LedgerAccountStatus status;

    /** 是否允许负余额 */
    private boolean allowNegativeBalance;


    /**
     * 创建账本账户
     */
    public static LedgerAccount create(
            LedgerAccountId ledgerAccountId,
            String accountCode,
            LedgerAccountOwnerType ownerType,
            Long ownerId,
            LedgerAccountCategory category,
            Currency currency,
            boolean allowNegativeBalance
    ) {
        validateLedgerAccountId(ledgerAccountId);
        validateAccountCode(accountCode);
        validateOwner(ownerType, ownerId);
        validateCategory(category);
        validateCurrency(currency);

        return LedgerAccount.builder()
                .ledgerAccountId(ledgerAccountId)
                .accountCode(accountCode)
                .ownerType(ownerType)
                .ownerId(ownerId)
                .category(category)
                .currency(currency)
                .status(LedgerAccountStatus.ACTIVE)
                .allowNegativeBalance(allowNegativeBalance)
                .build();
    }

    /**
     * 从持久化数据恢复账本账户
     */
    public static LedgerAccount reconstitute(
            LedgerAccountId ledgerAccountId,
            String accountCode,
            LedgerAccountOwnerType ownerType,
            Long ownerId,
            LedgerAccountCategory category,
            Currency currency,
            LedgerAccountStatus status,
            boolean allowNegativeBalance
    ) {
        validateLedgerAccountId(ledgerAccountId);
        validateAccountCode(accountCode);
        validateOwner(ownerType, ownerId);
        validateCategory(category);
        validateCurrency(currency);

        if (status == null) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_STATUS_REQUIRED
            );
        }

        return LedgerAccount.builder()
                .ledgerAccountId(ledgerAccountId)
                .accountCode(accountCode)
                .ownerType(ownerType)
                .ownerId(ownerId)
                .category(category)
                .currency(currency)
                .status(status)
                .allowNegativeBalance(allowNegativeBalance)
                .build();
    }

    /**
     * 校验账户是否允许记账
     */
    public void validatePostable(Money money) {
        if (status != LedgerAccountStatus.ACTIVE) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_NOT_ACTIVE
            );
        }

        if (money == null) {
            throw new LedgerDomainException(
                    LedgerErrorCode.POSTING_MONEY_REQUIRED
            );
        }

        if (!Objects.equals(currency, money.currency())) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_CURRENCY_MISMATCH
            );
        }
    }

    /**
     * 冻结账户
     */
    public void freeze() {
        if (status == LedgerAccountStatus.CLOSED) {
            throw new LedgerDomainException(
                    LedgerErrorCode.CLOSED_LEDGER_ACCOUNT_CANNOT_BE_FROZEN
            );
        }

        status = LedgerAccountStatus.FROZEN;
    }

    /**
     * 解冻账户
     */
    public void activate() {
        if (status == LedgerAccountStatus.CLOSED) {
            throw new LedgerDomainException(
                    LedgerErrorCode.CLOSED_LEDGER_ACCOUNT_CANNOT_BE_ACTIVATED
            );
        }

        status = LedgerAccountStatus.ACTIVE;
    }

    /**
     * 关闭账户
     */
    public void close() {
        if (status == LedgerAccountStatus.CLOSED) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_ALREADY_CLOSED
            );
        }

        status = LedgerAccountStatus.CLOSED;
    }

    /**
     * 获取账户正常余额方向
     */
    public NormalBalance normalBalance() {
        return category.getNormalBalance();
    }

    /**
     * 判断账户当前是否可记账
     */
    public boolean isPostable() {
        return status == LedgerAccountStatus.ACTIVE;
    }

    /**
     * 校验账本账户ID
     */
    private static void validateLedgerAccountId(
            LedgerAccountId ledgerAccountId
    ) {
        if (ledgerAccountId == null) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_ID_REQUIRED
            );
        }
    }

    /**
     * 校验账户编码
     */
    private static void validateAccountCode(String accountCode) {
        if (StringUtils.isBlank(accountCode)) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_CODE_REQUIRED
            );
        }
    }

    /**
     * 校验账户所属主体
     */
    private static void validateOwner(
            LedgerAccountOwnerType ownerType,
            Long ownerId
    ) {
        if (ownerType == null) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_OWNER_TYPE_REQUIRED
            );
        }

        if (ownerId == null || ownerId <= 0) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_OWNER_ID_ILLEGAL
            );
        }
    }

    /**
     * 校验账户会计分类
     */
    private static void validateCategory(LedgerAccountCategory category) {
        if (category == null) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_CATEGORY_REQUIRED
            );
        }
    }

    /**
     * 校验账户币种
     */
    private static void validateCurrency(Currency currency) {
        if (currency == null) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_CURRENCY_REQUIRED
            );
        }
    }
}
