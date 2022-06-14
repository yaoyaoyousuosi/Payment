package org.rookie.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.rookie.payment.entity.OrderInfo;
import org.rookie.payment.enums.OrderStatus;
import org.rookie.payment.enums.PayType;
import org.rookie.payment.service.AliPayService;
import org.rookie.payment.service.IOrderInfoService;
import org.rookie.payment.service.IPaymentInfoService;
import org.rookie.payment.service.IRefundInfoService;
import org.rookie.payment.util.OrderNoUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Service
@Slf4j
public class AliPayServiceImpl implements AliPayService {
    @Resource
    IOrderInfoService orderInfoService;
    @Resource
    IPaymentInfoService paymentInfoService;
    @Resource
    AlipayClient alipayClient;
    @Resource
    Environment config;
    @Override
    public String tradePagePay(Long productId, PayType payType) throws AlipayApiException {
        OrderInfo order = orderInfoService.createOrder(productId,payType);
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(config.getProperty("alipay.notify-url"));
        request.setReturnUrl(config.getProperty("alipay.return-url"));
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", order.getTotalFee());
        bizContent.put("subject", "测试商品");
                bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        String body = response.getBody();
        if(response.isSuccess()){
            log.info("支付宝下单接口调用成功:"+body);
        } else {
            log.info("支付宝下单接口调用失败:"+body);
        }
        return body;
    }

    private ReentrantLock lock = new ReentrantLock();
    @Override
    public void proceedOrderHandler(Map params) {
        String orderNo = params.get("out_trade_no").toString();
        OrderInfo order = orderInfoService.queryOrderStatusByOrderNo(orderNo);
        if(lock.tryLock()) {
            try {
                if (order == null || OrderStatus.SUCCESS.getType().equals(order.getOrderStatus())) {
                    return;
                }
                orderInfoService.updatePayStatus(orderNo, OrderStatus.SUCCESS);
                paymentInfoService.createPaymentInfoByAliPay(new Gson().toJson(params));
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void tradeClose(String orderNo) throws AlipayApiException {
        if(close(orderNo)){
            orderInfoService.updatePayStatus(orderNo, OrderStatus.CLOSED);
        }
    }

    private Boolean close(String orderNo) throws AlipayApiException {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        log.info(orderNo);
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public String queryOrderStatus(String orderNo) throws AlipayApiException {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            log.info("查单成功");
        } else {
            log.info("查单失败");
            // 可能没有创建这个订单
            return null;
        }
        return response.getTradeStatus();
    }

    @Override
    public void updateOrderStatus(String orderNo) throws AlipayApiException {
        String status = queryOrderStatus(orderNo);
        if(status == null){
            log.info("用户未扫码,删除本地订单");
            LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper();
            queryWrapper.eq(OrderInfo::getOrderNo,orderNo);
            orderInfoService.remove(queryWrapper);
        }else {
            log.info("超时订单,更新本地订单状态");
            close(orderNo);
            orderInfoService.updatePayStatus(orderNo, OrderStatus.CLOSED);
        }
    }

    @Override
    public String getBillDownloadUrl(String billDate, String type) throws AlipayApiException {
        AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("bill_type", type);
        bizContent.put("bill_date", billDate);
        request.setBizContent(bizContent.toString());
        AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            log.info("对账接口调用成功:"+response.getBody());
        } else {
            System.out.println("对账接口调用失败:"+response.getBody());
        }
        return response.getBillDownloadUrl();
    }
    @Resource
    IRefundInfoService refundInfoService;
    @Override
    public void refund(String orderNo, String reason) throws AlipayApiException {
        OrderInfo order = orderInfoService.queryOrderStatusByOrderNo(orderNo);
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("refund_amount", order.getTotalFee());
        bizContent.put("refund_reason", reason);
        bizContent.put("out_request_no", OrderNoUtils.getRefundNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            log.info("退款接口调用成功:"+response.getBody());
            orderInfoService.updatePayStatus(orderNo, OrderStatus.REFUND_PROCESSING);
            refundInfoService.createRefundInfo(orderNo, reason, response.getTradeNo());
        } else {
            log.info("退款接口调用失败:"+response.getBody());
        }
    }

    @Override
    public String refundQuery(String orderNo, String refundNo) throws AlipayApiException {
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("out_request_no", refundNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            log.info("查询退款接口调用成功:"+response.getBody());
        } else {
            log.info("查询退款接口调用失败:"+response.getBody());
        }
        return response.getSubMsg();
    }
}
