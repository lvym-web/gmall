package com.atguigu.gmall.api.service;

import com.atguigu.gmall.api.bean.OmsOrder;

public interface OrderService {
    String getTradeCode(String memberId);

    String checkTradeCode(String memberId, String tradeCode);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderInfo(String outTradeNo);

    void updateOrder(String out_trade_no);
}
