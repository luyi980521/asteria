package io.asteria.reconciliation.application.service.impl;

import io.asteria.reconciliation.application.service.ReconciliationDeliveryBatchApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 对账多文件交付批次头功能接口定义实现类
 */
@Slf4j
@Service
public class ReconciliationDeliveryBatchApplicationServiceImpl implements ReconciliationDeliveryBatchApplicationService {

    /**
     * 验证交付批次
     *
     * @param batchId 批次 id
     */
    @Override
    public void validateDeliveryBatch(Long batchId) {

        // 读取 DeliveryBatch

        // 是否允许校验

        // 读取 MinIO 校验 Manifest 文件

        //  检查全部 FileBatch 对应文件

        // 完成文件及交付批次状态更新 或 处理失败、隔离和重试
    }
}
