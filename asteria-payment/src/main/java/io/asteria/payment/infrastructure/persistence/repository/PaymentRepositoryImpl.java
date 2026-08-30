package io.asteria.payment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
import io.asteria.payment.domain.repository.PaymentRepository;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.infrastructure.persistence.converter.PaymentPersistenceConverter;
import io.asteria.payment.infrastructure.persistence.dataobject.PaymentDO;
import io.asteria.payment.infrastructure.persistence.mapper.PaymentMapper;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Date;

/** Payment 仓储实现 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentMapper paymentMapper;
    private final PaymentPersistenceConverter converter;

    @Override
    public void insert(Payment payment) {
        log.info("Inserting payment, paymentId={}, referenceType={}, referenceId={}",
                payment.getPaymentId().value(), payment.getReference().referenceType(),
                payment.getReference().referenceId());
        paymentMapper.insert(converter.toDataObject(payment));
    }

    @Override
    public void update(Payment payment) {
        Long paymentId = payment.getPaymentId().value();
        PaymentDO existing = paymentMapper.selectById(paymentId);
        if (existing == null) {
            log.warn("Payment does not exist: {}", paymentId);
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        PaymentDO dataObject = converter.toDataObject(payment);
        LambdaUpdateWrapper<PaymentDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(PaymentDO::getId, paymentId)
                .eq(PaymentDO::getVersion, existing.getVersion())
                .set(PaymentDO::getStatus, dataObject.getStatus())
                .set(PaymentDO::getAuthorizationTransactionId,
                        dataObject.getAuthorizationTransactionId())
                .set(PaymentDO::getAuthorizedAt, dataObject.getAuthorizedAt())
                .set(PaymentDO::getCapturedAt, dataObject.getCapturedAt())
                .set(PaymentDO::getUpdatedAt, new Date())
                .set(PaymentDO::getVersion, existing.getVersion() + 1);

        log.info("Updating payment, paymentId={}, status={}, version={}",
                paymentId, dataObject.getStatus(), existing.getVersion());
        if (paymentMapper.update(new PaymentDO(), updateWrapper) == 0) {
            log.warn("Payment update did not affect a row: {}", paymentId);
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
    }

    @Override
    public Payment findById(PaymentId paymentId) {
        PaymentDO dataObject = paymentMapper.selectById(paymentId.value());
        if (dataObject == null) {
            log.warn("Payment does not exist: {}", paymentId.value());
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        log.info("Found payment by id: {}", paymentId.value());
        return converter.toDomain(dataObject);
    }

    @Override
    public Payment findByReference(PaymentReference reference) {
        PaymentDO dataObject = paymentMapper.selectOne(referenceWrapper(reference));
        if (dataObject == null) {
            log.warn("Payment does not exist for reference type={}, referenceId={}",
                    reference.referenceType(), reference.referenceId());
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        }
        log.info("Found payment by reference, referenceType={}, referenceId={}",
                reference.referenceType(), reference.referenceId());
        return converter.toDomain(dataObject);
    }

    @Override
    public boolean existsByReference(PaymentReference reference) {
        boolean exists = paymentMapper.selectCount(referenceWrapper(reference)) > 0;
        log.info("Checked payment reference, referenceType={}, referenceId={}, exists={}",
                reference.referenceType(), reference.referenceId(), exists);
        return exists;
    }

    private static LambdaQueryWrapper<PaymentDO> referenceWrapper(PaymentReference reference) {
        return new LambdaQueryWrapper<PaymentDO>()
                .eq(PaymentDO::getReferenceType, reference.referenceType())
                .eq(PaymentDO::getReferenceId, reference.referenceId());
    }
}
