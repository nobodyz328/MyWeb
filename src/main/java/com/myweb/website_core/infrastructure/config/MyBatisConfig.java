package com.myweb.website_core.infrastructure.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis配置类
 * 
 * 配置MyBatis以处理复杂的动态SQL查询
 * 特别是解决PostgreSQL中null参数类型推断问题
 */
@Configuration
@MapperScan("com.myweb.website_core.infrastructure.persistence.mapper")
public class MyBatisConfig {
    
    /**
     * 配置SqlSessionFactory
     * 
     * @param dataSource 数据源
     * @return SqlSessionFactory
     * @throws Exception 配置异常
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        
        // 设置mapper XML文件位置
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        
        // 设置类型别名包
        factoryBean.setTypeAliasesPackage("com.myweb.website_core.domain.security.entity");
        
        // 配置MyBatis设置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        
        // 开启驼峰命名转换
        configuration.setMapUnderscoreToCamelCase(true);
        
        // 开启延迟加载
        configuration.setLazyLoadingEnabled(true);
        configuration.setAggressiveLazyLoading(false);
        
        // 设置缓存
        configuration.setCacheEnabled(true);
        
        // 设置JDBC类型为NULL时的处理
        configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);
        
        factoryBean.setConfiguration(configuration);
        
        return factoryBean.getObject();
    }
}