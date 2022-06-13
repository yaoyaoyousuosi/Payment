package org.rookie.payment.service.impl;

import org.rookie.payment.entity.Product;
import org.rookie.payment.mapper.ProductMapper;
import org.rookie.payment.service.IProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author whj
 * @since 2022-06-10
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

}
