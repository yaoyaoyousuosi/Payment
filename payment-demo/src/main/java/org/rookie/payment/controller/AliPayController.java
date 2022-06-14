package org.rookie.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.rookie.payment.enums.PayType;
import org.rookie.payment.service.AliPayService;
import org.rookie.payment.vo.RespBean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 王豪杰
 * @Version 1.0
 */
@Api(tags = "支付宝支付接口")
@RestController
@RequestMapping("/api/ali-pay")
@Slf4j
public class AliPayController {
    @Resource
    AliPayService aliPayService;

    @ApiOperation("下单接口")
    @PostMapping("/trade/page/pay/{productId}")
    public RespBean tradePagePay(@PathVariable Long productId) throws AlipayApiException {
        String body = aliPayService.tradePagePay(productId, PayType.ALIPAY);
        HashMap htmlMap = new HashMap<>();
        htmlMap.put("formStr",body);
        return new RespBean(200,"success",htmlMap);
    }

    @Resource
    Environment config;
    @ApiOperation("支付通知回调")
    @PostMapping("/trade/notify")
    public String tradeNotify(@RequestParam Map params) throws AlipayApiException {
        // TODO: 2022/6/14 接收通知
        log.info(params+"");
        String result;
        // TODO: 2022/6/14 验签
        boolean signVerified = AlipaySignature.rsaCheckV1(
                params,
                config.getProperty("alipay.alipay-public-key"),
                "UTF-8", "RSA2"); //调用SDK验证签名
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理
            result = "success";
            aliPayService.proceedOrderHandler(params);
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            result = "failure";
        }
        return result;
    }

    @ApiOperation("关闭订单")
    @PostMapping("/trade/close/{orderNo}")
    public RespBean tradeClose(@PathVariable String orderNo) throws AlipayApiException {
        aliPayService.tradeClose(orderNo);
        return new RespBean(200, "success", null);
    }

    @ApiOperation("查单接口")
    @PostMapping("/query/{orderNo}")
    public RespBean queryOrderStatus(@PathVariable String orderNo) throws AlipayApiException {
        return new RespBean(200, "success",aliPayService.queryOrderStatus(orderNo));
    }

    @ApiOperation("对账接口")
    @GetMapping("/bill/downloadurl/query/{billDate}/{type}")
    public RespBean  getBillDownloadUrl(@PathVariable String billDate, @PathVariable String type) throws AlipayApiException {
        String billUrl = aliPayService.getBillDownloadUrl(billDate, type);
        HashMap urlMap = new HashMap();
        urlMap.put("downloadUrl",billUrl);
        return new RespBean(200, "success",urlMap);
    }

    @ApiOperation("退款接口")
    @PostMapping("/trade/refund/{orderNo}/{reason}")
    public RespBean refund(@PathVariable String orderNo, @PathVariable String reason) throws AlipayApiException {
        aliPayService.refund(orderNo, reason);
        return new RespBean(200, "success", orderNo);
    }

    @ApiOperation("查询退款接口")
    @GetMapping("/refund/query/{orderNo}/{refundNo}")
    public RespBean refundQuery(@PathVariable String orderNo, @PathVariable String refundNo) throws AlipayApiException {
        String body = aliPayService.refundQuery(orderNo, refundNo);
        return new RespBean(200, "success", body);
    }
}
