package io.asteria.reconciliation.infrastructure.file;

import io.asteria.common.util.JsonUtils;
import io.asteria.reconciliation.application.model.ReconciliationFileManifest;
import org.springframework.stereotype.Component;

/**
 * 独立 UTF-8 JSON 清单编解码，复用 Common 的 Jackson 配置，不包含导入判断。
 */
@Component
public class ManifestSerializer {
    /**
     * 编码完整清单。
     */
    public byte[] serialize(ReconciliationFileManifest manifest) {
        new FileSchemaValidator().validate(manifest);
        return JsonUtils.toJsonBytes(manifest);
    }
    /**
     * 解析清单，字段语义及文件完整性由后续调用方检查。
     */
    public ReconciliationFileManifest deserialize(byte[] bytes) {
        var manifest = JsonUtils.fromJson(bytes, ReconciliationFileManifest.class);
        new FileSchemaValidator().validate(manifest);
        return manifest;
    }
}
