package io.asteria.payment.infrastructure.persistence.converter;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.enums.PaymentMethod;
import io.asteria.payment.domain.enums.PaymentStatus;
import io.asteria.payment.infrastructure.persistence.dataobject.PaymentDO;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentReference;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Date;

/** Payment 领域对象与持久化对象转换器 */
@Component
public class PaymentPersistenceConverter {

    public PaymentDO toDataObject(Payment payment) {
        PaymentDO dataObject = new PaymentDO();
        dataObject.setId(payment.getPaymentId().value());
        dataObject.setMerchantId(payment.getMerchantId());
        dataObject.setAmount(payment.getAmount().amount());
        dataObject.setCurrency(payment.getAmount().currency().getCurrencyCode());
        dataObject.setPaymentMethod(payment.getPaymentMethod().name());
        dataObject.setReferenceType(payment.getReference().referenceType());
        dataObject.setReferenceId(payment.getReference().referenceId());
        dataObject.setStatus(payment.getStatus().name());
        dataObject.setCreatedAt(Date.from(payment.getCreatedAt()));
        dataObject.setAuthorizedAt(toDate(payment.getAuthorizedAt()));
        dataObject.setCapturedAt(toDate(payment.getCapturedAt()));
        return dataObject;
    }

    public Payment toDomain(PaymentDO dataObject) {
        return Payment.reconstitute(
                PaymentId.of(dataObject.getId()),
                dataObject.getMerchantId(),
                Money.of(dataObject.getAmount(), Currency.getInstance(dataObject.getCurrency())),
                PaymentMethod.valueOf(dataObject.getPaymentMethod()),
                new PaymentReference(dataObject.getReferenceType(), dataObject.getReferenceId()),
                PaymentStatus.valueOf(dataObject.getStatus()),
                dataObject.getCreatedAt().toInstant(),
                toInstant(dataObject.getAuthorizedAt()),
                toInstant(dataObject.getCapturedAt())
        );
    }

    private static Date toDate(java.time.Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    private static java.time.Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
