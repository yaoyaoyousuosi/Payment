package org.rookie.payment.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.rookie.payment.entity.OrderInfo;
import org.rookie.payment.entity.Product;
import org.rookie.payment.enums.OrderStatus;
import org.rookie.payment.service.IOrderInfoService;
import org.rookie.payment.vo.RespBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Api(tags = "订单信息接口")
@RestController
@RequestMapping("/api/order-info")
public class OrderInfoController {
    @Resource
    private IOrderInfoService orderInfoService;

    @ApiOperation("查询订单列表")
    @GetMapping("/list")
    public RespBean<OrderInfo> getOrderList(){
        List<OrderInfo> orderInfos = orderInfoService.list();
        Map<String,List<OrderInfo>> respMap = new HashMap();
        respMap.put("list",orderInfos);
        return new RespBean(200,"success",respMap);
    }

    @ApiOperation("查询订单支付状态")
    @GetMapping("/query-order-status/{orderNo}")
    public RespBean<OrderInfo> queryOrderStatusByOrderNo(@PathVariable String orderNo){
        OrderInfo orderInfo = orderInfoService.queryOrderStatusByOrderNo(orderNo);
        if(OrderStatus.NOTPAY.getType().equals(orderInfo.getOrderStatus())){
            return new RespBean(101,"支付中.....",null);
        }
        return new RespBean(0,"支付成功",orderInfo);
    }
}
