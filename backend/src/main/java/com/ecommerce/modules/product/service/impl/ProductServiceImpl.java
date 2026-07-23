package com.ecommerce.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PageResult;
import com.ecommerce.modules.category.service.CategoryService;
import com.ecommerce.modules.merchant.entity.Merchant;
import com.ecommerce.modules.merchant.mapper.MerchantMapper;
import com.ecommerce.modules.product.client.AiAssistantClient;
import com.ecommerce.modules.product.dto.*;
import com.ecommerce.modules.product.entity.Product;
import com.ecommerce.modules.product.entity.Sku;
import com.ecommerce.modules.product.mapper.ProductMapper;
import com.ecommerce.modules.product.mapper.SkuMapper;
import com.ecommerce.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final MerchantMapper merchantMapper;
    private final AiAssistantClient aiAssistantClient;
    private final CategoryService categoryService;

    @Override
    public PageResult<ProductListVO> searchProducts(ProductPageRequest req) {
        int pageNum = req.page() != null ? req.page() : 1;
        int pageSize = req.pageSize() != null ? req.pageSize() : 20;

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(req.keyword())) {
            String kw = req.keyword().trim();
            if (kw.length() == 1) {
                // 单字符：ngram_token_size=2 无法匹配，降级为 LIKE 兜底
                wrapper.and(w -> w.like(Product::getTitle, kw)
                        .or().like(Product::getDescription, kw)
                        .or().like(Product::getSearchTags, kw));
            } else {
                // 多字符：走倒排索引（MATCH...AGAINST），利用 FULLTEXT + ngram 分词
                wrapper.apply("MATCH (title, description, search_tags) AGAINST ({0} IN BOOLEAN MODE)", kw);
            }
        }

        // 分类过滤（精确匹配单个分类ID）
        if (req.categoryId() != null) {
            wrapper.eq(Product::getCategoryId, req.categoryId());
        }

        // 分类过滤（名称 → 后端映射为多 ID，IN 查询）
        // LLM 只传品类名称，由 CategoryService 负责解析为 ID 列表
        if (StringUtils.isNotBlank(req.categoryName())) {
            List<Long> ids = categoryService.resolve(req.categoryName());
            if (!ids.isEmpty()) {
                wrapper.in(Product::getCategoryId, ids);
            } else {
                // 找不到该分类时，返回空结果
                wrapper.eq(Product::getId, 0); // 恒假条件
            }
        }

        // 仅查询上架商品（status = 0）
        wrapper.eq(Product::getStatus, 0);

        String sortBy = req.sortBy();
        boolean asc = !"desc".equalsIgnoreCase(req.sortDir());
        if (StringUtils.isNotBlank(sortBy)) {
            wrapper.orderBy(true, asc, Product::getPrice);
        } else {
            wrapper.orderByDesc(Product::getCreatedAt);
        }

        Page<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<ProductListVO> voList = page.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());

        return PageResult.of(voList, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public List<ProductListVO> getHotProducts(int limit) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 0)
                .orderByDesc(Product::getSales)
                .last("LIMIT " + limit);
        List<Product> products = productMapper.selectList(wrapper);
        return products.stream().map(this::toListVO).collect(Collectors.toList());
    }

    @Override
    public ProductDetailVO getProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new ResourceNotFoundException("商品", id);
        }
        return toDetailVO(product);
    }

    @Override
    public Long createProduct(ProductCreateRequest req, Long merchantId) {
        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setTitle(req.title());
        product.setDescription(req.description());
        product.setCategoryId(req.categoryId());
        product.setPrice(req.price());
        product.setMainImage(req.mainImage());

        // searchTags 为空时调用 AI 自动生成
        String tags = req.searchTags();
        if (StringUtils.isBlank(tags)) {
            // 优先调用 AI Assistant 生成标签
            tags = aiAssistantClient.generateTags(req.title(), req.description());
            // AI 失败时降级到规则引擎
            if (StringUtils.isBlank(tags)) {
                tags = autoGenerateTags(req.title(), req.description());
            }
        }
        product.setSearchTags(tags);

        product.setSales(0);
        product.setRating(new java.math.BigDecimal("5.00"));
        product.setStatus(0); // 默认上架

        productMapper.insert(product);
        return product.getId();
    }

    /**
     * 自动从标题和描述中提取搜索标签
     */
    private String autoGenerateTags(String title, String description) {
        // 1. 从标题提取有意义的词（分词）——去掉品牌/型号后的核心词
        // 2. 匹配预定义的通用标签映射
        // 如果提取不到足够关键词，补充几个通用标签兜底
        String text = (title + " " + (description != null ? description : "")).toLowerCase();

        java.util.Set<String> tags = new java.util.LinkedHashSet<>();
        String[] CATEGORY_TAGS = {"手机", "耳机", "手表", "电视", "电脑", "平板", "家电"};

        // 检查标题和描述中是否包含预定义品类词
        for (String cat : CATEGORY_TAGS) {
            if (text.contains(cat)) {
                tags.add(cat);
            }
        }

        // 通用标签：电子产品、数码
        tags.add("电子产品");

        // 如果实在太少，补充默认标签
        if (tags.size() <= 1) {
            tags.add("数码");
        }

        return String.join(",", tags);
    }

    /**
     * 将 Product 转为列表 VO（含商户名称）
     */
    private ProductListVO toListVO(Product product) {
        String merchantName = "";
        if (product.getMerchantId() != null) {
            Merchant merchant = merchantMapper.selectById(product.getMerchantId());
            if (merchant != null) {
                merchantName = merchant.getName();
            }
        }
        return ProductListVO.builder()
                .id(product.getId())
                .title(product.getTitle())
                .price(product.getPrice())
                .cover(product.getMainImage() != null ? product.getMainImage() : "")
                .merchantId(product.getMerchantId())
                .merchantName(merchantName)
                .sales(product.getSales() != null ? product.getSales() : 0)
                .rating(product.getRating() != null ? product.getRating() : new java.math.BigDecimal("5.00"))
                .build();
    }

    /**
     * 将 Product 实体转换为带 SKU 的详情 VO
     */
    private ProductDetailVO toDetailVO(Product product) {
        List<Sku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<Sku>().eq(Sku::getProductId, product.getId()));

        List<ProductDetailVO.SkuVO> skuVOs = skus.stream()
                .map(sku -> new ProductDetailVO.SkuVO(
                        sku.getId(),
                        sku.getSpecs(),
                        sku.getStock(),
                        sku.getPrice()))
                .collect(Collectors.toList());

        return new ProductDetailVO(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getCategoryId(),
                product.getPrice(),
                product.getMainImage(),
                skuVOs
        );
    }
}
