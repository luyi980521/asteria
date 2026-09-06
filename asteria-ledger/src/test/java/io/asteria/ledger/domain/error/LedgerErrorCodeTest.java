package io.asteria.ledger.domain.error;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LedgerErrorCodeTest {

    @Test
    void allCodesAreNonEmptyAndUnique() {
        Set<String> codes = new HashSet<>();

        Arrays.stream(LedgerErrorCode.values()).forEach(errorCode -> {
            assertFalse(errorCode.code().isBlank());
            assertFalse(errorCode.defaultMessage().isBlank());
            assertNotEquals(String.valueOf(errorCode.ordinal()), errorCode.code());
            codes.add(errorCode.code());
        });

        assertEquals(LedgerErrorCode.values().length, codes.size());
    }

    @Test
    void codesHaveExpectedValues() {
        assertEquals("LEDGER_0001", LedgerErrorCode.INVALID_MONEY_AMOUNT.code());
        assertEquals("LEDGER_0002", LedgerErrorCode.CURRENCY_MISMATCH.code());
        assertEquals("COMMON_0002", LedgerErrorCode.NULL_ARGUMENT.code());
    }
}
