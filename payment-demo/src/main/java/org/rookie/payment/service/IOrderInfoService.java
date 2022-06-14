package org.rookie.payment.service;

import org.rookie.payment.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.rookie.payment.enums.OrderStatus;
import org.rookie.payment.enums.PayType;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author whj
 * @since 2022-06-10
 */
public interface  IOrderInfoService extends IService<OrderInfo> {
    OrderInfo createOrder(Long productId, PayType payType);

    void saveCodeUrl(String codeUrl, String orderNo);

    void updatePayStatus(String out_trade_no, OrderStatus status);


    OrderInfo getOrderByOrderNo(String orderNo);

    OrderInfo queryOrderStatusByOrderNo(String orderNo);

    List<OrderInfo> queryOrderByTimeOut(int minus, PayType payType);
}
