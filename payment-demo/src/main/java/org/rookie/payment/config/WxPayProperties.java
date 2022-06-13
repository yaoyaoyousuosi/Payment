package org.rookie.payment.config;

import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.exception.HttpCodeException;
import com.wechat.pay.contrib.apache.httpclient.exception.NotFoundException;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.Data;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Configuration
@PropertySource(value = "classpath:/wxpay.properties")
@ConfigurationProperties(prefix = "wxpay") // 自动将配置文件中指定前缀的属性与java属性映射
@Data
public class WxPayProperties implements Serializable {
    private String mchId;
    private String mchSerialNo;
    private String privateKeyPath;
    private String apiV3Key;
    private String appId;
    private String domain;
    private String notifyDomain;
    public PrivateKey getPrivateKey(String filePath) {
        // 加载商户私钥（privateKey：私钥字符串）
        try {
            return PemUtil.loadPrivateKey(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("不存在商户私钥",e);
        }
    }
    @Bean
    public Verifier getVerifier() throws HttpCodeException, GeneralSecurityException, IOException, NotFoundException {
        PrivateKey privateKey = getPrivateKey(privateKeyPath);
        // 获取证书管理器实例
        CertificatesManager certificatesManager = CertificatesManager.getInstance();
        // 向证书管理器增加需要自动更新平台证书的商户信息
        certificatesManager.putMerchant(mchId, new WechatPay2Credentials(mchId,
                new PrivateKeySigner(mchSerialNo, privateKey)), apiV3Key.getBytes(StandardCharsets.UTF_8));
        // ... 若有多个商户号，可继续调用putMerchant添加商户信息
        // 从证书管理器中获取verifier
        return certificatesManager.getVerifier(mchId);
    }

    @Bean
    public CloseableHttpClient getWxClient(Verifier verifier){
        PrivateKey privateKey = getPrivateKey(privateKeyPath);
        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(mchId, mchSerialNo, privateKey)
                .withValidator(new WechatPay2Validator(verifier));
        // ... 接下来，你仍然可以通过builder设置各种参数，来配置你的HttpClient
        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        return builder.build();
    }
}
