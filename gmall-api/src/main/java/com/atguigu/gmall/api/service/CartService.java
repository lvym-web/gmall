package com.atguigu.gmall.api.service;

import com.atguigu.gmall.api.bean.OmsCartItem;

import java.util.List;

public interface CartService {
    OmsCartItem getCartByMem(String memberId, String skuId);

    void addCart(OmsCartItem omsCartItem);

    void updateCart(OmsCartItem omsCartItemFromDB);

    void flushCartCache(String memberId);

    List<OmsCartItem> getCartList(String memberId);

    void updateCartList(OmsCartItem omsCartItem);
}
