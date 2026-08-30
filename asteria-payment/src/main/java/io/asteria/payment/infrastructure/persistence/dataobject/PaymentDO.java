package io.asteria.payment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** Payment 持久化对象 */
@TableName("payment")
@Data
public class PaymentDO {

    @TableId(type = IdType.INPUT)
    private Long id;

    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String referenceType;
    private String referenceId;
    private String status;
    private Date createdAt;
    private Date authorizedAt;
    private Date capturedAt;
    private Date updatedAt;

    @Version
    private Long version;
}
