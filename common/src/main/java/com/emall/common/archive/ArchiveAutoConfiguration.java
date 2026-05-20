package com.emall.common.archive;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(SqlSessionTemplate.class)
public class ArchiveAutoConfiguration {
    @Bean
    @ConditionalOnBean(SqlSessionTemplate.class)
    @ConditionalOnMissingBean
    public ArchiveRepository archiveRepository(SqlSessionTemplate sqlSessionTemplate) {
        registerArchiveMapper(sqlSessionTemplate);
        return new MybatisPlusArchiveRepository(sqlSessionTemplate.getMapper(ArchiveMapper.class));
    }

    @Bean
    @ConditionalOnMissingBean
    public ArchiveService archiveService(ArchiveRepository archiveRepository) {
        return new ArchiveService(archiveRepository);
    }

    private void registerArchiveMapper(SqlSessionTemplate sqlSessionTemplate) {
        Configuration configuration = sqlSessionTemplate.getSqlSessionFactory().getConfiguration();
        if (!configuration.hasMapper(ArchiveMapper.class)) {
            configuration.addMapper(ArchiveMapper.class);
        }
    }
}
