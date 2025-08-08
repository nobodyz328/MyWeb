package com.myweb.website_core.infrastructure.config;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring应用上下文提供者
 * <p>
 * 用于在非Spring管理的类中获取Spring Bean
 * 主要用于JPA实体类中获取服务Bean
 * 
 * @author MyWeb Security Team
 * @version 1.0
 * @since 2025-08-01
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    /**
     * -- GETTER --
     *  获取Spring应用上下文
     *
     */
    @Getter
    private static ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 根据Bean名称获取Bean实例
     * 
     * @param beanName Bean名称
     * @return Bean实例
     */
    public static Object getBean(String beanName) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(beanName);
    }
    
    /**
     * 根据Bean类型获取Bean实例
     * 
     * @param beanClass Bean类型
     * @param <T> Bean类型泛型
     * @return Bean实例
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(beanClass);
    }
    
    /**
     * 根据Bean名称和类型获取Bean实例
     * 
     * @param beanName Bean名称
     * @param beanClass Bean类型
     * @param <T> Bean类型泛型
     * @return Bean实例
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(beanName, beanClass);
    }
    
    /**
     * 检查是否包含指定名称的Bean
     * 
     * @param beanName Bean名称
     * @return 是否包含
     */
    public static boolean containsBean(String beanName) {
        if (applicationContext == null) {
            return false;
        }
        return applicationContext.containsBean(beanName);
    }
}