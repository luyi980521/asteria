package io.asteria.ledger.application.request;

import lombok.Builder;
import java.util.List;

@Builder
public record ReverseJournalEntriesRequest(List<String> eventIds, String reason) {
}
