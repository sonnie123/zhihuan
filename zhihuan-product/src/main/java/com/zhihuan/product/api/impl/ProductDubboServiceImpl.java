package com.zhihuan.product.api.impl;

import com.zhihuan.product.api.ProductDubboService;
import com.zhihuan.product.api.vo.OnSaleProductVO;
import com.zhihuan.product.api.vo.ProductSkuVO;
import com.zhihuan.product.entity.ProductSku;
import com.zhihuan.product.mapper.ProductSkuMapper;
import com.zhihuan.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.Collection;
import java.util.List;

/**
 * 商品 Dubbo 服务实现
 */
@DubboService
@RequiredArgsConstructor
public class ProductDubboServiceImpl implements ProductDubboService {

    private final ProductService productService;
    private final ProductSkuMapper productSkuMapper;

    @Override
    public OnSaleProductVO getOnSaleById(Long productId) {
        return productService.getOnSaleById(productId);
    }

    @Override
    public List<OnSaleProductVO> listOnSaleByIds(Collection<Long> productIds) {
        return productService.listOnSaleByIds(productIds);
    }

    @Override
    public boolean isOnSale(Long productId) {
        return productService.isOnSale(productId);
    }

    @Override
    public boolean lockStock(Long skuId, int quantity) {
        return productService.lockStock(skuId, quantity);
    }

    @Override
    public void unlockStock(Long skuId, int quantity) {
        productService.unlockStock(skuId, quantity);
    }

    @Override
    public boolean reduceStock(Long skuId, int quantity) {
        return productService.reduceStock(skuId, quantity);
    }

    @Override
    public ProductSkuVO getSkuById(Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            return null;
        }
        ProductSkuVO vo = new ProductSkuVO();
        BeanUtils.copyProperties(sku, vo);
        return vo;
    }
}