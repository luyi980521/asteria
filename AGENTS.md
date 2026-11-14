# Asteria 开发约定

## 职责划分

- Codex 负责实体、DTO、DO、Converter、Mapper、Repository、基础工具、配置和基础设施。
- 核心业务 Service、导出与导入编排、Normalizer、Matcher、规则、差异分类、Case 业务、重试恢复及自动修复/冲正由开发者亲自实现。
- 不生成空壳 Service、大量 TODO、未经请求的 Consumer、Controller 或定时任务。
- 保留工作区已有修改，不执行 git reset、git clean 或覆盖现有业务数据的操作。

## 永久 JavaDoc 规范

- 类、接口、枚举、record、构造器和方法的 JavaDoc 必须使用真实换行的标准多行格式：

  ```java
  /**
   * 业务说明。
   *
   * @param value 参数说明
   * @return 返回值说明
   * @throws IllegalArgumentException 参数无效
   */
  ```

- 属性字段、record 组件和枚举常量的 JavaDoc 使用单行格式：`/** 对账批次标识 */`。
  仅对确实需要多行解释的代码示例、HTML 布局、标准块标签等保留多行。
- 按实际语义添加 @param、@return、@throws 等标签；保留 @see、{@link ...}、代码片段与 HTML。
- 不把普通 // 解释机械转为 JavaDoc，不为缺少业务含义的字段编造说明。
- 格式化只改注释范围，不为注释修改业务逻辑、方法签名、字符串或 SQL。
- 不处理第三方、生成代码、target、外部依赖或缓存目录。
- 可使用 `tools/javadoc/JavadocFormatter.java` 进行 AST 驱动的预检；批量写入前后核对其非注释内容摘要和备份。

## 模型和持久化

- 优先遵循已有 DDD 包结构、Repository 端口、Lombok、Builder、显式 Converter、Money/CurrencyCode、Instant 及异常规范。
- ID 使用已有 DistributedIdGenerator；不得另造业务主键生成机制。
- 跨模块只依赖公开查询端口和领域/传输模型，不直接引用其他模块的 Mapper 或 DO。
- SQL 表和全部字段必须有 COMMENT；关联保持逻辑关系，不建立物理外键。
- 大文本使用 TEXT，禁止 JSONB；复用 JsonUtils，不重复创建通用 JSON 工具。
- 增量 SQL 沿用 resources/sql 发布方式，不覆盖原脚本或自动清理业务数据。
- 新增基础设施完成后执行 Maven 编译和相关测试；未实际验证的外部连接必须明确说明。
