package io.asteria.ledger.application.response;

import io.asteria.ledger.domain.enums.JournalEntryReverseStatus;
import lombok.Builder;
import java.util.List;

@Builder
public record ReverseJournalEntriesResponse(List<Result> results) {

    @Builder
    public record Result(String eventId, JournalEntryReverseStatus status, String code, String message) {
    }
}
