package org.rookie.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Configuration
@EnableSwagger2 // 开启swagger2的支持
public class Swagger2Config {
    @Bean
    public Docket getDocket(){
        return new Docket(DocumentationType.SWAGGER_2)
                // 文档标题
                .apiInfo(new ApiInfoBuilder().title("微信支付案例接口文档").build());
    }
}
