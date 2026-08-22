package io.asteria.ledger.domain.entity;

import io.asteria.ledger.domain.enums.AccountCategory;
import io.asteria.ledger.domain.enums.AccountOwnerType;
import io.asteria.ledger.domain.enums.AccountStatus;
import io.asteria.ledger.domain.enums.NormalBalance;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.AccountId;
import io.asteria.ledger.domain.valueobject.Money;
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
public class Account {

    /** 账本账户ID */
    private AccountId accountId;

    /** 账户编码 */
    private String accountCode;

    /** 账户所属主体类型 */
    private AccountOwnerType ownerType;

    /** 账户所属主体ID */
    private Long ownerId;

    /** 会计分类 */
    private AccountCategory category;

    /** 账户币种 */
    private Currency currency;

    /** 账户状态 */
    private AccountStatus status;

    /** 是否允许负余额 */
    private boolean allowNegativeBalance;


    /**
     * 创建账本账户
     */
    public static Account create(
            AccountId accountId,
            String accountCode,
            AccountOwnerType ownerType,
            Long ownerId,
            AccountCategory category,
            Currency currency,
            boolean allowNegativeBalance
    ) {
        validateLedgerAccountId(accountId);
        validateAccountCode(accountCode);
        validateOwner(ownerType, ownerId);
        validateCategory(category);
        validateCurrency(currency);

        return Account.builder()
                .accountId(accountId)
                .accountCode(accountCode)
                .ownerType(ownerType)
                .ownerId(ownerId)
                .category(category)
                .currency(currency)
                .status(AccountStatus.ACTIVE)
                .allowNegativeBalance(allowNegativeBalance)
                .build();
    }

    /**
     * 从持久化数据恢复账本账户
     */
    public static Account reconstitute(
            AccountId accountId,
            String accountCode,
            AccountOwnerType ownerType,
            Long ownerId,
            AccountCategory category,
            Currency currency,
            AccountStatus status,
            boolean allowNegativeBalance
    ) {
        validateLedgerAccountId(accountId);
        validateAccountCode(accountCode);
        validateOwner(ownerType, ownerId);
        validateCategory(category);
        validateCurrency(currency);

        if (status == null) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_STATUS_REQUIRED
            );
        }

        return Account.builder()
                .accountId(accountId)
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
        if (status != AccountStatus.ACTIVE) {
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
        if (status == AccountStatus.CLOSED) {
            throw new LedgerDomainException(
                    LedgerErrorCode.CLOSED_LEDGER_ACCOUNT_CANNOT_BE_FROZEN
            );
        }

        status = AccountStatus.FROZEN;
    }

    /**
     * 解冻账户
     */
    public void activate() {
        if (status == AccountStatus.CLOSED) {
            throw new LedgerDomainException(
                    LedgerErrorCode.CLOSED_LEDGER_ACCOUNT_CANNOT_BE_ACTIVATED
            );
        }

        status = AccountStatus.ACTIVE;
    }

    /**
     * 关闭账户
     */
    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new LedgerDomainException(
                    LedgerErrorCode.LEDGER_ACCOUNT_ALREADY_CLOSED
            );
        }

        status = AccountStatus.CLOSED;
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
        return status == AccountStatus.ACTIVE;
    }

    /**
     * 校验账本账户ID
     */
    private static void validateLedgerAccountId(
            AccountId accountId
    ) {
        if (accountId == null) {
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
            AccountOwnerType ownerType,
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
    private static void validateCategory(AccountCategory category) {
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
