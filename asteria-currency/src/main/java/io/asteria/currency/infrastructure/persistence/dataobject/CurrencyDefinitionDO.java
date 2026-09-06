package io.asteria.currency.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("currency_definition")
public class CurrencyDefinitionDO {
    @TableId(type = IdType.INPUT)
    private String code;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String numericCode;
    private String displayName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String symbol;
    private int minorUnit;
    private boolean enabled;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Instant createdAt;
    private Instant updatedAt;
}
