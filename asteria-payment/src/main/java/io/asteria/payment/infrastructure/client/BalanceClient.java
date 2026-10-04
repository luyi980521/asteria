package io.asteria.payment.infrastructure.client;

import io.asteria.balance.api.request.ConsumeBalanceRequest;
import io.asteria.balance.api.request.ReleaseBalanceRequest;
import io.asteria.balance.api.request.ReserveBalanceRequest;
import io.asteria.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "balance-service", url = "${asteria.service.balance.url}")
public interface BalanceClient {

    @PostMapping("/internal/balance/reserve")
    ApiResponse<Void> reserve(@RequestBody ReserveBalanceRequest request);

    @PostMapping("/internal/balance/release")
    ApiResponse<Void> release(@RequestBody ReleaseBalanceRequest request);

    @PostMapping("/internal/balance/consume")
    ApiResponse<Void> consume(@RequestBody ConsumeBalanceRequest request);
}
