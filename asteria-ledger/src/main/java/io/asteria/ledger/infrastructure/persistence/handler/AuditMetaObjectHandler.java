package io.asteria.ledger.infrastructure.persistence.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自动填充持久化对象的创建时间和更新时间。
 * 仅在调用方没有显式提供值时填充。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        strictInsertFill(metaObject, "createdAt", Date.class, now);
        strictInsertFill(metaObject, "updatedAt", Date.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", Date.class, new Date());
    }
}
