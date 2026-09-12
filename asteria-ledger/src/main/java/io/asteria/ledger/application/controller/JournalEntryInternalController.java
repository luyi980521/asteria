package io.asteria.ledger.application.controller;

import io.asteria.ledger.application.request.ReverseJournalEntriesRequest;
import io.asteria.ledger.application.response.ReverseJournalEntriesResponse;
import io.asteria.ledger.application.command.ReverseJournalEntriesCommand;
import io.asteria.ledger.application.service.JournalEntryApplicationService;
import io.asteria.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/ledger/journal-entries")
public class JournalEntryInternalController {

    private final JournalEntryApplicationService journalEntryApplicationService;

    @PostMapping("/reversals")
    public ApiResponse<ReverseJournalEntriesResponse> reverse(@RequestBody ReverseJournalEntriesRequest request) {
        log.info("Received journal entry reversal request: {}", request.eventIds().size());
        ReverseJournalEntriesCommand command = new ReverseJournalEntriesCommand(request.eventIds(), request.reason());
        return ApiResponse.success(journalEntryApplicationService.reverseByEventIds(command));
    }
}
