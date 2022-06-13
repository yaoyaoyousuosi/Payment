package org.rookie.payment.task;

import lombok.extern.slf4j.Slf4j;
import org.rookie.payment.entity.OrderInfo;
import org.rookie.payment.entity.RefundInfo;
import org.rookie.payment.service.IOrderInfoService;
import org.rookie.payment.service.IRefundInfoService;
import org.rookie.payment.service.WxPayService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Slf4j
@Component // 注册定时任务
public class WxPayTask {
    /**
     * 测试
     * * (cron="秒 分 时 日 月 周")
     * * *：每隔一秒执行
     * * 0/3：从第0秒开始，每隔3秒执行一次
     * * 1-3: 从第1秒开始执行，到第3秒结束执行
     * * 1,2,3：第1、2、3秒执行
     * * ?：不指定，若指定日期，则不指定周，反之同理
     */
    @Resource
    private IOrderInfoService orderInfoService;
    @Resource
    private WxPayService wxPayService;
    @Scheduled(cron = "0/300 * * * * ?")
    public void orderConfirmStatus() throws IOException {
        log.info("查询订单支付状态定时任务触发");
        // TODO: 2022/6/12  查询超过五分钟/等于五分钟未支付的订单
        List<OrderInfo> orders = orderInfoService.queryOrderByTimeOut();
        // TODO: 2022/6/12  调用微信查单接口校验订单真实状态,更新本地库
        for (OrderInfo order : orders) {
            wxPayService.updateTimeOutWithNoNotifyOrder(order.getOrderNo());
        }
    }
    @Resource
    private IRefundInfoService refundInfoService;
    @Scheduled(cron = "0/300 * * * * ?")
    public void orderConfirmRefundStatus() throws IOException {
        log.info("查询退款状态定时任务触发");
        List<RefundInfo> refunds = refundInfoService.queryRefundByTimeOut();
        for (RefundInfo refund : refunds) {
            wxPayService.updateNoNotifyRefund(refund.getRefundNo());
        }
    }
}
