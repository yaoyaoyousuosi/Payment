package org.rookie.payment.service;

import org.rookie.payment.entity.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author whj
 * @since 2022-06-10
 */
public interface IRefundInfoService extends IService<RefundInfo> {

    void createRefundInfo(String orderNo, String reason, String refundNo);

    void updateRefund(String toJson);

    List<RefundInfo> queryRefundByTimeOut();
}
