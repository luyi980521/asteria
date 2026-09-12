package io.asteria.payment.infrastructure.client;

import io.asteria.payment.application.request.ReversePaymentCapturesRequest;
import io.asteria.payment.application.response.ReversePaymentCapturesResponse;
import io.asteria.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ledger-service", url = "${asteria.service.ledger.url}")
public interface LedgerClient {

    @PostMapping("/internal/ledger/journal-entries/reversals")
    ApiResponse<ReversePaymentCapturesResponse> reverseJournalEntries(@RequestBody ReversePaymentCapturesRequest request);
}
