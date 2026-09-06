package io.asteria.ledger.domain.entity;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.enums.JournalEntryStatus;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.EventId;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import io.asteria.ledger.domain.valueobject.JournalReference;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.ledger.domain.valueobject.PostingId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalEntryTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode EUR = CurrencyCode.of("EUR");
    private static final Instant POSTED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant REVERSED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void createsBalancedEntryAsDraft() {
        JournalEntry entry = JournalEntry.create(
                JournalEntryId.of(1001L), validPostings(), reference("REF-001"));

        assertEquals(JournalEntryStatus.DRAFT, entry.getStatus());
        assertNull(entry.getPostedAt());
        assertEquals(1001L, entry.getJournalEntryId().value());
        assertEquals(2, entry.getPostings().size());
    }

    @Test
    void rejectsEntryWithNullId() {
        assertError(LedgerErrorCode.NULL_ARGUMENT,
                () -> JournalEntry.create(null, validPostings(), reference("REF")));
    }

    @Test
    void rejectsEntryWithFewerThanTwoPostings() {
        assertError(LedgerErrorCode.INSUFFICIENT_POSTINGS,
                () -> JournalEntry.create(
                        JournalEntryId.of(1001L),
                        List.of(posting(2001L, 3001L, "10.00", DebitCredit.DEBIT)),
                        reference("REF")));
    }

    @Test
    void rejectsEntryWithMultipleCurrencies() {
        List<Posting> postings = List.of(
                posting(2001L, 3001L, "100.00", DebitCredit.DEBIT, USD),
                posting(2002L, 3002L, "100.00", DebitCredit.CREDIT, EUR));

        assertError(LedgerErrorCode.MULTIPLE_CURRENCIES_NOT_SUPPORTED,
                () -> JournalEntry.create(JournalEntryId.of(1001L), postings, reference("REF")));
    }

    @Test
    void rejectsUnbalancedEntry() {
        List<Posting> postings = List.of(
                posting(2001L, 3001L, "100.00", DebitCredit.DEBIT),
                posting(2002L, 3002L, "99.00", DebitCredit.CREDIT));

        assertError(LedgerErrorCode.JOURNAL_ENTRY_NOT_BALANCED,
                () -> JournalEntry.create(JournalEntryId.of(1001L), postings, reference("REF")));
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
    void reversalUsesApplicationGeneratedIds() {
        JournalEntry original = createPosted();
        Posting originalDebit = original.getPostings().get(0);
        Posting originalCredit = original.getPostings().get(1);

        JournalEntry reversal = original.reverse(
                JournalEntryId.of(1002L),
                List.of(PostingId.of(2003L), PostingId.of(2004L)),
                REVERSED_AT,
                "reversal");

        assertEquals(JournalEntryStatus.POSTED, reversal.getStatus());
        assertEquals(1002L, reversal.getJournalEntryId().value());
        assertEquals(original.getJournalEntryId(), reversal.getOriginalJournalEntryId());
        assertEquals(reversal.getJournalEntryId(), original.getReversingJournalEntryId());
        assertEquals(REVERSED_AT, original.getReversedAt());
        assertNotEquals(original.getJournalEntryId(), reversal.getJournalEntryId());

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
                () -> createDraft().reverse(
                        JournalEntryId.of(1002L), reversalPostingIds(), REVERSED_AT, "reversal"));
    }

    @Test
    void cannotReverseTwice() {
        JournalEntry entry = createPosted();
        entry.reverse(JournalEntryId.of(1002L), reversalPostingIds(), REVERSED_AT, "first");

        assertError(LedgerErrorCode.JOURNAL_ENTRY_ALREADY_REVERSED,
                () -> entry.reverse(JournalEntryId.of(1003L),
                        List.of(PostingId.of(2005L), PostingId.of(2006L)),
                        REVERSED_AT, "second"));
    }

    @Test
    void cannotReverseBeforePostedAt() {
        assertError(LedgerErrorCode.REVERSED_AT_BEFORE_POSTED_AT,
                () -> createPosted().reverse(
                        JournalEntryId.of(1002L), reversalPostingIds(),
                        POSTED_AT.minusSeconds(1), "reversal"));
    }

    @Test
    void reversalReasonCannotBeBlank() {
        assertError(LedgerErrorCode.INVALID_PARAMS,
                () -> createPosted().reverse(
                        JournalEntryId.of(1002L), reversalPostingIds(), REVERSED_AT, "  "));
    }

    private JournalEntry createDraft() {
        return JournalEntry.create(JournalEntryId.of(1001L), validPostings(), reference("REF-001"));
    }

    private JournalEntry createPosted() {
        JournalEntry entry = createDraft();
        entry.post(POSTED_AT);
        return entry;
    }

    private List<Posting> validPostings() {
        return List.of(
                posting(2001L, 3001L, "100.00", DebitCredit.DEBIT),
                posting(2002L, 3002L, "100.00", DebitCredit.CREDIT));
    }

    private List<PostingId> reversalPostingIds() {
        return List.of(PostingId.of(2003L), PostingId.of(2004L));
    }

    private Posting posting(Long postingId, Long ledgerAccountId,
                            String amount, DebitCredit direction) {
        return posting(postingId, ledgerAccountId, amount, direction, USD);
    }

    private Posting posting(Long postingId, Long ledgerAccountId,
                            String amount, DebitCredit direction, CurrencyCode currency) {
        return Posting.create(
                PostingId.of(postingId),
                LedgerAccountId.of(ledgerAccountId),
                Money.of(new BigDecimal(amount), currency),
                direction);
    }

    private JournalReference reference(String sourceId) {
        return new JournalReference("TEST", sourceId, "TEST_CREATED", EventId.generate().value());
    }

    private void assertError(LedgerErrorCode expected,
                             org.junit.jupiter.api.function.Executable executable) {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, executable);
        assertEquals(expected, exception.errorCode());
    }
}
