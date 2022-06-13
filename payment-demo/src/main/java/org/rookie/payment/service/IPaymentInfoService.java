package org.rookie.payment.service;

import org.rookie.payment.entity.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author whj
 * @since 2022-06-10
 */
public interface IPaymentInfoService extends IService<PaymentInfo> {
    void createPaymentInfo(String plainText);
}
