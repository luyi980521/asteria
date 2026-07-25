package io.asteria.ledger.domain.error;

public enum LedgerErrorCode {
    INVALID_MONEY_AMOUNT("LEDGER_0001", "Money amount must be greater than zero"),
    CURRENCY_MISMATCH("LEDGER_0002", "Currencies do not match"),
    NULL_ARGUMENT("LEDGER_0003", "Argument must not be null"),
    INSUFFICIENT_POSTINGS("LEDGER_0004", "Journal entry must contain at least two postings"),
    MULTIPLE_CURRENCIES_NOT_SUPPORTED("LEDGER_0005", "Journal entry must contain postings in the same currency"),
    DEBIT_CREDIT_DIRECTION_ILLEGAL("LEDGER_0006", "DebitCredit direction is illegal"),
    JOURNAL_ENTRY_NOT_BALANCED("LEDGER_0007", "Journal entry not balanced"),
    POSTING_AMOUNT_MUST_BE_POSITIVE("LEDGER_0008", "Posting amount must be positive"),
    JOURNAL_ENTRY_CANNOT_BE_POSTED("LEDGER_0009", "Journal entry cannot be posted"),
    INVALID_PARAMS("LEDGER_0010", "Invalid params"),
    ONLY_POSTED_ENTRY_CAN_BE_REVERSED("LEDGER_0011", "Only posted entry can be reversed"),
    JOURNAL_ENTRY_ALREADY_REVERSED("LEDGER_0012", "Journal entry already reversed"),
    REVERSED_AT_BEFORE_POSTED_AT("LEDGER_0013", "Reversed at before posted at"),
    SOURCE_TYPE_REQUIRED("LEDGER_0014", "Source type required"),
    SOURCE_ID_REQUIRED("LEDGER_0015", "Source id required"),
    EVENT_TYPE_REQUIRED("LEDGER_0016", "Event type required"),
    EVENT_ID_REQUIRED("LEDGER_0017", "Event id required"),

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
