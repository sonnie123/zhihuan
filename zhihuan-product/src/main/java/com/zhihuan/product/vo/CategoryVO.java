package com.zhihuan.product.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 类目树节点
 */
@Data
public class CategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long parentId;

    private String name;

    private Integer level;

    private String icon;

    private Integer sort;

    private List<CategoryVO> children = new ArrayList<>();
}