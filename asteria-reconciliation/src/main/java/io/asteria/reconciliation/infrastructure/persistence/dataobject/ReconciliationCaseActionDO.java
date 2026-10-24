package io.asteria.reconciliation.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 工单追加式审计记录；仅记录操作，不执行资金修复，必须与工单变更在同一事务保存。仅映射数据库行，不承担领域状态转换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reconciliation_case_action")
public class ReconciliationCaseActionDO {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 工单 ID，逻辑关联 reconciliation_case.id，无物理外键。 */
    private Long caseId;

    /** 审计动作，限定合法的前后状态组合。 */
    private String actionType;

    /** 操作主体类型 SYSTEM 或 HUMAN。 */
    private String operatorType;

    /** 可追溯的系统或人工主体标识，系统操作也必填。 */
    private String operatorId;

    /** 操作前状态，仅 OPEN 动作允许为空。 */
    private String fromStatus;

    /** 操作后状态；COMMENT 时必须与前状态相同。 */
    private String toStatus;

    /** 操作说明，解决和忽略动作必须给出原因。 */
    private String reason;

    /** 使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存。 */
    private String details;

    /** 记录创建时间，UTC 时间点。 */
    private Instant createdAt;
}
