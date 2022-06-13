package org.rookie.payment.service.impl;

import com.google.gson.Gson;
import org.rookie.payment.entity.PaymentInfo;
import org.rookie.payment.enums.PayType;
import org.rookie.payment.mapper.PaymentInfoMapper;
import org.rookie.payment.service.IPaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
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
}
