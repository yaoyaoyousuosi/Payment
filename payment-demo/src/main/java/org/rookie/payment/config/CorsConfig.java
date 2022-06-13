package org.rookie.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Configuration
public class CorsConfig extends WebMvcConfigurerAdapter {
    // 添加跨域支持
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 允许访问的地址
                .allowedOriginPatterns("*") // 请求来源
                .allowCredentials(true)  // 是否允许携带cookie
                .allowedMethods("GET","PUT","DELETE","POST");  // 允许的请求方式
    }
}
