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
