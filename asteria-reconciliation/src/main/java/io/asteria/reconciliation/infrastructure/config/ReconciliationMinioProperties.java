package io.asteria.reconciliation.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reconciliation 专用 MinIO 配置，不读取或覆盖其他模块的 S3 配置。
 */
@Getter
@Setter
@ConfigurationProperties("asteria.reconciliation.storage.minio")
public class ReconciliationMinioProperties {
    /** S3 API 端点，生产环境通过配置提供 HTTPS 地址 */
    private String endpoint;
    /** 访问标识，不参与日志输出 */
    private String accessKey;
    /** 访问密钥，不参与日志输出 */
    private String secretKey;
    /** Reconciliation 专用存储桶 */
    private String bucket;
    /** 是否在初始化客户端时创建缺失的桶，生产默认关闭 */
    private boolean initializeBucket;
}
