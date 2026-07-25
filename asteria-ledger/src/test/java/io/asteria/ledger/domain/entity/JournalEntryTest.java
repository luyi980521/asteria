package io.asteria.ledger.domain.entity;

import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.enums.JournalEntryStatus;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.ledger.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalEntryTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Instant POSTED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant REVERSED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void createsBalancedEntryAsDraft() {
        JournalEntry entry = new JournalEntry().create(validPostings(), "REF-001");

        assertEquals(JournalEntryStatus.DRAFT, entry.getStatus());
        assertNull(entry.getPostedAt());
        assertEquals(2, entry.getPostings().size());
    }

    @Test
    void rejectsEntryWithFewerThanTwoPostings() {
        assertError(LedgerErrorCode.INSUFFICIENT_POSTINGS,
                () -> new JournalEntry().create(List.of(posting("10.00", DebitCredit.DEBIT)), "REF"));
    }

    @Test
    void rejectsEntryWithMultipleCurrencies() {
        List<Posting> postings = List.of(
                posting("100.00", DebitCredit.DEBIT, USD),
                posting("100.00", DebitCredit.CREDIT, EUR));

        assertError(LedgerErrorCode.MULTIPLE_CURRENCIES_NOT_SUPPORTED,
                () -> new JournalEntry().create(postings, "REF"));
    }

    @Test
    void rejectsUnbalancedEntry() {
        List<Posting> postings = List.of(
                posting("100.00", DebitCredit.DEBIT),
                posting("99.00", DebitCredit.CREDIT));

        assertError(LedgerErrorCode.JOURNAL_ENTRY_NOT_BALANCED,
                () -> new JournalEntry().create(postings, "REF"));
    }

    @Test
    void postsDraftAndRecordsPostedAt() {
        JournalEntry entry = createDraft();

        entry.post(POSTED_AT);

        assertEquals(JournalEntryStatus.POSTED, entry.getStatus());
        assertEquals(POSTED_AT, entry.getPostedAt());
    }

    @Test
    void cannotPostAnAlreadyPostedEntry() {
        JournalEntry entry = createPosted();

        assertError(LedgerErrorCode.JOURNAL_ENTRY_CANNOT_BE_POSTED,
                () -> entry.post(REVERSED_AT));
    }

    @Test
    void cannotPostWithNullTime() {
        assertError(LedgerErrorCode.INVALID_PARAMS,
                () -> createDraft().post(null));
    }

    @Test
    void reversalSwapsDirectionsAndPreservesPostingValues() {
        JournalEntry original = createPosted();
        Posting originalDebit = original.getPostings().get(0);
        Posting originalCredit = original.getPostings().get(1);

        JournalEntry reversal = original.reverse(REVERSED_AT, "业务冲正");

        assertEquals(JournalEntryStatus.POSTED, reversal.getStatus());
        assertEquals(REVERSED_AT, reversal.getPostedAt());
        assertEquals(original.getJournalEntryId(), reversal.getOriginalJournalEntryId());
        assertEquals(reversal.getJournalEntryId(), original.getReversingJournalEntryId());
        assertEquals(REVERSED_AT, original.getReversedAt());
        assertNotEquals(original.getJournalEntryId(), reversal.getJournalEntryId());
        assertEquals(2, reversal.getPostings().size());

        Posting reversedDebit = reversal.getPostings().get(0);
        Posting reversedCredit = reversal.getPostings().get(1);
        assertEquals(originalDebit.getDirection().reverse(), reversedDebit.getDirection());
        assertEquals(originalCredit.getDirection().reverse(), reversedCredit.getDirection());
        assertEquals(originalDebit.getMoney(), reversedDebit.getMoney());
        assertEquals(originalCredit.getMoney(), reversedCredit.getMoney());
        assertEquals(originalDebit.getLedgerAccountId(), reversedDebit.getLedgerAccountId());
        assertEquals(originalCredit.getLedgerAccountId(), reversedCredit.getLedgerAccountId());
        assertNotEquals(originalDebit.getPostingId(), reversedDebit.getPostingId());
        assertNotEquals(originalCredit.getPostingId(), reversedCredit.getPostingId());
    }

    @Test
    void cannotReverseDraftEntry() {
        assertError(LedgerErrorCode.ONLY_POSTED_ENTRY_CAN_BE_REVERSED,
                () -> createDraft().reverse(REVERSED_AT, "业务冲正"));
    }

    @Test
    void cannotReverseTwice() {
        JournalEntry entry = createPosted();
        entry.reverse(REVERSED_AT, "第一次冲正");

        assertError(LedgerErrorCode.JOURNAL_ENTRY_ALREADY_REVERSED,
                () -> entry.reverse(REVERSED_AT, "第二次冲正"));
    }

    @Test
    void cannotReverseBeforePostedAt() {
        assertError(LedgerErrorCode.REVERSED_AT_BEFORE_POSTED_AT,
                () -> createPosted().reverse(POSTED_AT.minusSeconds(1), "业务冲正"));
    }

    @Test
    void reversalReasonCannotBeBlank() {
        assertError(LedgerErrorCode.INVALID_PARAMS,
                () -> createPosted().reverse(REVERSED_AT, "  "));
    }

    private JournalEntry createDraft() {
        return new JournalEntry().create(validPostings(), "REF-001");
    }

    private JournalEntry createPosted() {
        JournalEntry entry = createDraft();
        entry.post(POSTED_AT);
        return entry;
    }

    private List<Posting> validPostings() {
        return List.of(
                posting("100.00", DebitCredit.DEBIT),
                posting("100.00", DebitCredit.CREDIT));
    }

    private Posting posting(String amount, DebitCredit direction) {
        return posting(amount, direction, USD);
    }

    private Posting posting(String amount, DebitCredit direction, Currency currency) {
        return new Posting().create(
                LedgerAccountId.generate(),
                Money.of(new BigDecimal(amount), currency),
                direction);
    }

    private void assertError(LedgerErrorCode expected, org.junit.jupiter.api.function.Executable executable) {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, executable);
        assertEquals(expected, exception.errorCode());
    }
}
