package com.zhihuan.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhihuan.common.result.Result;
import com.zhihuan.product.service.ProductService;
import com.zhihuan.product.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品收藏接口
 */
@Tag(name = "商品收藏")
@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final ProductService productService;

    @Operation(summary = "收藏商品")
    @PostMapping("/add")
    public Result<Void> add(@RequestParam Long userId, @RequestParam Long productId) {
        productService.favorite(userId, productId, true);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestParam Long userId, @RequestParam Long productId) {
        productService.favorite(userId, productId, false);
        return Result.success();
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/mine")
    public Result<IPage<ProductVO>> mine(@RequestParam Long userId,
                                         @RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(productService.pageMyFavorite(userId, pageNum, pageSize));
    }
}