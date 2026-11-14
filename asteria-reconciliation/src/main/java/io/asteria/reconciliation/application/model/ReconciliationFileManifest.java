package io.asteria.reconciliation.application.model;

import lombok.Builder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 一次交付的文件清单；调用方应先发布数据文件，最后发布独立 JSON 清单。
 */
@Builder
public record ReconciliationFileManifest(
        /** 清单格式版本。 */ Integer schemaVersion,
        /** 应用分配的交付批次 ID。 */ Long deliveryId,
        /** 清单生成时间。 */ Instant createdAt,
        /** 交付来源系统 */ String sourceSystem,
        /** 来源系统约定的业务日期，不从 UTC 时间擅自推导 */ LocalDate businessDate,
        /** 包含的业务窗口起点 */ Instant windowStart,
        /** 不包含的业务窗口终点 */ Instant windowEnd,
        /** 来源采集可见截止时间 */ Instant cutoffAt,
        /** 交付版本，不代表来源业务记录版本 */ Long fileVersion,
        /** 所有数据文件预期记录数之和，不含表头 */ Long recordCount,
        /** 该交付的所有文件，Ledger 头与分录分别列出。 */ List<ReconciliationFileMetadata> files
) { }
