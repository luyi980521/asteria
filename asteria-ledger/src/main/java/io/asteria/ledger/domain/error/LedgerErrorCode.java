package io.asteria.ledger.domain.error;

public enum LedgerErrorCode {
    /*********************** 通用错误 ***********************/
    INVALID_PARAMS("COMMON_0001", "Invalid params"),
    NULL_ARGUMENT("COMMON_0002", "Argument must not be null"),

    /*********************** 账本模块错误 ***********************/
    INVALID_MONEY_AMOUNT("LEDGER_0001", "Money amount must be greater than zero"),
    CURRENCY_MISMATCH("LEDGER_0002", "Currencies do not match"),
    INSUFFICIENT_POSTINGS("LEDGER_0003", "Journal entry must contain at least two postings"),
    MULTIPLE_CURRENCIES_NOT_SUPPORTED("LEDGER_0004", "Journal entry must contain postings in the same currency"),
    DEBIT_CREDIT_DIRECTION_ILLEGAL("LEDGER_0005", "DebitCredit direction is illegal"),
    JOURNAL_ENTRY_NOT_BALANCED("LEDGER_0006", "Journal entry not balanced"),
    POSTING_AMOUNT_MUST_BE_POSITIVE("LEDGER_0007", "Posting amount must be positive"),
    JOURNAL_ENTRY_CANNOT_BE_POSTED("LEDGER_0008", "Journal entry cannot be posted"),
    ONLY_POSTED_ENTRY_CAN_BE_REVERSED("LEDGER_0009", "Only posted entry can be reversed"),
    JOURNAL_ENTRY_ALREADY_REVERSED("LEDGER_0010", "Journal entry already reversed"),
    REVERSED_AT_BEFORE_POSTED_AT("LEDGER_0011", "Reversed at before posted at"),
    SOURCE_TYPE_REQUIRED("LEDGER_0012", "Source type required"),
    SOURCE_ID_REQUIRED("LEDGER_0013", "Source id required"),
    EVENT_TYPE_REQUIRED("LEDGER_0014", "Event type required"),
    EVENT_ID_REQUIRED("LEDGER_0015", "Event id required"),
    DUPLICATE_LEDGER_EVENT("LEDGER_0016", "Duplicate ledger event"),
    LEDGER_ACCOUNT_ID_REQUIRED("LEDGER_0017", "Ledger account id required"),
    LEDGER_ACCOUNT_CODE_REQUIRED("LEDGER_0018", "Ledger account code required"),
    LEDGER_ACCOUNT_OWNER_TYPE_REQUIRED("LEDGER_0019", "Ledger account owner type required"),
    LEDGER_ACCOUNT_OWNER_ID_ILLEGAL("LEDGER_0020", "Ledger account owner id must be greater than zero"),
    LEDGER_ACCOUNT_CATEGORY_REQUIRED("LEDGER_0021", "Ledger account category required"),
    LEDGER_ACCOUNT_CURRENCY_REQUIRED("LEDGER_0022", "Ledger account currency required"),
    LEDGER_ACCOUNT_STATUS_REQUIRED("LEDGER_0023", "Ledger account status required"),
    LEDGER_ACCOUNT_NOT_ACTIVE("LEDGER_0024", "Ledger account is not active"),
    LEDGER_ACCOUNT_CURRENCY_MISMATCH("LEDGER_0025", "Ledger account currency does not match posting currency"),
    POSTING_MONEY_REQUIRED("LEDGER_0026", "Posting money required"),
    CLOSED_LEDGER_ACCOUNT_CANNOT_BE_FROZEN("LEDGER_0027", "Closed ledger account cannot be frozen"),
    CLOSED_LEDGER_ACCOUNT_CANNOT_BE_ACTIVATED("LEDGER_0028", "Closed ledger account cannot be activated"),
    LEDGER_ACCOUNT_ALREADY_CLOSED("LEDGER_0029", "Ledger account is already closed"),
    LEDGER_ACCOUNT_ALREADY_EXISTS("LEDGER_0030", "Ledger account is already exists"),
    LEDGER_ACCOUNT_NOT_EXIST("LEDGER_0031", "Ledger account doesn't exist"),

    ;

    private final String code;
    private final String defaultMessage;

    LedgerErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
