package com.ecommerce.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.response.PageResult;
import com.ecommerce.modules.merchant.entity.Merchant;
import com.ecommerce.modules.merchant.mapper.MerchantMapper;
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

    @Override
    public PageResult<ProductListVO> searchProducts(ProductPageRequest req) {
        int pageNum = req.page() != null ? req.page() : 1;
        int pageSize = req.pageSize() != null ? req.pageSize() : 20;

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(req.keyword())) {
            wrapper.like(Product::getTitle, req.keyword());
        }

        // 分类过滤（包含子分类由调用层决定，这里只精确匹配）
        if (req.categoryId() != null) {
            wrapper.eq(Product::getCategoryId, req.categoryId());
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
        product.setSales(0);
        product.setRating(new java.math.BigDecimal("5.00"));
        product.setStatus(0); // 默认上架

        productMapper.insert(product);
        return product.getId();
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
