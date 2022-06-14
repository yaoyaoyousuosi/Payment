package org.rookie.payment.service;

import com.alipay.api.AlipayApiException;
import org.rookie.payment.enums.PayType;

import java.util.Map;

/**
 * @author 王豪杰
 * @Version 1.0
 */
public interface AliPayService {
    String tradePagePay(Long productId, PayType payType) throws AlipayApiException;

    void proceedOrderHandler(Map params);

    void tradeClose(String orderNo) throws AlipayApiException;

    String queryOrderStatus(String orderNo) throws AlipayApiException;

    void updateOrderStatus(String orderNo) throws AlipayApiException;

    String getBillDownloadUrl(String billDate, String type) throws AlipayApiException;

    void refund(String orderNo, String reason) throws AlipayApiException;

    String refundQuery(String orderNo, String refundNo) throws AlipayApiException;
}
