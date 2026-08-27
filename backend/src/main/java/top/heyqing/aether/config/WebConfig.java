package top.heyqing.aether.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import top.heyqing.aether.interceptor.VisitorLogInterceptor;

/**
 * Web MVC 配置：注册游客访问记录拦截器（BackEnd-Plan §4.6）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final VisitorLogInterceptor visitorLogInterceptor;

    public WebConfig(VisitorLogInterceptor visitorLogInterceptor) {
        this.visitorLogInterceptor = visitorLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitorLogInterceptor).addPathPatterns("/**");
    }
}
