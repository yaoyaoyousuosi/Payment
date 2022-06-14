package org.rookie.payment.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@RestController
@Slf4j
public class TestController {
    @Resource
    Environment config;

    @GetMapping("/test")
    public void test(){
        log.info(config.getProperty("alipay.app-id"));
    }
}
