package com.myweb.website_core.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
    @Bean
    TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
               // configureVirtualHost(context);
                SecurityConstraint constraint = new SecurityConstraint();
                constraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                constraint.addCollection(collection);
                context.addConstraint(constraint);
            }
        };
        factory.addAdditionalTomcatConnectors(redirectConnector());
        return factory;
    }

    private Connector redirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);  // HTTP端口
        connector.setSecure(false);
        connector.setRedirectPort(8443);  // 重定向到HTTPS端口
        return connector;
    }
//    private void configureVirtualHost(Context context) {
//        // 创建一个新的虚拟主机
//        StandardHost host = new StandardHost();
//        host.setName("www.myblog.com"); // 设置虚拟主机的域名
//        host.setAutoDeploy(false);
//        host.setAppBase("webapps");
//
//        // 创建一个新的上下文
//        Context appContext = new StandardContext();
//        appContext.setPath("/");
//        appContext.setDocBase("path/to/your/webapp"); // 设置Web应用的路径
//
//        // 将上下文添加到虚拟主机中
//        host.addChild(appContext);
//
//        // 将虚拟主机添加到Tomcat服务器中
//
//    }

} 