package io.asteria.reconciliation.application.model;

import lombok.Builder;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 单个数据文件的交付描述；fileVersion 为交付版本，不等同于业务记录版本。
 */
@Builder
public record ReconciliationFileMetadata(
        /** 应用分配的文件 ID。 */ Long fileId,
        /** 对象存储相对键。 */ String objectKey,
        /** 来源系统。 */ String sourceSystem,
        /** PAYMENT_CAPTURE、LEDGER_JOURNAL 或 LEDGER_POSTING。 */ String recordType,
        /** 业务窗口起点，包含。 */ Instant windowStart,
        /** 业务窗口终点，不包含。 */ Instant windowEnd,
        /** 文件非负版本。 */ Long fileVersion,
        /** CSV 格式版本。 */ Integer schemaVersion,
        /** 不含表头的数据行数。 */ Long recordCount,
        /** 文件原始字节数。 */ Long byteSize,
        /** 原始 UTF-8 文件字节的 SHA-256，小写十六进制。 */ String sha256,
        /** 文件生成时间。 */ Instant createdAt,
        /** 所属交付的业务日期 */ LocalDate businessDate,
        /** 所属交付的采集截止时间 */ Instant cutoffAt
) { }
