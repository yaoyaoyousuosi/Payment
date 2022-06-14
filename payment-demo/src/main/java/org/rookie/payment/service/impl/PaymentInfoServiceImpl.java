package org.rookie.payment.service.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.rookie.payment.entity.PaymentInfo;
import org.rookie.payment.enums.PayType;
import org.rookie.payment.mapper.PaymentInfoMapper;
import org.rookie.payment.service.IPaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author whj
 * @since 2022-06-10
 */
@Service
@Slf4j
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements IPaymentInfoService {
    @Resource
    PaymentInfoMapper paymentInfoMapper;
    @Override
    public void createPaymentInfo(String plainText) {
        HashMap bodyMap = new Gson().fromJson(plainText, HashMap.class);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo((String) bodyMap.get("out_trade_no"));
        paymentInfo.setTransactionId((String) bodyMap.get("transaction_id"));
        paymentInfo.setTradeType((String) bodyMap.get("trade_type"));
        paymentInfo.setOrderNo((String) bodyMap.get("out_trade_no"));
        paymentInfo.setTradeState((String) bodyMap.get("trade_state"));
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        Map amount = (Map) bodyMap.get("amount");
        Integer total = ((Double) amount.get("total")).intValue();
        paymentInfo.setPayerTotal(total);
        // 不想写那么多字段了，干脆直接把所有回调信息插到数据库
        paymentInfo.setContent(plainText);
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public void createPaymentInfoByAliPay(String plainText) {
        Map body = new Gson().fromJson(plainText, Map.class);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo((String) body.get("out_trade_no"));
        paymentInfo.setTransactionId((String) body.get("trade_no"));
        log.info(body.get("fund_bill_list").toString());
        List fundBillList = new Gson().fromJson(body.get("fund_bill_list").toString(), List.class);
        Map fundBillMap = (Map) fundBillList.get(0);
        String fundChannel = fundBillMap.get("fundChannel").toString();
        paymentInfo.setTradeType(fundChannel);
        paymentInfo.setTradeState((String) body.get("trade_status"));
        paymentInfo.setPaymentType(PayType.ALIPAY.getType());
        Integer total = Double.valueOf(body.get("receipt_amount").toString()).intValue();
        paymentInfo.setPayerTotal(total);
        // 不想写那么多字段了，干脆直接把所有回调信息插到数据库
        paymentInfo.setContent(plainText);
        paymentInfoMapper.insert(paymentInfo);
    }
}
