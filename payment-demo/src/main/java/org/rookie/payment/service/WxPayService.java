package org.rookie.payment.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 王豪杰
 * @Version 1.0
 */
public interface WxPayService {
    Map nativePay(Long productId) throws Exception;

    void processOrder(Map bodyMap);

    void cancelOrder(String orderNo) throws IOException;

    String queryOrderStatus(String orderNo) throws IOException;

    void updateTimeOutWithNoNotifyOrder(String orderNo) throws IOException;

    void refund(String orderNo, String reason) throws IOException;

    void processRefund(Map bodyMap);

    String queryRefundStatus(String refundNo) throws IOException;

    void updateNoNotifyRefund(String refundNo) throws IOException;

    String downloadBill(String billDate, String type) throws URISyntaxException, IOException;

    String queryBill(String billDate, String type) throws IOException;
}
