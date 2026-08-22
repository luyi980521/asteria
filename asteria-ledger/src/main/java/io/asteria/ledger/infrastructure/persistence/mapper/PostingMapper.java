package io.asteria.ledger.infrastructure.persistence.mapper;

import io.asteria.ledger.infrastructure.persistence.dataobject.PostingDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author lyman.lu
* @description 针对表【ledger_posting(记账分录表)】的数据库操作Mapper
* @createDate 2026-07-25 17:37:15
* @Entity io.asteria.ledger.infrastructure.persistence.dataobject.PostingDO
*/
@Mapper
public interface PostingMapper extends BaseMapper<PostingDO> {

}
