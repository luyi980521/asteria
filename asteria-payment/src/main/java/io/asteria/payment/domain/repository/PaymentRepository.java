package io.asteria.payment.domain.repository;

import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentReference;

public interface PaymentRepository {

    void insert(Payment payment);

    void update(Payment payment);

    Payment findById(PaymentId paymentId);

    Payment findByReference(PaymentReference reference);

    boolean existsByReference(PaymentReference reference);
}
