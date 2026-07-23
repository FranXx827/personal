package com.ecommerce.modules.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.modules.category.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
