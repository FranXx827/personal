package com.ecommerce.modules.cart.service;

import com.ecommerce.modules.cart.dto.CartAddRequest;
import com.ecommerce.modules.cart.dto.CartUpdateRequest;
import com.ecommerce.modules.cart.dto.CartVO;

import java.util.List;

public interface CartService {

    List<CartVO> getCart(Long userId);

    void addItem(CartAddRequest req, Long userId);

    /**
     * 更新购物车项（数量/选中状态）
     */
    void updateItem(Long itemId, CartUpdateRequest req, Long userId);

    void deleteItem(Long itemId, Long userId);

    void clearCart(Long userId);
}
