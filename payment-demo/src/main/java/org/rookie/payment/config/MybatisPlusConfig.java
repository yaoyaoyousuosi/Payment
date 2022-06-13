package org.rookie.payment.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Configuration
@MapperScan(basePackages = "org.rookie.payment.mapper")
public class MybatisPlusConfig {
}
