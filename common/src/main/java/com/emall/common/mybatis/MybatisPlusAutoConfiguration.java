package com.emall.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({ MybatisPlusInterceptor.class, PaginationInnerInterceptor.class })
public class MybatisPlusAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
            @Value("${emall.mybatis-plus.illegal-sql-check:false}") boolean illegalSqlCheckEnabled) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        if (illegalSqlCheckEnabled) {
            interceptor.addInnerInterceptor(new IllegalSQLInnerInterceptor());
        }
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler mybatisPlusAuditMetaObjectHandler(Clock clock) {
        return new UtcAuditMetaObjectHandler(clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock mybatisPlusAuditClock() {
        return Clock.systemUTC();
    }

    private static final class UtcAuditMetaObjectHandler implements MetaObjectHandler {
        private final Clock clock;

        private UtcAuditMetaObjectHandler(Clock clock) {
            this.clock = clock;
        }

        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = now();
            strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
            strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now());
        }

        private LocalDateTime now() {
            return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        }
    }
}
