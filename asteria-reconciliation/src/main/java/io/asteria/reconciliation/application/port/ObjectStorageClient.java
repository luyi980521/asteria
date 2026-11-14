package io.asteria.reconciliation.application.port;

import java.io.IOException;
import java.io.InputStream;
import io.asteria.reconciliation.application.model.ObjectStorageMetadata;
import io.asteria.reconciliation.application.model.ObjectStoragePage;

/**
 * 文件对象存储端口；键为使用 / 分隔的相对路径，调用方负责关闭读取流。
 */
public interface ObjectStorageClient {
    /**
     * 同步复制并原子发布；不关闭调用方的输入流。
     */
    void put(String objectKey, InputStream input) throws IOException;
    /**
     * 打开已发布对象；不存在时抛出 IOException。
     */
    InputStream get(String objectKey) throws IOException;
    /**
     * 检查对象是否存在。
     */
    boolean exists(String objectKey) throws IOException;

    /**
     * 读取对象元数据；ETag 为存储标识，不能用作 SHA-256。
     * @param objectKey 对象相对键
     * @return 对象描述
     * @throws IOException 对象不存在或存储访问失败
     */
    ObjectStorageMetadata metadata(String objectKey) throws IOException;

    /**
     * 按前缀列出一页对象，按键升序；afterKey 为上一页最后的键，首屏为 null。
     * @param prefix 对象键前缀，空字符串表示全部
     * @param afterKey 排他起点
     * @param limit 页大小，1..1000
     * @return 对象及下一页起点，末页起点为空
     * @throws IOException 存储访问失败
     */
    ObjectStoragePage list(String prefix, String afterKey, int limit) throws IOException;
}
