package io.asteria.reconciliation.application.model;

import java.util.List;

/**
 * 有界的对象列表结果；遍历多页期间的快照一致性由调用方负责。
 */
public record ObjectStoragePage(
        /** 本页对象 */ List<ObjectStorageMetadata> objects,
        /** 下一页排他起点，末页为空 */ String nextAfterKey
) { }
