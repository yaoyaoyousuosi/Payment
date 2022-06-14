package org.rookie.payment.config;

import com.alipay.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Configuration
@PropertySource(value = "classpath:alipay-sandbox.properties")
public class AliPayProperties {
    // Environment 整个spring的运行时环境,可以方便的读取配置
    @Resource
    private Environment config;

    @Bean
    public AlipayClient getAlipayClient() throws AlipayApiException {
        AlipayConfig alipayConfig = new AlipayConfig();
        //设置网关地址
        alipayConfig.setServerUrl(config.getProperty("alipay.gateway-url"));
        //设置应用ID
        alipayConfig.setAppId(config.getProperty("alipay.app-id"));
        //设置应用私钥
        alipayConfig.setPrivateKey(config.getProperty("alipay.merchant-private-key"));
        //设置请求格式，固定值json
        alipayConfig.setFormat(AlipayConstants.FORMAT_JSON);
        //设置字符集
        alipayConfig.setCharset(AlipayConstants.CHARSET_UTF8);
        //设置支付宝公钥
        alipayConfig.setAlipayPublicKey(config.getProperty("alipay.alipay-public-key"));
        //设置签名类型
        alipayConfig.setSignType(AlipayConstants.SIGN_TYPE_RSA2);
        //构造client
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);
        return alipayClient;
    }
}
