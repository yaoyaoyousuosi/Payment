package org.rookie.payment.controller;

import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.notification.Notification;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationHandler;
import com.wechat.pay.contrib.apache.httpclient.notification.NotificationRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.rookie.payment.config.WxPayProperties;
import org.rookie.payment.service.WxPayService;
import org.rookie.payment.util.HttpUtils;
import org.rookie.payment.vo.RespBean;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "微信支付接口")
public class WxPayController {
    @Resource
    WxPayService wxPayService;
    @Resource
    WxPayProperties wxPayProperties;
    @Resource
    Verifier verifier;
    @ApiOperation("创建订单")
    @PostMapping("/native/{productId}")
    public RespBean nativePay(@PathVariable Long productId) throws Exception {
        Map map = wxPayService.nativePay(productId);
        return new RespBean(200,"success",map);
    }

    /**
     *  网络超时>5s的响应微信会拒绝并发起重复通知,此接口要满足幂等性并再多线程情况下保证数据的一致性
     */
    @ApiOperation("支付通知回调")
    @PostMapping("/native/notify")
    public String payNotify(HttpServletRequest req, HttpServletResponse resp) throws InterruptedException {
        log.info("接收支付通知");
        Gson gson = new Gson();
        // TODO: 接收微信通知
        String body = HttpUtils.readData(req);
        HashMap notifyParams = gson.fromJson(body, HashMap.class);
        log.info(notifyParams.toString());

        // TODO: 验签
        // 从请求头中获取签名信息
        log.info("支付回调验签开始");
        String nonce = req.getHeader("Wechatpay-Nonce"); // 随机串
        String signature = req.getHeader("Wechatpay-Signature"); // 签名值
        String wechatPaySerial = req.getHeader("Wechatpay-Serial"); // 平台证书序列号
        String timestamp = req.getHeader("Wechatpay-Timestamp"); // 时间戳
        // 构造验签名串
        NotificationRequest request = new NotificationRequest.Builder().withSerialNumber(wechatPaySerial)
                .withNonce(nonce)
                .withTimestamp(timestamp)
                .withSignature(signature)
                .withBody(body)
                .build();
        NotificationHandler handler = new NotificationHandler(verifier, wxPayProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        // 验签和解析请求体
        Notification notification = null;
        HashMap<String, String> respParams = new HashMap();
        try {
            notification = handler.parse(request);
        } catch (Exception e) {
            resp.setStatus(500);
            respParams.put("code","ERROR");
            respParams.put("message","失败");
            log.info("支付回调验签失败");
            return gson.toJson(respParams);
        }
        // 从notification中获取解密报文
        String decryptData = notification.getDecryptData();
        HashMap bodyMap = gson.fromJson(decryptData, HashMap.class);
        wxPayService.processOrder(bodyMap);
        // TODO: 应答
        resp.setStatus(200);
        respParams.put("code","SUCCESS");
        respParams.put("message","成功");
        log.info("支付回调应答成功");
        return gson.toJson(respParams);
    }

    @PostMapping("/cancel/{orderNo}")
    @ApiOperation("关单接口")
    public RespBean cancelOrder(@PathVariable("orderNo") String orderNo) throws IOException {
        wxPayService.cancelOrder(orderNo);
        return new RespBean(200,"取消订单成功",null);
    }


    @ApiOperation("退款接口")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public RespBean refund(@PathVariable String orderNo, @PathVariable String reason) throws IOException {
        wxPayService.refund(orderNo, reason);
        return new RespBean(200, "退款中", null);
    }

    @PostMapping("/refunds/notify")
    public String refundNotify(HttpServletRequest req, HttpServletResponse resp){
        // TODO: 2022/6/12 接收通知
        String body = HttpUtils.readData(req);
        // TODO: 2022/6/12 验签
        log.info("退款通知验签开始");
        Gson gson = new Gson();
        String nonce = req.getHeader("Wechatpay-Nonce"); // 随机串
        String signature = req.getHeader("Wechatpay-Signature"); // 签名值
        String wechatPaySerial = req.getHeader("Wechatpay-Serial"); // 平台证书序列号
        String timestamp = req.getHeader("Wechatpay-Timestamp"); // 时间戳
        // 构造验签名串
        NotificationRequest request = new NotificationRequest.Builder().withSerialNumber(wechatPaySerial)
                .withNonce(nonce)
                .withTimestamp(timestamp)
                .withSignature(signature)
                .withBody(body)
                .build();
        NotificationHandler handler = new NotificationHandler(verifier, wxPayProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        // 验签和解析请求体
        Notification notification = null;
        HashMap<String, String> respParams = new HashMap();
        try {
            notification = handler.parse(request);
        } catch (Exception e) {
            resp.setStatus(500);
            respParams.put("code","ERROR");
            respParams.put("message","失败");
            log.info("验签失败");
            return gson.toJson(respParams);
        }
        // 从notification中获取解密报文
        String decryptData = notification.getDecryptData();
        Map bodyMap = gson.fromJson(decryptData, Map.class);
        // TODO: 2022/6/12 更新退款日志,订单状态
        wxPayService.processRefund(bodyMap);
        // TODO: 2022/6/12 响应微信
        resp.setStatus(200);
        respParams.put("code","SUCCESS");
        respParams.put("message","成功");
        return gson.toJson(respParams);
    }

    @GetMapping("/downloadbill/{billDate}/{type}")
    public RespBean downloadBill(@PathVariable String billDate, @PathVariable String type) throws IOException, URISyntaxException {
        String body = wxPayService.downloadBill(billDate, type);
        HashMap<Object, Object> hashMap = new HashMap<>();
        hashMap.put("result", body);
        return new RespBean(200, "success", hashMap);
    }

}
