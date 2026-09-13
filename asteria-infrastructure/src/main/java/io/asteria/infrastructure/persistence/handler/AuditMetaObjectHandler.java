package io.asteria.infrastructure.persistence.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/** 各模块共用的审计时间填充：插入时补空值，更新时刷新更新时间。 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Instant now = Instant.now();
        strictInsertFill(metaObject, "createdAt", Date.class, Date.from(now));
        strictInsertFill(metaObject, "updatedAt", Date.class, Date.from(now));
        strictInsertFill(metaObject, "createdAt", Instant.class, now);
        strictInsertFill(metaObject, "updatedAt", Instant.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Instant now = Instant.now();
        findTableInfo(metaObject).getFieldList().stream()
                .filter(field -> field.isWithUpdateFill() && "updatedAt".equals(field.getProperty()))
                .forEach(field -> {
                    if (field.getPropertyType() == Date.class) {
                        setFieldValByName("updatedAt", Date.from(now), metaObject);
                    } else if (field.getPropertyType() == Instant.class) {
                        setFieldValByName("updatedAt", now, metaObject);
                    }
                });
    }
}
