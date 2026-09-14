package com.zhihuan.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhihuan.common.result.Result;
import com.zhihuan.product.dto.ProductQueryDTO;
import com.zhihuan.product.dto.ProductSaveDTO;
import com.zhihuan.product.service.ProductService;
import com.zhihuan.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口（公开浏览 + 卖家管理）
 */
@Tag(name = "商品")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "在售商品分页")
    @GetMapping("/page")
    public Result<IPage<ProductVO>> pageOnSale(@Valid ProductQueryDTO query) {
        return Result.success(productService.pageOnSale(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{productId}")
    public Result<ProductVO> detail(@PathVariable Long productId,
                                    @RequestParam(required = false) Long userId) {
        return Result.success(productService.detail(productId, userId));
    }

    @Operation(summary = "卖家：创建草稿")
    @PostMapping("/seller")
    public Result<Long> create(@RequestParam Long sellerId,
                               @Valid @RequestBody ProductSaveDTO dto) {
        return Result.success(productService.create(sellerId, dto));
    }

    @Operation(summary = "卖家：编辑草稿")
    @PutMapping("/seller/{productId}")
    public Result<Void> update(@RequestParam Long sellerId,
                               @PathVariable Long productId,
                               @Valid @RequestBody ProductSaveDTO dto) {
        productService.update(sellerId, productId, dto);
        return Result.success();
    }

    @Operation(summary = "卖家：提交审核")
    @PostMapping("/seller/{productId}/submit")
    public Result<Void> submit(@RequestParam Long sellerId, @PathVariable Long productId) {
        productService.submitAudit(sellerId, productId);
        return Result.success();
    }

    @Operation(summary = "审核通过（审计服务/临时）")
    @PostMapping("/approve/{productId}")
    public Result<Void> approve(@PathVariable Long productId) {
        productService.auditPass(productId);
        return Result.success();
    }

    @Operation(summary = "卖家：下架")
    @PostMapping("/seller/{productId}/off-shelf")
    public Result<Void> offShelf(@RequestParam Long sellerId, @PathVariable Long productId) {
        productService.offShelf(sellerId, productId);
        return Result.success();
    }

    @Operation(summary = "卖家：删除商品")
    @DeleteMapping("/seller/{productId}")
    public Result<Void> delete(@RequestParam Long sellerId, @PathVariable Long productId) {
        productService.delete(sellerId, productId);
        return Result.success();
    }

    @Operation(summary = "卖家：我的商品")
    @GetMapping("/seller")
    public Result<IPage<ProductVO>> myProducts(@RequestParam Long sellerId,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(productService.pageBySeller(sellerId, pageNum, pageSize));
    }
}