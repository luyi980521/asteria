package io.asteria.reconciliation;

import io.asteria.reconciliation.infrastructure.file.*;
import io.minio.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 显式启用的真实 MinIO 测试，只删除本测试创建的唯一前缀对象，不删除存储桶。
 */
@EnabledIfEnvironmentVariable(named = "ASTERIA_MINIO_INTEGRATION_TEST", matches = "true")
class MinioObjectStorageIntegrationTest {
    @Test
    void realMinioUploadDownloadStatListAndChecksum() throws Exception {
        String bucket = System.getenv("ASTERIA_RECONCILIATION_MINIO_BUCKET");
        if (bucket == null || bucket.isBlank()) bucket = "asteria-reconciliation";
        try (var sdk = MinioClient.builder().endpoint(System.getenv("ASTERIA_RECONCILIATION_MINIO_ENDPOINT"))
                .credentials(System.getenv("ASTERIA_RECONCILIATION_MINIO_ACCESS_KEY"), System.getenv("ASTERIA_RECONCILIATION_MINIO_SECRET_KEY")).build()) {
            var storage = new MinioObjectStorageClient(sdk, bucket);
            String key = "integration-tests/" + java.util.UUID.randomUUID() + "/test.csv";
            byte[] bytes = "\"中文,引号\"\r\n".getBytes(StandardCharsets.UTF_8);
            try {
                assertFalse(storage.exists(key));
                storage.put(key, new ByteArrayInputStream(bytes));
                assertTrue(storage.exists(key));
                assertEquals(bytes.length, storage.metadata(key).byteSize());
                try (var stream = storage.get(key)) { FileChecksumUtils.verify(stream, FileChecksumUtils.sha256(new ByteArrayInputStream(bytes))); }
                assertEquals(key, storage.list(key, null, 1).objects().getFirst().objectKey());
            } finally {
                sdk.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
            }
        }
    }
}
