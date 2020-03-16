package com.atguigu.gmall.api.service;

import com.atguigu.gmall.api.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);

    void sendDelayPaymentResultCheckQueue(String outTradeNo,Integer count);

    Map<String, Object> checkAliPayPayment(String out_trade_no);
}
