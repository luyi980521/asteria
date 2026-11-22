package io.asteria.reconciliation.application.service.impl;

import io.asteria.common.domain.exception.BusinessException;
import io.asteria.common.util.JsonUtils;
import io.asteria.reconciliation.application.model.ReconciliationFileMetadata;
import io.asteria.reconciliation.application.service.ReconciliationDeliveryBatchApplicationService;
import io.asteria.reconciliation.application.model.ReconciliationFileManifest;
import io.asteria.reconciliation.application.port.ObjectStorageClient;
import io.asteria.reconciliation.domain.entity.ReconciliationFileBatch;
import io.asteria.reconciliation.domain.repository.ReconciliationFileBatchRepository;
import io.asteria.reconciliation.infrastructure.file.ManifestSerializer;
import io.asteria.reconciliation.infrastructure.file.FileSchemaValidator;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.repository.ReconciliationDeliveryBatchRepository;
import io.asteria.reconciliation.domain.entity.ReconciliationDeliveryBatch;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 对账多文件交付批次头功能接口定义实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationDeliveryBatchApplicationServiceImpl implements ReconciliationDeliveryBatchApplicationService {

    private final ReconciliationDeliveryBatchRepository reconciliationDeliveryBatchRepository;

    private final ReconciliationFileBatchRepository reconciliationFileBatchRepository;

    /** 按现有配置选择的对象存储客户端 */
    private final ObjectStorageClient objectStorageClient;

    /** 复用现有 JSON 清单编解码 */
    private final ManifestSerializer manifestSerializer;

    /**
     * 验证交付批次
     *
     * @param batchId 批次 id
     */
    @Override
    public void validateDeliveryBatch(ReconciliationDeliveryBatchId batchId) {

        // 读取 DeliveryBatch
        Optional<ReconciliationDeliveryBatch> deliveryBatchOptional = reconciliationDeliveryBatchRepository.findById(batchId);
        if (deliveryBatchOptional.isEmpty()) {
            log.warn("ReconciliationDeliveryBatch does not exist: {}", batchId.value());
            throw new BusinessException(ReconciliationErrorCode.INVALID_PARAMS);
        }
        // 是否允许校验
        ReconciliationDeliveryBatch deliveryBatch = deliveryBatchOptional.get();
        if (ReconciliationFileBatchStatus.DELIVERED != deliveryBatch.getStatus()) {
            log.warn("File batch is not in expected status: {}, {}", batchId.value(), deliveryBatch.getStatus());
            throw new BusinessException(ReconciliationErrorCode.INVALID_STATE);
        }

        // 原子抢占校验
        boolean isUpdated = reconciliationDeliveryBatchRepository.updateStatus(
                deliveryBatch.getBatchId(),
                ReconciliationFileBatchStatus.DELIVERED,
                ReconciliationFileBatchStatus.VALIDATING
        );
        if (!isUpdated) {
            log.warn("Unexpected file batch status, skip validation: {}, {}",
                    deliveryBatch.getBatchId().value(), deliveryBatch.getStatus());
            throw new BusinessException(ReconciliationErrorCode.INVALID_STATE);
        }

        // 读取 Manifest 文件，仅反序列化，业务校验由后续步骤完成。
        ReconciliationFileManifest manifest = readManifest(deliveryBatch.getManifestObjectKey());
        //  检查全部 FileBatch 对应文件
        validateFileBatches(batchId, manifest);
        // 完成文件及交付批次状态更新 或 处理失败、隔离和重试
        isUpdated = reconciliationDeliveryBatchRepository.updateStatus(
                deliveryBatch.getBatchId(),
                ReconciliationFileBatchStatus.VALIDATING,
                ReconciliationFileBatchStatus.VALIDATED
        );
        if (!isUpdated) {
            log.warn("Unexpected file batch status, skip validation: {}", deliveryBatch.getBatchId().value());
            throw new BusinessException(ReconciliationErrorCode.INVALID_STATE);
        }
    }

    /**
     * 读取并反序列化 Manifest，自动关闭对象流，不执行业务字段校验。
     *
     * @param objectKey Manifest 对象存储键
     * @return 反序列化后的清单
     * @throws UncheckedIOException 对象不存在、存储访问或读取关闭失败，保留原始原因
     * @throws BusinessException 清单反序列化失败，保留解析原因
     */
    private ReconciliationFileManifest readManifest(String objectKey) {
        byte[] manifestBytes;
        try (var input = objectStorageClient.get(objectKey)) {
            manifestBytes = input.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read reconciliation Manifest: "
                    + objectKey, exception);
        }
        try {
            return manifestSerializer.deserializeContent(manifestBytes);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ReconciliationErrorCode.INVALID_JSON, ReconciliationErrorCode.INVALID_JSON.defaultMessage(), exception);
        }
    }

    /**
     * 分页加载并核对文件批次，返回与清单一一对应的全部登记文件。
     */
    private void validateFileBatches(ReconciliationDeliveryBatchId batchId, ReconciliationFileManifest manifest) {

        validateManifest(batchId, manifest);
        Map<String, ReconciliationFileMetadata> remaining = manifest.files().stream()
                .collect(Collectors.toMap(ReconciliationFileMetadata::objectKey, Function.identity()));

        List<ReconciliationFileBatch> loaded = new ArrayList<>();
        long afterId = 0L;
        long totalRecordCount = 0L;
        int limit = 100;

        while (true) {

            List<ReconciliationFileBatch> fileBatches = reconciliationFileBatchRepository.findByBatchId(batchId, afterId, limit);
            if (CollectionUtils.isEmpty(fileBatches)) {
                break;
            }

            for (ReconciliationFileBatch batch : fileBatches) {

                validateFileOrder(batch, afterId);
                ReconciliationFileMetadata metadata = remaining.remove(batch.getObjectKey());
                if (metadata == null) {
                    throw new BusinessException(ReconciliationErrorCode.UNEXPECTED_FILE_BATCH);
                }

                validateFileMetadata(batchId, batch, metadata);
                totalRecordCount = addRecordCount(totalRecordCount, batch.getRecordCount());
                loaded.add(batch);
                afterId = batch.getFileId().value();
            }
        }
        validateFileTotals(manifest, remaining, loaded.size(), totalRecordCount);
    }

    /**
     * 复用清单结构校验，并核对所属交付批次。
     *
     * @param batchId 交付批次标识
     * @param manifest 交付清单
     * @throws BusinessException 清单结构或批次标识不一致
     */
    private void validateManifest(ReconciliationDeliveryBatchId batchId, ReconciliationFileManifest manifest) {

        try {
            new FileSchemaValidator().validate(manifest);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ReconciliationErrorCode.INVALID_MANIFEST,
                    ReconciliationErrorCode.INVALID_MANIFEST.defaultMessage(), exception);
        }
        if (batchId == null || !Objects.equals(batchId.value(), manifest.deliveryId())) {
            throw new BusinessException(ReconciliationErrorCode.MANIFEST_DELIVERY_MISMATCH);
        }
    }

    /**
     * 确保文件 ID 严格递增，拒绝重复行及未推进的分页。
     *
     * @param batch 文件批次
     * @param afterId 上一个文件 ID
     * @throws BusinessException 文件缺失或分页顺序非法
     */
    private void validateFileOrder(ReconciliationFileBatch batch, long afterId) {

        if (batch == null || batch.getFileId().value() <= afterId) {
            throw new BusinessException(ReconciliationErrorCode.INVALID_FILE_BATCH_PAGE);
        }
    }

    /**
     * 核对登记文件的身份、所属批次及不可变描述，不比较处理状态和登记时间。
     *
     * @param batchId 交付批次标识
     * @param batch 登记文件批次
     * @param metadata 清单文件描述
     * @throws BusinessException 文件身份或描述不一致
     */
    private void validateFileMetadata(ReconciliationDeliveryBatchId batchId, ReconciliationFileBatch batch,
                                      ReconciliationFileMetadata metadata) {

        if (!Objects.equals(batchId, batch.getBatchId())
                || !Objects.equals(metadata.fileId(), batch.getFileId().value())
                || !Objects.equals(metadata.sourceSystem(), batch.getSourceSystem())
                || !Objects.equals(metadata.recordType(), batch.getRecordType())
                || !Objects.equals(metadata.fileVersion(), batch.getFileVersion())
                || !Objects.equals(metadata.schemaVersion(), batch.getSchemaVersion())
                || !Objects.equals(metadata.businessDate(), batch.getBusinessDate())
                || !Objects.equals(metadata.windowStart(), batch.getWindowStart())
                || !Objects.equals(metadata.windowEnd(), batch.getWindowEnd())
                || !Objects.equals(metadata.cutoffAt(), batch.getCutoffAt())
                || !Objects.equals(metadata.sha256(), batch.getSha256())
                || !Objects.equals(metadata.byteSize(), batch.getByteSize())
                || !Objects.equals(metadata.recordCount(), batch.getRecordCount())) {
            log.error("File batch metadata differs from manifest: {}, {}",
                    JsonUtils.toJson(batch), JsonUtils.toJson(metadata));
            throw new BusinessException(ReconciliationErrorCode.FILE_BATCH_METADATA_MISMATCH);
        }
    }

    /**
     * 安全累加文件预期记录数。
     *
     * @param total 当前累计记录数
     * @param count 本文件记录数
     * @return 累加后的记录数
     * @throws BusinessException 记录数溢出，保留原始原因
     */
    private long addRecordCount(long total, long count) {

        try {
            return Math.addExact(total, count);
        } catch (ArithmeticException exception) {
            throw new BusinessException(ReconciliationErrorCode.FILE_RECORD_COUNT_OVERFLOW,
                    ReconciliationErrorCode.FILE_RECORD_COUNT_OVERFLOW.defaultMessage(), exception);
        }
    }

    /**
     * 核对清单文件覆盖范围、文件数及预期记录总数。
     *
     * @param manifest 交付清单
     * @param remaining 未匹配的文件描述
     * @param fileCount 已加载文件数
     * @param recordCount 已加载预期记录数
     * @throws BusinessException 文件覆盖或汇总数量不一致
     */
    private void validateFileTotals(ReconciliationFileManifest manifest,
                                    Map<String, ReconciliationFileMetadata> remaining,
                                    long fileCount, long recordCount) {

        if (!remaining.isEmpty() || fileCount != manifest.files().size() || recordCount != manifest.recordCount()) {
            throw new BusinessException(ReconciliationErrorCode.FILE_BATCH_TOTAL_MISMATCH);
        }
    }
}