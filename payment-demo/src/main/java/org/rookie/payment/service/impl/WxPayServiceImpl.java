package org.rookie.payment.service.impl;

import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.rookie.payment.config.WxPayProperties;
import org.rookie.payment.entity.OrderInfo;
import org.rookie.payment.enums.OrderStatus;
import org.rookie.payment.enums.PayType;
import org.rookie.payment.enums.wxpay.WxApiType;
import org.rookie.payment.enums.wxpay.WxNotifyType;
import org.rookie.payment.enums.wxpay.WxTradeState;
import org.rookie.payment.service.IOrderInfoService;
import org.rookie.payment.service.IPaymentInfoService;
import org.rookie.payment.service.IRefundInfoService;
import org.rookie.payment.service.WxPayService;
import org.rookie.payment.util.OrderNoUtils;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {
    // 支付参数配置对象
    @Resource
    WxPayProperties wxPayProperties;
    // http client对象,负责微信接口的远程调用,下载证书,自动签名解签
    @Resource
    CloseableHttpClient httpClient;
    @Resource
    IOrderInfoService orderInfoService;

    @Override
    public Map nativePay(Long productId) throws Exception {
        log.info("创建订单");
        OrderInfo order = orderInfoService.createOrder(productId);
        String codeUrl = order.getCodeUrl();
        if (codeUrl != null && codeUrl != "") {
            HashMap<String, Object> respMap = new HashMap();
            respMap.put("orderNo", order.getOrderNo());
            respMap.put("codeUrl", codeUrl);
            return respMap;
        }
        // 封装一个post请求对象
        HttpPost httpPost = new HttpPost(wxPayProperties.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        // 请求body参数
        Gson gson = new Gson();
        Map<String, Object> paramsMap = new HashMap();
        paramsMap.put("appid", wxPayProperties.getAppId());
        paramsMap.put("mchid", wxPayProperties.getMchId());
        paramsMap.put("description", "测试商品");
        paramsMap.put("out_trade_no", order.getOrderNo());
        paramsMap.put("notify_url", wxPayProperties.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));
        Map<String, Object> amount = new HashMap();
        amount.put("total", order.getTotalFee());
        amount.put("currency", "CNY");
        paramsMap.put("amount", amount);
        StringEntity entity = new StringEntity(gson.toJson(paramsMap), "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        //完成签名并执行请求
        CloseableHttpResponse response = httpClient.execute(httpPost);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String respBody = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) { //处理成功
                log.info("预支付订单创建成功:" + respBody);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("预支付订单创建成功");
            } else {
                log.info("预支付订单创建失败:" + respBody);
                throw new IOException("request failed");
            }
            HashMap<String, String> codeMap = gson.fromJson(respBody, HashMap.class);
            codeUrl = codeMap.get("code_url");
            orderInfoService.saveCodeUrl(codeUrl, order.getOrderNo());
            HashMap<String, Object> respMap = new HashMap();
            respMap.put("orderNo", order.getOrderNo());
            respMap.put("codeUrl", codeUrl);
            return respMap;
        } finally {
            response.close();
        }
    }

    @Resource
    IPaymentInfoService paymentInfoService;
    /**
     * ReentrantLock 可重入锁, 线程可尝试获取锁,最多等一秒获取不到直接false不等待。需要自己释放锁 JUC包
     * synchronized 比较重,线程拿不到锁会一直等待。
     */
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public void processOrder(Map bodyMap) {
        String orderNo = ((String) bodyMap.get("out_trade_no"));
        if (lock.tryLock()) {
            try {
                // 处理重复通知
                OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);
                if (orderInfo != null && OrderStatus.SUCCESS.getType().equals(orderInfo.getOrderStatus())) {
                    return;
                }
                // TODO: 订单后续处理器
                log.info("创建支付日志");
                paymentInfoService.createPaymentInfo(new Gson().toJson(bodyMap));
                log.info("更新订单状态");
                orderInfoService.updatePayStatus((String) bodyMap.get("out_trade_no"), OrderStatus.SUCCESS);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void cancelOrder(String orderNo) throws IOException {
        // TODO  调用微信关单接口
        close(orderNo);
        // TODO  更新订单状态
        orderInfoService.updatePayStatus(orderNo, OrderStatus.CANCEL);
    }

    private void close(String orderNo) throws IOException {
        //请求URL
        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        HttpPost httpPost = new HttpPost(wxPayProperties.getDomain() + url);
        //请求body参数
        String reqdata = "{\"mchid\": \"" + wxPayProperties.getMchId() + "\"}";

        StringEntity entity = new StringEntity(reqdata, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = httpClient.execute(httpPost);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("关单接口调用成功");
            } else if (statusCode == 204) {
                log.info("关单接口调用成功");
            } else {
                log.info("关单接口调用失败failed,resp code = " + statusCode);
                throw new IOException("request failed");
            }
        } finally {
            response.close();
        }
    }

    @Override
    public String queryOrderStatus(String orderNo) throws IOException {
        String url = wxPayProperties.getDomain().concat(String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo));
        url += "?mchid=" + wxPayProperties.getMchId();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String respBody = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("查单接口调用成功:" + respBody);
            } else if (statusCode == 204) {
                log.info("查单接口调用成功");
                return null;
            } else {
                log.info("查单接口调用失败,resp code = " + statusCode);
                throw new IOException("request failed");
            }
            return respBody;
        } finally {
            response.close();
        }
    }

    @Override
    public void updateTimeOutWithNoNotifyOrder(String orderNo) throws IOException {
        String body = queryOrderStatus(orderNo);
        Map bodyMap = new Gson().fromJson(body, Map.class);
        String orderStatus = (String) bodyMap.get("trade_state");
        // TODO: 2022/6/12 用户已支付但客户端未收到微信通知情况
        if(WxTradeState.SUCCESS.getType().equals(orderStatus)) {
            processOrder(bodyMap);
            // TODO: 2022/6/12 用户未支付超时,取消订单。
        } else if(WxTradeState.NOTPAY.getType().equals(orderStatus)){
            close(orderNo);
            orderInfoService.updatePayStatus(orderNo, OrderStatus.CLOSED);
        }
    }

    @Override
    public void refund(String orderNo, String reason) throws IOException {
        log.info("发起退款");
        // 封装一个post请求对象
        Gson gson = new Gson();
        // 根据订单号创建退款单
        String refundNo = OrderNoUtils.getRefundNo();
        refundInfoService.createRefundInfo(orderNo, reason, refundNo);
        HttpPost httpPost = new HttpPost(wxPayProperties.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType()));
        // 构造退款body参数
        Map<String, Object> paramsMap = new HashMap();
        paramsMap.put("out_refund_no", refundNo);
        paramsMap.put("reason", reason);
        paramsMap.put("notify_url", wxPayProperties.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));
        paramsMap.put("out_trade_no", orderNo);
        Map<String, Object> amount = new HashMap();
        // 偷懒了这边·要从数据库查
        amount.put("refund", 1);
        amount.put("total", 1);
        amount.put("currency", "CNY");
        paramsMap.put("amount", amount);
        StringEntity entity = new StringEntity(gson.toJson(paramsMap), "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        //完成签名并执行请求
        CloseableHttpResponse response = httpClient.execute(httpPost);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String respBody = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) { //处理成功
                log.info("退款受理中:" + respBody);
            } else if (statusCode == 204) { //处理成功，无返回Body
                log.info("退款受理中");
            } else {
                log.info("退款失败" + respBody);
                throw new IOException("request failed");
            }
            orderInfoService.updatePayStatus(orderNo, OrderStatus.REFUND_PROCESSING);
        } finally {
            response.close();
        }
    }
    @Resource
    IRefundInfoService refundInfoService;
    @Override
    public void processRefund(Map bodyMap) {
        // 此处测试用,生产不可使用
        String refundStatus = (String) bodyMap.get("refund_status");
        String orderNo = (String) bodyMap.get("out_trade_no");
        if(lock.tryLock()){
            try{
                OrderInfo order = orderInfoService.getOrderByOrderNo(orderNo);
                if(order != null && OrderStatus.REFUND_SUCCESS.getType().equals(order.getOrderStatus())){
                    return;
                }
//                if(WxTradeState.SUCCESS.getType().equals(refundStatus)){
                refundInfoService.updateRefund(new Gson().toJson(bodyMap));
                orderInfoService.updatePayStatus(orderNo, OrderStatus.REFUND_SUCCESS);
//                }
            }finally {
                lock.unlock();
            }
        }
    }

    @Override
    public String queryRefundStatus(String refundNo) throws IOException {
        String url = wxPayProperties.getDomain().concat(String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo));
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String respBody = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("查退款接口调用成功:" + respBody);
            } else if (statusCode == 204) {
                log.info("查退款接口调用成功");
                return null;
            } else {
                log.info("查退款接口调用失败,resp code = " + statusCode);
                throw new IOException("request failed");
            }
            return respBody;
        } finally {
            response.close();
        }
    }

    @Override
    public void updateNoNotifyRefund(String refundNo) throws IOException {
        String body = this.queryRefundStatus(refundNo);
        Map bodyMap = new Gson().fromJson(body, Map.class);
        String status = (String) bodyMap.get("status");
        if(WxTradeState.SUCCESS.getType().equals(status)){
            processRefund(bodyMap);
        }
    }

    @Override
    public String downloadBill(String billDate, String type) throws URISyntaxException, IOException {
        String downloadUrl = this.queryBill(billDate, type);
        PrivateKey merchantPrivateKey = PemUtil
                .loadPrivateKey(new FileInputStream("apiclient_key.pem"));
        //初始化httpClient
        //该接口无需进行签名验证、通过withValidator((response) -> true)实现
        httpClient =  WechatPayHttpClientBuilder.create()
                .withMerchant(wxPayProperties.getMchId(), wxPayProperties.getMchSerialNo(), merchantPrivateKey)
                .withValidator((response) -> true).build();

        //请求URL
        //账单文件的下载地址的有效时间为30s
        URIBuilder uriBuilder = new URIBuilder(downloadUrl);
        HttpGet httpGet = new HttpGet(uriBuilder.build());
        httpGet.addHeader("Accept", "application/json");

        //执行请求
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("账单下载成功:" + body);
            } else if (statusCode == 204) {
                log.info("账单下载成功");
            } else {
                log.info("failed,resp code = " + statusCode+ ",return body = " + body);
                throw new IOException("request failed");
            }
            return body;
        } finally {
            response.close();
        }
    }

    @Override
    public String queryBill(String billDate, String type) throws IOException {
        String url = wxPayProperties.getDomain();
        if("tradebill".equals(type)){
            url += WxApiType.TRADE_BILLS.getType().concat("?bill_date=").concat(billDate);
        }else {
            url += WxApiType.FUND_FLOW_BILLS.getType().concat("?bill_date=").concat(billDate);
        }
        System.out.println(url);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Accept", "application/json");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String body = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("申请账单成功:"+body);
            } else if (statusCode == 204) {
                log.info("申请账单成功");
            } else {
                log.info("failed,resp code = " + statusCode+ ",return body = " + body);
                throw new IOException("request failed");
            }
            Map map = new Gson().fromJson(body, Map.class);
            return (String) map.get("download_url");
        } finally {
            response.close();
        }
    }
}
