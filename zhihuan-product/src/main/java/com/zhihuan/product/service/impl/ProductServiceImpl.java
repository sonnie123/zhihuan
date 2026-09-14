package com.zhihuan.product.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhihuan.common.exception.BizException;
import com.zhihuan.common.result.ResultCode;
import com.zhihuan.product.api.vo.OnSaleProductVO;
import com.zhihuan.product.api.vo.ProductSkuVO;
import com.zhihuan.product.common.ProductStatus;
import com.zhihuan.product.dto.ProductQueryDTO;
import com.zhihuan.product.dto.ProductSaveDTO;
import com.zhihuan.product.dto.SkuItemDTO;
import com.zhihuan.product.entity.ProductFavorite;
import com.zhihuan.product.entity.ProductMain;
import com.zhihuan.product.entity.ProductSku;
import com.zhihuan.product.entity.ProductTag;
import com.zhihuan.product.event.ProductPublishedEvent;
import com.zhihuan.product.mapper.ProductFavoriteMapper;
import com.zhihuan.product.mapper.ProductMainMapper;
import com.zhihuan.product.mapper.ProductSkuMapper;
import com.zhihuan.product.mapper.ProductTagMapper;
import com.zhihuan.product.service.CategoryService;
import com.zhihuan.product.service.ProductService;
import com.zhihuan.product.vo.ProductVO;
import com.zhihuan.product.vo.SkuVO;
import com.zhihuan.user.api.UserDubboService;
import com.zhihuan.user.vo.UserBasicVO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMainMapper productMainMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductTagMapper productTagMapper;
    private final ProductFavoriteMapper productFavoriteMapper;
    private final CategoryService categoryService;

    @DubboReference(check = false)
    private UserDubboService userDubboService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /** 在售详情一级本地缓存（Caffeine） */
    private final Cache<Long, ProductVO> detailCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(5))
        .maximumSize(2000)
        .build();

    public ProductServiceImpl(ProductMainMapper productMainMapper,
                              ProductSkuMapper productSkuMapper,
                              ProductTagMapper productTagMapper,
                              ProductFavoriteMapper productFavoriteMapper,
                              CategoryService categoryService) {
        this.productMainMapper = productMainMapper;
        this.productSkuMapper = productSkuMapper;
        this.productTagMapper = productTagMapper;
        this.productFavoriteMapper = productFavoriteMapper;
        this.categoryService = categoryService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long sellerId, ProductSaveDTO dto) {
        long productId = IdUtil.getSnowflakeNextId();
        ProductMain product = new ProductMain();
        product.setId(productId);
        product.setSellerId(sellerId);
        product.setCategoryId(dto.getCategoryId());
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setCoverImage(dto.getCoverImage());
        product.setImages(JSONUtil.toJsonStr(dto.getImages()));
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCondition(dto.getCondition() == null ? 1 : dto.getCondition());
        product.setStatus(ProductStatus.DRAFT);
        product.setViewCount(0L);
        product.setFavoriteCount(0L);
        product.setAiAuditStatus(0);
        productMainMapper.insert(product);

        saveSkus(productId, dto.getSkus());
        saveTags(productId, dto.getTags());
        return productId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long sellerId, Long productId, ProductSaveDTO dto) {
        ProductMain product = requireOwned(sellerId, productId);
        if (!Objects.equals(product.getStatus(), ProductStatus.DRAFT)) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅草稿状态可编辑");
        }
        product.setCategoryId(dto.getCategoryId());
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setCoverImage(dto.getCoverImage());
        product.setImages(JSONUtil.toJsonStr(dto.getImages()));
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCondition(dto.getCondition());
        productMainMapper.updateById(product);

        // 重建 SKU / 标签（简单起见先清后插）
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>()
            .eq(ProductSku::getProductId, productId));
        productTagMapper.delete(new LambdaQueryWrapper<ProductTag>()
            .eq(ProductTag::getProductId, productId));
        saveSkus(productId, dto.getSkus());
        saveTags(productId, dto.getTags());
        detailCache.invalidate(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long sellerId, Long productId) {
        ProductMain product = requireOwned(sellerId, productId);
        if (!Objects.equals(product.getStatus(), ProductStatus.DRAFT)
            && !Objects.equals(product.getStatus(), ProductStatus.OFF_SHELF)) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅草稿/下架商品可删除");
        }
        productMainMapper.deleteById(productId);
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>()
            .eq(ProductSku::getProductId, productId));
        productTagMapper.delete(new LambdaQueryWrapper<ProductTag>()
            .eq(ProductTag::getProductId, productId));
        detailCache.invalidate(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAudit(Long sellerId, Long productId) {
        ProductMain product = requireOwned(sellerId, productId);
        // 草稿或下架状态均可提交审核（下架商品重新上架需再次审核）
        if (!Objects.equals(product.getStatus(), ProductStatus.DRAFT)
            && !Objects.equals(product.getStatus(), ProductStatus.OFF_SHELF)) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅草稿或下架状态可提交审核");
        }
        ProductMain update = new ProductMain();
        update.setId(productId);
        update.setStatus(ProductStatus.AUDITING);
        productMainMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPass(Long productId) {
        ProductMain product = requireJc(productId);
        if (!Objects.equals(product.getStatus(), ProductStatus.AUDITING)) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅审核中状态可通过");
        }
        ProductMain update = new ProductMain();
        update.setId(productId);
        update.setStatus(ProductStatus.ON_SALE);
        update.setPublishTime(LocalDateTime.now());
        productMainMapper.updateById(update);
        detailCache.invalidate(productId);
        eventPublisher.publishEvent(new ProductPublishedEvent(productId, product.getSellerId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReject(Long productId, String reason) {
        ProductMain product = requireJc(productId);
        if (!Objects.equals(product.getStatus(), ProductStatus.AUDITING)) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅审核中状态可拒绝");
        }
        ProductMain update = new ProductMain();
        update.setId(productId);
        update.setStatus(ProductStatus.DRAFT);
        productMainMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offShelf(Long sellerId, Long productId) {
        ProductMain product = requireOwned(sellerId, productId);
        if (!Objects.equals(product.getStatus(), ProductStatus.ON_SALE)) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅在售商品可下架");
        }
        ProductMain update = new ProductMain();
        update.setId(productId);
        update.setStatus(ProductStatus.OFF_SHELF);
        productMainMapper.updateById(update);
        detailCache.invalidate(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSold(Long productId) {
        ProductMain product = requireJc(productId);
        if (!Objects.equals(product.getStatus(), ProductStatus.ON_SALE)) {
            throw new BizException(ResultCode.BIZ_ERROR, "仅在售商品可标记售出");
        }
        ProductMain update = new ProductMain();
        update.setId(productId);
        update.setStatus(ProductStatus.SOLD);
        productMainMapper.updateById(update);
        detailCache.invalidate(productId);
    }

    @Override
    public IPage<ProductVO> pageOnSale(ProductQueryDTO query) {
        LambdaQueryWrapper<ProductMain> wrapper = new LambdaQueryWrapper<ProductMain>()
            .eq(ProductMain::getStatus, ProductStatus.ON_SALE);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(ProductMain::getTitle, query.getKeyword());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(ProductMain::getCategoryId, query.getCategoryId());
        }
        if (query.getCondition() != null) {
            wrapper.eq(ProductMain::getCondition, query.getCondition());
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(ProductMain::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(ProductMain::getPrice, query.getMaxPrice());
        }
        switch (query.getSort() == null ? 1 : query.getSort()) {
            case 2 -> wrapper.orderByAsc(ProductMain::getPrice);
            case 3 -> wrapper.orderByDesc(ProductMain::getPrice);
            case 4 -> wrapper.orderByDesc(ProductMain::getViewCount);
            default -> wrapper.orderByDesc(ProductMain::getPublishTime);
        }
        IPage<ProductMain> page = productMainMapper.selectPage(
            new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return page.convert(this::toProductVO);
    }

    @Override
    public ProductVO detail(Long productId, Long currentUserId) {
        ProductMain product = productMainMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商品不存在");
        }
        // 非在售商品仅卖家可见
        if (!Objects.equals(product.getStatus(), ProductStatus.ON_SALE)
            && !Objects.equals(product.getSellerId(), currentUserId)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商品不存在或未上架");
        }

        ProductVO cached = detailCache.getIfPresent(productId);
        ProductVO vo;
        if (cached != null && cached.getStatus() != null
            && Objects.equals(cached.getStatus(), ProductStatus.ON_SALE)) {
            vo = copy(cached);
        } else {
            vo = buildDetailVO(product);
            if (Objects.equals(product.getStatus(), ProductStatus.ON_SALE)) {
                detailCache.put(productId, copy(vo));
            }
        }
        if (currentUserId != null) {
            ProductFavorite fav = productFavoriteMapper.selectOne(
                new LambdaQueryWrapper<ProductFavorite>()
                    .eq(ProductFavorite::getUserId, currentUserId)
                    .eq(ProductFavorite::getProductId, productId));
            vo.setFavorited(fav != null && Objects.equals(fav.getStatus(), 1));
        }
        // 浏览 +1（异步忽略失败）
        try {
            productMainMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductMain>()
                .eq(ProductMain::getId, productId)
                .setSql("view_count = view_count + 1"));
        } catch (Exception ignored) {
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long userId, Long productId, boolean doFav) {
        ProductMain product = productMainMapper.selectById(productId);
        if (product == null || !Objects.equals(product.getStatus(), ProductStatus.ON_SALE)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商品不存在或未上架");
        }
        if (Objects.equals(product.getSellerId(), userId)) {
            throw new BizException(ResultCode.BIZ_ERROR, "不能收藏自己的商品");
        }
        ProductFavorite fav = productFavoriteMapper.selectOne(
            new LambdaQueryWrapper<ProductFavorite>()
                .eq(ProductFavorite::getUserId, userId)
                .eq(ProductFavorite::getProductId, productId));
        if (doFav) {
            if (fav == null) {
                fav = new ProductFavorite();
                fav.setId(IdUtil.getSnowflakeNextId());
                fav.setUserId(userId);
                fav.setProductId(productId);
                fav.setStatus(1);
                productFavoriteMapper.insert(fav);
            } else if (!Objects.equals(fav.getStatus(), 1)) {
                fav.setStatus(1);
                productFavoriteMapper.updateById(fav);
            }
        } else {
            if (fav != null && Objects.equals(fav.getStatus(), 1)) {
                fav.setStatus(0);
                productFavoriteMapper.updateById(fav);
            }
        }
        int delta = doFav ? 1 : -1;
        try {
            productMainMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductMain>()
                .eq(ProductMain::getId, productId)
                .setSql("favorite_count = GREATEST(favorite_count + " + delta + ", 0)"));
        } catch (Exception ignored) {
        }
        // 收藏数变更后失效详情缓存，避免读到旧计数
        detailCache.invalidate(productId);
    }

    @Override
    public IPage<ProductVO> pageMyFavorite(Long userId, long pageNum, long pageSize) {
        IPage<ProductFavorite> favPage = productFavoriteMapper.selectPage(
            new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<ProductFavorite>()
                .eq(ProductFavorite::getUserId, userId)
                .eq(ProductFavorite::getStatus, 1)
                .orderByDesc(ProductFavorite::getCreateTime));
        List<Long> ids = favPage.getRecords().stream()
            .map(ProductFavorite::getProductId).collect(Collectors.toList());
        Map<Long, ProductMain> productMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(ids)) {
            productMainMapper.selectBatchIds(ids).forEach(p -> productMap.put(p.getId(), p));
        }
        List<ProductVO> records = favPage.getRecords().stream()
            .map(f -> toProductVO(productMap.get(f.getProductId())))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Page<ProductVO> result = new Page<>(pageNum, pageSize, favPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public IPage<ProductVO> pageBySeller(Long sellerId, long pageNum, long pageSize) {
        IPage<ProductMain> page = productMainMapper.selectPage(new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<ProductMain>()
                .eq(ProductMain::getSellerId, sellerId)
                .orderByDesc(ProductMain::getCreateTime));
        return page.convert(this::toProductVO);
    }

    // ---------------- Dubbo 实现 ----------------

    @Override
    public OnSaleProductVO getOnSaleById(Long productId) {
        ProductMain product = productMainMapper.selectById(productId);
        if (product == null || !Objects.equals(product.getStatus(), ProductStatus.ON_SALE)) {
            return null;
        }
        OnSaleProductVO vo = new OnSaleProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setSkus(listSkuVO(productId));
        return vo;
    }

    @Override
    public List<OnSaleProductVO> listOnSaleByIds(Collection<Long> productIds) {
        if (CollectionUtils.isEmpty(productIds)) {
            return Collections.emptyList();
        }
        return productIds.stream().map(this::getOnSaleById).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isOnSale(Long productId) {
        ProductMain product = productMainMapper.selectById(productId);
        return product != null && Objects.equals(product.getStatus(), ProductStatus.ON_SALE);
    }

    @Override
    public boolean lockStock(Long skuId, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        int rows = productSkuMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId)
                .ge(ProductSku::getStock, quantity)
                .setSql("stock = stock - " + quantity)
                .setSql("locked_stock = locked_stock + " + quantity));
        return rows > 0;
    }

    @Override
    public void unlockStock(Long skuId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        productSkuMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId)
                .ge(ProductSku::getLockedStock, quantity)
                .setSql("stock = stock + " + quantity)
                .setSql("locked_stock = locked_stock - " + quantity));
    }

    @Override
    public boolean reduceStock(Long skuId, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        int rows = productSkuMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId)
                .ge(ProductSku::getLockedStock, quantity)
                .setSql("locked_stock = locked_stock - " + quantity));
        return rows > 0;
    }

    // ---------------- 私有辅助 ----------------

    private ProductMain requireOwned(Long sellerId, Long productId) {
        ProductMain product = productMainMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商品不存在");
        }
        if (!Objects.equals(product.getSellerId(), sellerId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作该商品");
        }
        return product;
    }

    private ProductMain requireJc(Long productId) {
        ProductMain product = productMainMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "商品不存在");
        }
        return product;
    }

    private void saveSkus(Long productId, List<SkuItemDTO> skus) {
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        for (SkuItemDTO sku : skus) {
            ProductSku entity = new ProductSku();
            entity.setId(IdUtil.getSnowflakeNextId());
            entity.setProductId(productId);
            entity.setSkuName(sku.getSkuName());
            entity.setSkuValue(sku.getSkuValue());
            entity.setPrice(sku.getPrice());
            entity.setStock(sku.getStock() == null ? 0 : sku.getStock());
            entity.setLockedStock(0);
            productSkuMapper.insert(entity);
        }
    }

    private void saveTags(Long productId, List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        for (String tag : tags) {
            ProductTag entity = new ProductTag();
            entity.setId(IdUtil.getSnowflakeNextId());
            entity.setProductId(productId);
            entity.setTagName(tag);
            entity.setTagType(2);
            productTagMapper.insert(entity);
        }
    }

    private List<ProductSkuVO> listSkuVO(Long productId) {
        List<ProductSku> skus = productSkuMapper.selectList(
            new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId));
        return skus.stream().map(s -> {
            ProductSkuVO vo = new ProductSkuVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<SkuVO> listSkuView(Long productId) {
        List<ProductSku> skus = productSkuMapper.selectList(
            new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId));
        return skus.stream().map(s -> {
            SkuVO vo = new SkuVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<String> listTagNames(Long productId) {
        return productTagMapper.selectList(
                new LambdaQueryWrapper<ProductTag>().eq(ProductTag::getProductId, productId))
            .stream().map(ProductTag::getTagName).collect(Collectors.toList());
    }

    private ProductVO toProductVO(ProductMain product) {
        if (product == null) {
            return null;
        }
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        vo.setImages(parseImages(product.getImages()));
        vo.setCategoryName(categoryService.getName(product.getCategoryId()));
        vo.setTags(listTagNames(product.getId()));
        fillSeller(product.getSellerId(), vo::setSellerNickname);
        return vo;
    }

    private ProductVO buildDetailVO(ProductMain product) {
        ProductVO vo = toProductVO(product);
        vo.setSkus(listSkuView(product.getId()));
        return vo;
    }

    private void fillSeller(Long sellerId, java.util.function.Consumer<String> setter) {
        try {
            if (sellerId != null && userDubboService != null) {
                List<UserBasicVO> users = userDubboService.listBasicByIds(List.of(sellerId));
                if (!CollectionUtils.isEmpty(users) && users.get(0) != null) {
                    setter.accept(users.get(0).getNickname());
                }
            }
        } catch (Exception ignored) {
            // Dubbo 提供方不可用时降级为空昵称
        }
    }

    private List<String> parseImages(String images) {
        if (!StringUtils.hasText(images)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(images, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private ProductVO copy(ProductVO src) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(src, vo);
        vo.setImages(src.getImages() == null ? null : new ArrayList<>(src.getImages()));
        vo.setSkus(src.getSkus() == null ? null : src.getSkus().stream()
            .map(s -> { SkuVO c = new SkuVO(); BeanUtils.copyProperties(s, c); return c; })
            .collect(Collectors.toList()));
        vo.setTags(src.getTags() == null ? null : new ArrayList<>(src.getTags()));
        return vo;
    }
}