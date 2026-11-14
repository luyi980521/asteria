package io.asteria.reconciliation.infrastructure.file;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 原始文件字节的 SHA-256 工具；不关闭调用方流，不转换换行或字符集。
 */
public final class FileChecksumUtils {
    private FileChecksumUtils() { }

    /**
     * 流式计算小写十六进制摘要。
     */
    public static String sha256(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM must support SHA-256", exception);
        }
    }

    /**
     * 验证摘要，拒绝格式错误及内容不一致。
     */
    public static void verify(InputStream input, String expected) throws IOException {
        if (expected == null || !expected.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Expected lowercase SHA-256 hex digest");
        }
        if (!MessageDigest.isEqual(HexFormat.of().parseHex(expected), HexFormat.of().parseHex(sha256(input)))) {
            throw new IOException("File SHA-256 mismatch");
        }
    }
}
