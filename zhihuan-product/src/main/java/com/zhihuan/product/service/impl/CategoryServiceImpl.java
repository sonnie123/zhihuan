package com.zhihuan.product.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhihuan.product.entity.Category;
import com.zhihuan.product.mapper.CategoryMapper;
import com.zhihuan.product.service.CategoryService;
import com.zhihuan.product.vo.CategoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品类目服务实现。类目为低频数据，树与名称映射本地 Caffeine 缓存。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    /** 名称缓存 key=categoryId -> name */
    private final Cache<Long, String> nameCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(1000)
        .build();

    /** 树缓存（单 key） */
    private final Cache<String, List<CategoryVO>> treeCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(10)
        .build();

    @Override
    public List<CategoryVO> listTree() {
        List<CategoryVO> cached = treeCache.getIfPresent("tree");
        if (cached != null) {
            return cached;
        }
        List<Category> all = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
            .eq(Category::getStatus, 1)
            .orderByAsc(Category::getSort));
        List<CategoryVO> list = buildTree(all);
        treeCache.put("tree", list);
        return list;
    }

    private List<CategoryVO> buildTree(List<Category> all) {
        Map<Long, CategoryVO> nodeMap = all.stream().collect(Collectors.toMap(
            Category::getId,
            c -> {
                CategoryVO vo = new CategoryVO();
                vo.setId(c.getId());
                vo.setParentId(c.getParentId());
                vo.setName(c.getName());
                vo.setLevel(c.getLevel());
                vo.setIcon(c.getIcon());
                vo.setSort(c.getSort());
                return vo;
            }));
        List<CategoryVO> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryVO vo = nodeMap.get(c.getId());
            if (c.getParentId() == null || c.getParentId() == 0) {
                roots.add(vo);
            } else {
                CategoryVO parent = nodeMap.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        roots.sort(Comparator.comparing(CategoryVO::getSort));
        return roots;
    }

    @Override
    public String getName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        String name = nameCache.getIfPresent(categoryId);
        if (name != null) {
            return name;
        }
        Category c = categoryMapper.selectById(categoryId);
        if (c != null) {
            nameCache.put(categoryId, c.getName());
            return c.getName();
        }
        return null;
    }
}