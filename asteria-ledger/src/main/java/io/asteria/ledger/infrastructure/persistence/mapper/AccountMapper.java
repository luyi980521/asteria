package io.asteria.ledger.infrastructure.persistence.mapper;

import io.asteria.ledger.infrastructure.persistence.dataobject.AccountDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 61421
* @description 针对表【ledger_account(账本账户表)】的数据库操作Mapper
* @createDate 2026-08-22 17:36:35
* @Entity io.asteria.ledger.infrastructure.persistence.dataobject.AccountDO
*/
@Mapper
public interface AccountMapper extends BaseMapper<AccountDO> {

}




