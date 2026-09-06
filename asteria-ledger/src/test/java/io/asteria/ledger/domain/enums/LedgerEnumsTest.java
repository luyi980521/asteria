package io.asteria.ledger.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class LedgerEnumsTest {

    @Test
    void debitCreditHasExpectedValues() {
        assertArrayEquals(new DebitCredit[]{DebitCredit.DEBIT, DebitCredit.CREDIT}, DebitCredit.values());
    }

    @Test
    void ledgerAccountStatusHasExpectedValues() {
        assertArrayEquals(new LedgerAccountStatus[]{LedgerAccountStatus.ACTIVE, LedgerAccountStatus.FROZEN,
                LedgerAccountStatus.CLOSED}, LedgerAccountStatus.values());
    }

    @Test
    void journalEntryStatusHasExpectedValues() {
        assertArrayEquals(new JournalEntryStatus[]{JournalEntryStatus.DRAFT, JournalEntryStatus.POSTED}, JournalEntryStatus.values());
    }
}
