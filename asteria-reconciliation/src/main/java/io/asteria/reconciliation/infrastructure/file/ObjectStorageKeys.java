package io.asteria.reconciliation.infrastructure.file;

/**
 * 本地与远程实现共用的对象键语法约束，不执行文件业务判断。
 */
final class ObjectStorageKeys {
    private ObjectStorageKeys() { }

    /**
     * 校验对象键，禁止绝对路径、穿越、设备名与内部临时对象名。
     * @param key 相对对象键
     */
    static void validate(String key) {
        if (key == null || key.isBlank() || key.contains("\\") || key.contains(":") || key.startsWith("/")) {
            throw new IllegalArgumentException("Object key must be a relative / separated path");
        }
        for (String part : key.split("/", -1)) {
            if (part.isBlank() || part.equals(".") || part.equals("..") || part.startsWith(".upload-")
                    || part.endsWith(".") || part.endsWith(" ") || part.chars().anyMatch(c -> c < 32 || "<>\"|?*".indexOf(c) >= 0)
                    || part.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?")) {
                throw new IllegalArgumentException("Illegal object key component");
            }
        }
    }

    /**
     * 校验列表参数，前缀允许空值或尾部斜线。
     * @param prefix 对象前缀
     * @param afterKey 上页起点
     * @param limit 页大小
     */
    static void validateList(String prefix, String afterKey, int limit) {
        if (prefix == null || limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid object list page");
        if (!prefix.isEmpty()) validate(prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix);
        if (afterKey != null) validate(afterKey);
    }
}
