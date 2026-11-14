package io.asteria.reconciliation.infrastructure.file;

import io.asteria.reconciliation.application.model.*;
import io.asteria.reconciliation.application.port.ObjectStorageClient;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIO 对象操作适配器；不判断交付完整性，上传成功后对象才对读取可见。
 */
public class MinioObjectStorageClient implements ObjectStorageClient {
    /** 专用 SDK 客户端，由 Spring 生命周期管理 */
    private final MinioClient client;
    /** 专用存储桶 */
    private final String bucket;

    /**
     * 创建对象存储适配器。
     * @param client SDK 客户端
     * @param bucket 存储桶名称
     */
    public MinioObjectStorageClient(MinioClient client, String bucket) {
        this.client = java.util.Objects.requireNonNull(client, "client");
        this.bucket = java.util.Objects.requireNonNull(bucket, "bucket");
    }

    @Override
    public void put(String objectKey, InputStream input) throws IOException {
        ObjectStorageKeys.validate(objectKey);
        try {
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey)
                    .stream(input, -1, 10 * 1024 * 1024).contentType("application/octet-stream").build());
        } catch (Exception exception) {
            throw storageFailure("upload", exception);
        }
    }

    @Override
    public InputStream get(String objectKey) throws IOException {
        ObjectStorageKeys.validate(objectKey);
        try {
            return client.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw storageFailure("read", exception);
        }
    }

    @Override
    public boolean exists(String objectKey) throws IOException {
        try {
            metadata(objectKey);
            return true;
        } catch (java.nio.file.NoSuchFileException exception) {
            return false;
        }
    }

    @Override
    public ObjectStorageMetadata metadata(String objectKey) throws IOException {
        ObjectStorageKeys.validate(objectKey);
        try {
            var stat = client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectKey).build());
            return new ObjectStorageMetadata(objectKey, stat.size(), stat.lastModified().toInstant(), stat.contentType(), stat.etag());
        } catch (ErrorResponseException exception) {
            if ("NoSuchKey".equals(exception.errorResponse().code()) || "NoSuchObject".equals(exception.errorResponse().code())) {
                var missing = new java.nio.file.NoSuchFileException(objectKey);
                missing.initCause(exception);
                throw missing;
            }
            throw storageFailure("stat", exception);
        } catch (Exception exception) {
            throw storageFailure("stat", exception);
        }
    }

    @Override
    public ObjectStoragePage list(String prefix, String afterKey, int limit) throws IOException {
        ObjectStorageKeys.validateList(prefix, afterKey, limit);
        List<ObjectStorageMetadata> objects = new ArrayList<>();
        try {
            var args = ListObjectsArgs.builder().bucket(bucket).prefix(prefix).recursive(true).maxKeys(limit + 1);
            if (afterKey != null) args.startAfter(afterKey);
            var iterator = client.listObjects(args.build()).iterator();
            while (iterator.hasNext() && objects.size() <= limit) {
                var item = iterator.next().get();
                objects.add(new ObjectStorageMetadata(item.objectName(), item.size(), item.lastModified().toInstant(), null, item.etag()));
            }
            boolean more = objects.size() > limit;
            if (more) objects.removeLast();
            return new ObjectStoragePage(List.copyOf(objects), more ? objects.getLast().objectKey() : null);
        } catch (Exception exception) {
            throw storageFailure("list", exception);
        }
    }

    /**
     * 保留底层异常供诊断，不在错误消息中插入密钥。
     * @param operation 存储操作名称
     * @param cause 底层失败
     * @return 统一存储异常
     */
    private IOException storageFailure(String operation, Exception cause) {
        return new IOException("Reconciliation object storage " + operation + " failed", cause);
    }
}
