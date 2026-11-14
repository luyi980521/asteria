package io.asteria.reconciliation.infrastructure.file;

import io.asteria.reconciliation.application.port.ObjectStorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.asteria.reconciliation.application.model.ObjectStorageMetadata;
import io.asteria.reconciliation.application.model.ObjectStoragePage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * 本地对象存储模拟；根目录及其内容须由本进程专用，禁止外部进程并发替换目录。
 */
@Component
@ConditionalOnProperty(name = "asteria.reconciliation.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageClient implements ObjectStorageClient {
    /** 对象根目录，启动时解析真实路径。 */
    private final Path root;

    public LocalObjectStorageClient(
            @Value("${asteria.reconciliation.storage.local-directory:./data/reconciliation-objects}") String directory) throws IOException {
        Path configured = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(configured);
        root = configured.toRealPath();
    }

    @Override
    public void put(String objectKey, InputStream input) throws IOException {
        Path target = resolve(objectKey);
        Files.createDirectories(target.getParent());
        // 再次检查新建目录及已有符号链接，禁止越过对象根目录。
        target = resolve(objectKey);
        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            // 不降级为普通 move；不支持原子移动时失败，旧对象保持可读。
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public InputStream get(String objectKey) throws IOException {
        return Files.newInputStream(resolve(objectKey), LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public boolean exists(String objectKey) throws IOException {
        return Files.isRegularFile(resolve(objectKey), LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public ObjectStorageMetadata metadata(String objectKey) throws IOException {
        Path path = resolve(objectKey);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new NoSuchFileException(objectKey);
        return new ObjectStorageMetadata(objectKey, Files.size(path), Files.getLastModifiedTime(path).toInstant(),
                Files.probeContentType(path), null);
    }

    @Override
    public ObjectStoragePage list(String prefix, String afterKey, int limit) throws IOException {
        ObjectStorageKeys.validateList(prefix, afterKey, limit);
        // 仅本地模拟使用目录扫描；使用有界 TreeSet，避免整个对象目录驻留内存。
        var keys = new java.util.TreeSet<String>();
        try (var paths = Files.walk(root)) {
            var iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                String key = root.relativize(path).toString().replace('\\', '/');
                if (java.util.Arrays.stream(key.split("/")).anyMatch(part -> part.startsWith(".upload-"))) continue;
                if (key.startsWith(prefix) && (afterKey == null || key.compareTo(afterKey) > 0)) {
                    keys.add(key);
                    if (keys.size() > limit + 1) keys.pollLast();
                }
            }
        }
        boolean more = keys.size() > limit;
        if (more) keys.pollLast();
        var result = new java.util.ArrayList<ObjectStorageMetadata>();
        for (String key : keys) result.add(metadata(key));
        return new ObjectStoragePage(java.util.List.copyOf(result), more ? keys.last() : null);
    }

    private Path resolve(String key) throws IOException {
        ObjectStorageKeys.validate(key);
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IllegalArgumentException("Object key escapes storage root");
        }
        Path current = root;
        for (Path part : root.relativize(target)) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)
                    || (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && !current.toRealPath().startsWith(root))) {
                throw new IOException("Object path contains an unsafe link");
            }
        }
        return target;
    }
}
