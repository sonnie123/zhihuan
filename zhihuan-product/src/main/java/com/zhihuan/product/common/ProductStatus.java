package com.zhihuan.product.common;

/**
 * 商品状态常量
 */
public final class ProductStatus {

    private ProductStatus() {
    }

    public static final int DRAFT = 1;
    /** 审核中 */
    public static final int AUDITING = 2;
    /** 在售 */
    public static final int ON_SALE = 3;
    /** 已售 */
    public static final int SOLD = 4;
    /** 下架 */
    public static final int OFF_SHELF = 5;
    /** 违规 */
    public static final int ILLEGAL = 6;
}