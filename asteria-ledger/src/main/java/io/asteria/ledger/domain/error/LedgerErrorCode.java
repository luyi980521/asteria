package io.asteria.ledger.domain.error;

public enum LedgerErrorCode {
    INVALID_MONEY_AMOUNT("LEDGER_0001", "Money amount must be greater than zero"),
    CURRENCY_MISMATCH("LEDGER_0002", "Currencies do not match"),
    NULL_ARGUMENT("LEDGER_0003", "Argument must not be null"),
    INSUFFICIENT_POSTINGS("LEDGER_0004", "Journal entry must contain at least two postings"),
    MULTIPLE_CURRENCIES_NOT_SUPPORTED("LEDGER_0005", "Journal entry must contain postings in the same currency"),

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
