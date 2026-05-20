package com.emall.common.mybatis;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MybatisPlusAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class));

    @Test
    void shouldCreateMybatisPlusInterceptor() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
            assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors()).extracting(Object::getClass)
                    .containsExactly(OptimisticLockerInnerInterceptor.class, BlockAttackInnerInterceptor.class,
                            PaginationInnerInterceptor.class);
        });
    }

    @Test
    void shouldEnableIllegalSqlInterceptorOnlyWhenRequested() {
        contextRunner.withPropertyValues("emall.mybatis-plus.illegal-sql-check=true").run(context -> {
            assertThat(context.getBean(MybatisPlusInterceptor.class).getInterceptors()).extracting(Object::getClass)
                    .contains(IllegalSQLInnerInterceptor.class);
        });
    }

    @Test
    void shouldCreateAuditMetaObjectHandler() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(MetaObjectHandler.class));
    }
}
