package org.rookie.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.gson.Gson;
import org.rookie.payment.entity.OrderInfo;
import org.rookie.payment.entity.RefundInfo;
import org.rookie.payment.enums.OrderStatus;
import org.rookie.payment.enums.wxpay.WxApiType;
import org.rookie.payment.enums.wxpay.WxTradeState;
import org.rookie.payment.mapper.RefundInfoMapper;
import org.rookie.payment.service.IRefundInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
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
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements IRefundInfoService {
    @Override
    public void createRefundInfo(String orderNo, String reason, String refundNo) {
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setReason(reason);
        refundInfo.setRefundNo(refundNo);
        refundInfo.setRefundStatus(WxTradeState.REFUND.getType());
        baseMapper.insert(refundInfo);
    }

    @Override
    public void updateRefund(String json) {
        Map map = new Gson().fromJson(json, Map.class);
        RefundInfo refundInfo = new RefundInfo();
        String orderNo = map.get("out_trade_no").toString();
        refundInfo.setRefundId(map.get("refund_id").toString());
        refundInfo.setRefundStatus(WxTradeState.SUCCESS.getType());
        Map amount = (Map) map.get("amount");
        refundInfo.setTotalFee(((Double) amount.get("total")).intValue());
        refundInfo.setRefund(((Double) amount.get("refund")).intValue());
        refundInfo.setContentNotify(json);
        LambdaUpdateWrapper<RefundInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(RefundInfo::getOrderNo, orderNo);
        baseMapper.update(refundInfo, updateWrapper);
    }

    @Override
    public List<RefundInfo> queryRefundByTimeOut() {
        Instant past = Instant.now().minus(Duration.ofMinutes(5));
        LambdaQueryWrapper<RefundInfo> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(RefundInfo::getRefundStatus, WxTradeState.REFUND.getType());
        // lean equals
        queryWrapper.le(RefundInfo::getCreateTime,past);
        return baseMapper.selectList(queryWrapper);
    }
}
