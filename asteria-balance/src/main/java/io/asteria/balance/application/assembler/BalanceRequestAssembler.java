package io.asteria.balance.application.assembler;

import io.asteria.balance.api.request.ConsumeBalanceRequest;
import io.asteria.balance.api.request.ReleaseBalanceRequest;
import io.asteria.balance.api.request.ReserveBalanceRequest;
import io.asteria.balance.application.command.ConsumeBalanceCommand;
import io.asteria.balance.application.command.ReleaseBalanceCommand;
import io.asteria.balance.application.command.ReserveBalanceCommand;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceAccountRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceRequestAssembler {

    private final BalanceAccountRepository balanceAccountRepository;

    public ReserveBalanceCommand toCommand(ReserveBalanceRequest request) {
        BalanceAccountId accountId = resolveAccount(request.merchantId(), request.referenceType(),
                request.referenceId(), request.eventId());
        CurrencyCode currency = CurrencyCode.of(request.currency());
        return ReserveBalanceCommand.builder()
                .balanceAccountId(accountId)
                .currency(currency)
                .amount(Money.of(request.amount(), currency))
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .eventId(request.eventId().toString())
                .build();
    }

    public ReleaseBalanceCommand toCommand(ReleaseBalanceRequest request) {
        return ReleaseBalanceCommand.builder()
                .balanceAccountId(resolveAccount(request.merchantId(), request.referenceType(),
                        request.referenceId(), request.eventId()))
                .currency(CurrencyCode.of(request.currency()))
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .eventId(request.eventId().toString())
                .build();
    }

    public ConsumeBalanceCommand toCommand(ConsumeBalanceRequest request) {
        return ConsumeBalanceCommand.builder()
                .balanceAccountId(resolveAccount(request.merchantId(), request.referenceType(),
                        request.referenceId(), request.eventId()))
                .currency(CurrencyCode.of(request.currency()))
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .eventId(request.eventId().toString())
                .build();
    }

    private BalanceAccountId resolveAccount(Long merchantId, String referenceType, String referenceId, Long eventId) {
        if (merchantId == null || merchantId <= 0 || eventId == null || eventId <= 0
                || StringUtils.isBlank(referenceType) || StringUtils.isBlank(referenceId)) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        return balanceAccountRepository.findByOwner(BalanceAccountOwnerType.MERCHANT, merchantId)
                .orElseThrow(() -> new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS))
                .getBalanceAccountId();
    }
}
