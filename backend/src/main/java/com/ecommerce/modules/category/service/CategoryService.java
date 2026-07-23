package com.ecommerce.modules.category.service;

import com.ecommerce.modules.category.entity.Category;

import java.util.List;

public interface CategoryService {

    /**
     * 根据分类名称解析为分类ID列表（含该分类及其所有子分类）。
     * <p>
     * 这是 LLM → 后端 分层架构的核心接口：
     * LLM 只输出品类中文名称，由本服务负责 名称 → ID 的映射。
     * <p>
     * 解析策略：
     * 1. 精确匹配（覆盖 > 99% 的情况）
     * 2. 精确匹配不到时，做模糊兜底（包含关系）
     * 3. 找到节点后，返回该节点 ID + 所有子节点 ID
     *
     * @param categoryName 品类名称，如"手机"、"智能手机"、"降噪耳机"
     * @return 该分类及所有子分类的 ID 列表；找不到时返回空列表
     */
    List<Long> resolve(String categoryName);

    /**
     * 获取全部分类树（顶级→子级）
     */
    List<Category> getTree();

    /**
     * 根据ID获取分类
     */
    Category getById(Long id);
}
