package io.asteria.reconciliation.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 来源业务的不可变标准化快照；同一来源记录不同版本可并存，不依附单个 Run。仅映射数据库行，不承担领域状态转换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reconciliation_source_record")
public class ReconciliationSourceRecordDO {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 来源系统类型，与原始标识和版本共同构成幂等键。 */
    private String sourceType;

    /** 来源系统原始记录 ID，跨渠道时需带命名空间。 */
    private String sourceRecordId;

    /** 来源记录的单调非负版本；无原生版本时采集方须提供稳定映射。 */
    private Long sourceVersion;

    /** 规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间。 */
    private String businessKey;

    /** 标准化金额，允许零；未知金额整体为空。 */
    private BigDecimal amount;

    /** 标准化金额，允许零；未知金额整体为空所对应的币种；金额为空时币种也必须为空。 */
    private String currency;

    /** 原始业务状态，保留来源语义，由规则解释。 */
    private String sourceStatus;

    /** 可选的入账或记账状态，由规则解释。 */
    private String postingStatus;

    /** 来源业务发生时间，按此字段落入左闭右开对账窗口。 */
    private Instant occurredAt;

    /** JsonUtils 序列化的原始业务快照 JSON 对象；不可修改。 */
    private String payload;

    /** 记录创建时间，UTC 时间点。 */
    private Instant createdAt;
}
