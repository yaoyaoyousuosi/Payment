package org.rookie.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.rookie.payment.entity.OrderInfo;
import org.rookie.payment.entity.Product;
import org.rookie.payment.enums.OrderStatus;
import org.rookie.payment.mapper.OrderInfoMapper;
import org.rookie.payment.mapper.ProductMapper;
import org.rookie.payment.service.IOrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.rookie.payment.util.OrderNoUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author whj
 * @since 2022-06-10
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements IOrderInfoService {
    @Resource
    ProductMapper productMapper;
    @Override
    public OrderInfo createOrder(Long productId) {
        OrderInfo existOrderInfo = queryOrderByProductIdWithStatus(productId);
        if(existOrderInfo != null){
            return existOrderInfo;
        }
        Product product = productMapper.selectById(productId);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setProductId(productId);
        orderInfo.setCreateTime(new Date());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setTotalFee(product.getPrice());
        baseMapper.insert(orderInfo);
        return orderInfo;
    }

    /**
     * 测试用,实际上只通过商品ID查询未支付订单是不合理的。
     * @param productId
     * @return
     */
    private OrderInfo queryOrderByProductIdWithStatus(Long productId){
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(OrderInfo::getProductId,productId);
        queryWrapper.eq(OrderInfo::getOrderStatus, OrderStatus.NOTPAY.getType());
        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);
        return orderInfo;
    }

    @Override
    public void saveCodeUrl(String codeUrl, String orderNo) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(OrderInfo::getOrderNo,orderNo);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);
        baseMapper.update(orderInfo,queryWrapper);
    }

    @Override
    public void updatePayStatus(String out_trade_no, OrderStatus status) {
        LambdaUpdateWrapper<OrderInfo> updateWrapper = new LambdaUpdateWrapper();
        updateWrapper.eq(OrderInfo::getOrderNo,out_trade_no);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(status.getType());
        baseMapper.update(orderInfo,updateWrapper);
    }

    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getOrderNo,orderNo);
        OrderInfo orderInfo = baseMapper.selectOne(wrapper);
        if(orderInfo == null){
            return null;
        }
        return orderInfo;
    }

    @Override
    public OrderInfo queryOrderStatusByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(OrderInfo::getOrderNo,orderNo);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public List<OrderInfo> queryOrderByTimeOut() {
        // 获取当前时间戳,minus(要减去的时间量)
        Instant past = Instant.now().minus(Duration.ofMinutes(5));
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(OrderInfo::getOrderStatus,OrderStatus.NOTPAY.getType());
        // lean equals
        queryWrapper.le(OrderInfo::getCreateTime,past);
        return baseMapper.selectList(queryWrapper);
    }
}
