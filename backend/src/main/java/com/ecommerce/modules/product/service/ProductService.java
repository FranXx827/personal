package com.ecommerce.modules.product.service;

import com.ecommerce.common.response.PageResult;
import com.ecommerce.modules.product.dto.*;

import java.util.List;

public interface ProductService {

    PageResult<ProductListVO> searchProducts(ProductPageRequest req);

    /**
     * 获取商品详情（含SKU列表）
     */
    ProductDetailVO getProductDetail(Long id);

    List<ProductListVO> getHotProducts(int limit);

    Long createProduct(ProductCreateRequest req, Long merchantId);
}
