package com.ecommerce.modules.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.modules.category.entity.Category;
import com.ecommerce.modules.category.mapper.CategoryMapper;
import com.ecommerce.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分类映射服务 —— LLM 只输出品类名称，由本服务负责名称→ID 的精确/模糊映射。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 名称→节点的缓存索引（启动时构建，写入时失效）
     */
    private volatile Map<String, Category> nameIndex = null;

    /**
     * 全量分类列表缓存
     */
    private volatile List<Category> allCategories = null;

    // ========== 公开接口 ==========

    @Override
    public List<Long> resolve(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = categoryName.trim();

        // 1. 加载并构建索引
        Map<String, Category> index = getNameIndex();
        List<Category> all = getAllCategories();

        // 2. 精确匹配
        Category node = index.get(trimmed);
        if (node == null) {
            // 小写兜底
            node = index.get(trimmed.toLowerCase());
        }

        // 3. 模糊兜底
        if (node == null) {
            node = fuzzyMatch(trimmed, index);
        }

        if (node == null) {
            log.warn("category_resolve_not_found name={}", trimmed);
            return Collections.emptyList();
        }

        // 4. 收集当前节点 + 所有子节点 ID
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(node.getId());
        collectChildIds(node.getId(), all, ids);

        List<Long> result = new ArrayList<>(ids);
        log.info("category_resolved name={} -> ids={}", trimmed, result);
        return result;
    }

    @Override
    public List<Category> getTree() {
        List<Category> all = getAllCategories();
        // 按 parentId 分组
        Map<Long, List<Category>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        // 顶级分类
        return all.stream()
                .filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .peek(c -> c.setChildren(childrenMap.getOrDefault(c.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    // ========== 内部方法 ==========

    /**
     * 精确匹配不到时，做模糊兜底：包含关系
     */
    private Category fuzzyMatch(String name, Map<String, Category> index) {
        // 遍历所有节点名，找包含关系
        for (Map.Entry<String, Category> entry : index.entrySet()) {
            String key = entry.getKey();
            if (name.contains(key) || key.contains(name)) {
                log.info("category_fuzzy_match name={} matched={}", name, key);
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 递归收集指定节点的所有子节点 ID
     */
    private void collectChildIds(Long parentId, List<Category> all, Set<Long> ids) {
        for (Category c : all) {
            if (parentId.equals(c.getParentId())) {
                ids.add(c.getId());
                collectChildIds(c.getId(), all, ids);
            }
        }
    }

    /**
     * 构建或获取 名称→节点 索引（懒加载 + 缓存）
     */
    private Map<String, Category> getNameIndex() {
        if (nameIndex != null) {
            return nameIndex;
        }
        synchronized (this) {
            if (nameIndex != null) {
                return nameIndex;
            }
            List<Category> all = categoryMapper.selectList(null);
            Map<String, Category> index = new HashMap<>();
            for (Category c : all) {
                index.put(c.getName(), c);
                index.put(c.getName().toLowerCase(), c);
            }
            nameIndex = index;
            allCategories = all;
            log.info("category_index_built size={}", index.size());
            return index;
        }
    }

    /**
     * 获取全量分类列表（缓存）
     */
    private List<Category> getAllCategories() {
        if (allCategories != null) {
            return allCategories;
        }
        getNameIndex(); // 触发构建
        return allCategories;
    }
}
