package org.rookie.payment.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.rookie.payment.entity.OrderInfo;
import org.rookie.payment.entity.Product;
import org.rookie.payment.service.IOrderInfoService;
import org.rookie.payment.service.IProductService;
import org.rookie.payment.vo.RespBean;
import org.springframework.web.bind.annotation.GetMapping;
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
@Api(tags = "商品管理接口")
@RestController
@RequestMapping("/api/product")
public class ProductController {
    @Resource
    private IProductService productService;
    @ApiOperation("产品列表")
    @GetMapping("/list")
    public RespBean<Product> getProductList(){
        List<Product> products = productService.list();
        Map<String,List<Product>> respMap = new HashMap();
        respMap.put("productList",products);
        return new RespBean(200,"success",respMap);
    }
}
