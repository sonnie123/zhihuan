package com.zhihuan.product.service;

import com.zhihuan.product.vo.CategoryVO;

import java.util.List;

/**
 * 商品类目服务
 */
public interface CategoryService {

    /** 类目树（启用状态） */
    List<CategoryVO> listTree();

    /** 类目名称映射 */
    String getName(Long categoryId);
}