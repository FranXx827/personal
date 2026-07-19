package com.ecommerce.modules.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.modules.cart.dto.CartAddRequest;
import com.ecommerce.modules.cart.dto.CartUpdateRequest;
import com.ecommerce.modules.cart.dto.CartVO;
import com.ecommerce.modules.cart.entity.CartItem;
import com.ecommerce.modules.cart.mapper.CartItemMapper;
import com.ecommerce.modules.cart.service.CartService;
import com.ecommerce.modules.product.entity.Product;
import com.ecommerce.modules.product.entity.Sku;
import com.ecommerce.modules.product.mapper.ProductMapper;
import com.ecommerce.modules.product.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;

    @Override
    public List<CartVO> getCart(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .orderByDesc(CartItem::getCreatedAt));

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = items.stream()
                .map(CartItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> skuIds = items.stream()
                .map(CartItem::getSkuId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Sku> skuMap = skuMapper.selectBatchIds(skuIds)
                .stream().collect(Collectors.toMap(Sku::getId, s -> s));

        return items.stream().map(item -> {
            Product product = productMap.get(item.getProductId());
            Sku sku = skuMap.get(item.getSkuId());

            CartVO vo = new CartVO();
            vo.setId(item.getId());
            vo.setSkuId(item.getSkuId());
            vo.setProductId(item.getProductId());
            vo.setProductTitle(product != null ? product.getTitle() : null);
            vo.setProductImage(product != null ? product.getMainImage() : null);
            vo.setSpecs(sku != null ? sku.getSpecs() : null);
            vo.setPrice(sku != null ? sku.getPrice() : (product != null ? product.getPrice() : null));
            vo.setQuantity(item.getQuantity());
            vo.setSelected(item.getSelected());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(CartAddRequest req, Long userId) {
        Sku sku = skuMapper.selectById(req.skuId());
        if (sku == null) {
            throw new ResourceNotFoundException("SKU", req.skuId());
        }

        CartItem existing = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getSkuId, req.skuId())
                        .last("LIMIT 1"));

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + req.quantity());
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setSkuId(req.skuId());
            item.setProductId(sku.getProductId());
            item.setQuantity(req.quantity());
            item.setSelected(true);
            cartItemMapper.insert(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long itemId, CartUpdateRequest req, Long userId) {
        CartItem item = verifyOwnership(itemId, userId);

        if (req.quantity() != null) {
            item.setQuantity(req.quantity());
        }
        if (req.selected() != null) {
            item.setSelected(req.selected());
        }
        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId, Long userId) {
        CartItem item = verifyOwnership(itemId, userId);
        cartItemMapper.deleteById(item.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }

    /**
     * 校验购物车项归属，不存在或不属于当前用户则抛异常
     */
    private CartItem verifyOwnership(Long itemId, Long userId) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null) {
            throw new ResourceNotFoundException("购物车项", itemId);
        }
        if (!Objects.equals(item.getUserId(), userId)) {
            throw new ResourceNotFoundException("购物车项", itemId);
        }
        return item;
    }
}
