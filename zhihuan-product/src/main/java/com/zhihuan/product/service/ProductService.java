package com.zhihuan.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhihuan.product.dto.ProductQueryDTO;
import com.zhihuan.product.dto.ProductSaveDTO;
import com.zhihuan.product.api.vo.OnSaleProductVO;
import com.zhihuan.product.vo.ProductVO;

import java.util.Collection;
import java.util.List;

/**
 * 商品服务：CRUD + 状态机 + 收藏 + 库存
 */
public interface ProductService {

    /** 创建草稿，返回商品ID */
    Long create(Long sellerId, ProductSaveDTO dto);

    /** 编辑草稿 */
    void update(Long sellerId, Long productId, ProductSaveDTO dto);

    /** 删除草稿/下架商品 */
    void delete(Long sellerId, Long productId);

    /** 提交审核：草稿 → 审核中 */
    void submitAudit(Long sellerId, Long productId);

    /** 审核通过：审核中 → 在售 */
    void auditPass(Long productId);

    /** 审核拒绝：审核中 → 草稿 */
    void auditReject(Long productId, String reason);

    /** 下架：在售 → 下架 */
    void offShelf(Long sellerId, Long productId);

    /** 售出：在售 → 已售（trade 调用） */
    void markSold(Long productId);

    /** 在售商品分页列表 */
    IPage<ProductVO> pageOnSale(ProductQueryDTO query);

    /** 商品详情（当前用户可传 null 表示游客） */
    ProductVO detail(Long productId, Long currentUserId);

    /** 收藏/取消收藏 */
    void favorite(Long userId, Long productId, boolean doFav);

    /** 我的收藏分页 */
    IPage<ProductVO> pageMyFavorite(Long userId, long pageNum, long pageSize);

    /** 卖家商品分页 */
    IPage<ProductVO> pageBySeller(Long sellerId, long pageNum, long pageSize);

    /** Dubbo：查询在售商品详情 */
    OnSaleProductVO getOnSaleById(Long productId);

    /** Dubbo：批量查询在售商品 */
    List<OnSaleProductVO> listOnSaleByIds(Collection<Long> productIds);

    /** Dubbo：是否在售 */
    boolean isOnSale(Long productId);

    /** Dubbo：锁定库存 */
    boolean lockStock(Long skuId, int quantity);

    /** Dubbo：释放锁定库存 */
    void unlockStock(Long skuId, int quantity);

    /** Dubbo：扣减库存 */
    boolean reduceStock(Long skuId, int quantity);
}