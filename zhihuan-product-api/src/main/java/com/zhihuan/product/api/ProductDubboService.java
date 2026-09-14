package com.zhihuan.product.api;

import com.zhihuan.product.api.vo.OnSaleProductVO;
import com.zhihuan.product.api.vo.ProductSkuVO;

import java.util.Collection;
import java.util.List;

/**
 * 商品 Dubbo 接口：供 trade / search / recommend 等其它微服务跨服务调用。
 * 放置于独立 API 模块 zhihuan-product-api，避免服务间依赖整个业务 jar。
 */
public interface ProductDubboService {

    /**
     * 按 ID 查询在售商品详情（含 SKU）。仅返回 status=3 在售商品，否则返回 null。
     */
    OnSaleProductVO getOnSaleById(Long productId);

    /**
     * 批量查询在售商品
     */
    List<OnSaleProductVO> listOnSaleByIds(Collection<Long> productIds);

    /**
     * 校验商品是否在售
     */
    boolean isOnSale(Long productId);

    /**
     * 锁定 SKU 库存（下单时调用），返回是否成功
     */
    boolean lockStock(Long skuId, int quantity);

    /**
     * 释放 SKU 已锁定库存（取消订单时调用）
     */
    void unlockStock(Long skuId, int quantity);

    /**
     * 扣减 SKU 库存（支付成功后正式扣减，需先锁定），返回是否成功
     */
    boolean reduceStock(Long skuId, int quantity);

    /**
     * 查询 SKU 详情
     */
    ProductSkuVO getSkuById(Long skuId);
}