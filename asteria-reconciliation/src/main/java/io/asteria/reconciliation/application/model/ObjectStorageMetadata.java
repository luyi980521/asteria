package io.asteria.reconciliation.application.model;

import java.time.Instant;

/**
 * 对象存储返回的基础元数据，不表达文件业务完整性。
 */
public record ObjectStorageMetadata(
        /** 相对对象键 */ String objectKey,
        /** 对象字节数 */ long byteSize,
        /** 最后修改时间 */ Instant lastModified,
        /** 内容类型，可空 */ String contentType,
        /** 存储 ETag，可空，不等于 SHA-256 */ String etag
) { }
