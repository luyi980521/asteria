package io.asteria.reconciliation.infrastructure.config;

import io.asteria.reconciliation.application.port.ObjectStorageClient;
import io.asteria.reconciliation.infrastructure.file.MinioObjectStorageClient;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 按显式 provider 选择远程存储；local 实现保持可独立测试。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "asteria.reconciliation.storage.provider", havingValue = "minio")
@EnableConfigurationProperties(ReconciliationMinioProperties.class)
public class ReconciliationStorageConfiguration {
    /**
     * 创建专用 SDK 客户端，缺少配置时立即失败，不使用匿名访问。
     * @param properties 远程存储配置
     * @return MinIO 客户端
     */
    @Bean(name = "reconciliationMinioClient", destroyMethod = "close")
    public MinioClient reconciliationMinioClient(ReconciliationMinioProperties properties) {
        for (String required : new String[]{properties.getEndpoint(), properties.getAccessKey(), properties.getSecretKey(), properties.getBucket()}) {
            if (required == null || required.isBlank()) throw new IllegalArgumentException("Reconciliation MinIO configuration is incomplete");
        }
        MinioClient client = MinioClient.builder().endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey()).build();
        client.setTimeout(10000, 60000, 60000);
        return client;
    }

    /**
     * 创建远程对象适配器，仅在显式启用时初始化桶。
     * @param client Reconciliation 专用 SDK 客户端
     * @param properties 远程存储配置
     * @return 对象存储端口
     * @throws Exception 桶初始化或访问失败
     */
    @Bean
    public ObjectStorageClient reconciliationObjectStorageClient(
            @org.springframework.beans.factory.annotation.Qualifier("reconciliationMinioClient") MinioClient client,
            ReconciliationMinioProperties properties) throws Exception {
        if (properties.isInitializeBucket() && !client.bucketExists(BucketExistsArgs.builder().bucket(properties.getBucket()).build())) {
            try {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            } catch (ErrorResponseException exception) {
                if (!"BucketAlreadyOwnedByYou".equals(exception.errorResponse().code())) throw exception;
            }
        }
        return new MinioObjectStorageClient(client, properties.getBucket());
    }
}
