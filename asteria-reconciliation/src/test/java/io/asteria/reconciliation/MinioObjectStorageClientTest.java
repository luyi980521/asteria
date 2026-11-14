package io.asteria.reconciliation;

import io.asteria.reconciliation.infrastructure.file.MinioObjectStorageClient;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Item;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.time.ZonedDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SDK 适配器单元测试，不替代真实 MinIO 连通性验证。
 */
class MinioObjectStorageClientTest {
    @Test
    void delegatesUploadReadAndStatWithoutUsingEtagAsChecksum() throws Exception {
        var sdk = mock(MinioClient.class);
        var storage = new MinioObjectStorageClient(sdk, "reconciliation");
        var stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(3L);
        when(stat.lastModified()).thenReturn(ZonedDateTime.parse("2026-11-14T00:00:00Z"));
        when(stat.etag()).thenReturn("multipart-etag");
        when(sdk.statObject(any())).thenReturn(stat);
        var response = mock(GetObjectResponse.class);
        when(sdk.getObject(any())).thenReturn(response);
        var input = new ByteArrayInputStream(new byte[]{1, 2, 3});
        storage.put("a/data.csv", input);
        verify(sdk).putObject(argThatPut());
        assertSame(response, storage.get("a/data.csv"));
        assertTrue(storage.exists("a/data.csv"));
        assertEquals(3, storage.metadata("a/data.csv").byteSize());
        assertEquals("multipart-etag", storage.metadata("a/data.csv").etag());
        assertThrows(IllegalArgumentException.class, () -> storage.get("../escape"));
    }

    private PutObjectArgs argThatPut() {
        return org.mockito.ArgumentMatchers.argThat(args -> args.bucket().equals("reconciliation") && args.object().equals("a/data.csv"));
    }

    @Test
    void onlyMissingObjectsReturnFalseWhilePermissionAndNetworkErrorsPropagate() throws Exception {
        var sdk = mock(MinioClient.class);
        var storage = new MinioObjectStorageClient(sdk, "reconciliation");
        var missing = error("NoSuchKey");
        doThrow(missing).when(sdk).statObject(any());
        assertFalse(storage.exists("missing.csv"));
        doThrow(error("AccessDenied")).when(sdk).statObject(any());
        assertThrows(IOException.class, () -> storage.exists("private.csv"));
        doThrow(new IOException("offline")).when(sdk).statObject(any());
        assertThrows(IOException.class, () -> storage.metadata("data.csv"));
    }

    @Test
    void listsBoundedPagesAndExposesExclusiveNextKey() throws Exception {
        var sdk = mock(MinioClient.class);
        var storage = new MinioObjectStorageClient(sdk, "reconciliation");
        Item first = item("a/1.csv"), second = item("a/2.csv");
        when(sdk.listObjects(any())).thenReturn(List.of(new Result<>(first), new Result<>(second)));
        var page = storage.list("a/", null, 1);
        assertEquals(1, page.objects().size());
        assertEquals("a/1.csv", page.nextAfterKey());
        when(sdk.listObjects(any())).thenReturn(List.of(new Result<>(second)));
        assertNull(storage.list("a/", "a/1.csv", 1).nextAfterKey());
        assertThrows(IllegalArgumentException.class, () -> storage.list("../", null, 1));
    }

    private Item item(String key) {
        var item = mock(Item.class);
        when(item.objectName()).thenReturn(key);
        when(item.lastModified()).thenReturn(ZonedDateTime.parse("2026-11-14T00:00:00Z"));
        return item;
    }

    private ErrorResponseException error(String code) {
        return new ErrorResponseException(new ErrorResponse(code, "test failure", "reconciliation", "test.csv", "/test", "request", "host"), null, "");
    }
}
