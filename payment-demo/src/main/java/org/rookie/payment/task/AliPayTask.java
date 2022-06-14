package org.rookie.payment.task;

import com.alipay.api.AlipayApiException;
import lombok.extern.slf4j.Slf4j;
import org.rookie.payment.entity.OrderInfo;
import org.rookie.payment.enums.PayType;
import org.rookie.payment.service.AliPayService;
import org.rookie.payment.service.IOrderInfoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Slf4j
@Component
public class AliPayTask {
    @Resource
    IOrderInfoService orderInfoService;
    @Resource
    AliPayService aliPayService;
    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirmStatus() throws AlipayApiException {
        log.info("支付宝查单定时任务开始执行");
        List<OrderInfo> orders = orderInfoService.queryOrderByTimeOut(1, PayType.ALIPAY);
        for (OrderInfo order : orders) {
            aliPayService.updateOrderStatus(order.getOrderNo());
        }
    }
}
