package io.asteria.ledger.infrastructure.persistence.mapper;

import io.asteria.ledger.infrastructure.persistence.dataobject.JournalEntryDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author lyman.lu
* @description 针对表【ledger_journal_entry(记账凭证表)】的数据库操作Mapper
* @createDate 2026-07-25 17:37:15
* @Entity io.asteria.ledger.infrastructure.persistence.dataobject.JournalEntryDO
*/
@Mapper
public interface JournalEntryMapper extends BaseMapper<JournalEntryDO> {

}




