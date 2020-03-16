package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.api.bean.OmsCartItem;
import tk.mybatis.mapper.common.Mapper;

public interface OmsCartItemMapper extends Mapper<OmsCartItem> {
    void updateCartList(OmsCartItem omsCartItem);
}
