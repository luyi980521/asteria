package io.asteria.bootstrap;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisParameterHandler;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import io.asteria.currency.infrastructure.persistence.dataobject.CurrencyDefinitionDO;
import io.asteria.infrastructure.persistence.handler.AuditMetaObjectHandler;
import io.asteria.ledger.infrastructure.persistence.dataobject.JournalEntryDO;
import io.asteria.ledger.infrastructure.persistence.dataobject.LedgerAccountDO;
import io.asteria.ledger.infrastructure.persistence.dataobject.PostingDO;
import io.asteria.payment.infrastructure.persistence.dataobject.PaymentDO;
import io.asteria.payment.infrastructure.persistence.dataobject.PaymentOutboxEventDO;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementBatchDO;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementItemDO;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementOutboxEventDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditTimeFillTest {
    private final MybatisConfiguration configuration = new MybatisConfiguration();

    AuditTimeFillTest() {
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig()
                .setDbConfig(new GlobalConfig.DbConfig().setIdType(com.baomidou.mybatisplus.annotation.IdType.INPUT))
                .setMetaObjectHandler(new AuditMetaObjectHandler()));
    }

    @Test
    void fillsAllModulesAndRefreshesExistingUpdateTime() {
        List<Object> entities = List.of(new PaymentDO(), new PaymentOutboxEventDO(),
                new LedgerAccountDO(), new JournalEntryDO(), new PostingDO(),
                CurrencyDefinitionDO.builder().build(), SettlementBatchDO.builder().build(),
                SettlementItemDO.builder().build(), new SettlementOutboxEventDO());
        for (Object entity : entities) {
            register(entity);
            var meta = SystemMetaObject.forObject(entity);
            fill(entity, SqlCommandType.INSERT);
            assertNotNull(meta.getValue("createdAt"), entity.getClass().getName());
            Object old = meta.getSetterType("createdAt") == Date.class
                    ? Date.from(Instant.EPOCH) : Instant.EPOCH;
            meta.setValue("createdAt", old);
            if (meta.hasSetter("updatedAt")) {
                assertNotNull(meta.getValue("updatedAt"));
                meta.setValue("updatedAt", old);
            }
            fill(entity, SqlCommandType.INSERT);
            assertEquals(old, meta.getValue("createdAt"));
            if (meta.hasSetter("updatedAt")) {
                assertEquals(old, meta.getValue("updatedAt"));
                fill(Map.of("et", entity), SqlCommandType.UPDATE);
                assertNotEquals(old, meta.getValue("updatedAt"));
            }
            assertEquals(old, meta.getValue("createdAt"));
        }
    }

    @Test
    void fillsCustomSettlementItemBatch() {
        var first = SettlementItemDO.builder().build();
        var second = SettlementItemDO.builder().createdAt(Instant.EPOCH).build();
        register(first);
        fill(Map.of("items", List.of(first, second), "param1", List.of(first, second)),
                SqlCommandType.INSERT);
        assertNotNull(first.getCreatedAt());
        assertEquals(Instant.EPOCH, second.getCreatedAt());
    }

    private void register(Object entity) {
        var assistant = new MapperBuilderAssistant(configuration, "audit-test");
        assistant.setCurrentNamespace(entity.getClass().getName());
        TableInfoHelper.initTableInfo(assistant, entity.getClass());
    }

    private void fill(Object parameter, SqlCommandType command) {
        var statement = new MappedStatement.Builder(configuration, "audit." + command,
                new StaticSqlSource(configuration, "SELECT 1"), command).build();
        new MybatisParameterHandler(statement, parameter, statement.getBoundSql(parameter));
    }
}
