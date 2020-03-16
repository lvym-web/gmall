package com.atguigu.gmall.api.service;

import com.atguigu.gmall.api.bean.PmsSkuInfo;

import java.math.BigDecimal;
import java.util.List;

public interface SkuService {
    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getItemInfoById(String skuId);

    List<PmsSkuInfo> getSkuSaleAttrValueListBySpuId(String productId);

    PmsSkuInfo getskuById(String skuId, String remoteAddr);
    List<PmsSkuInfo> getAllSku();

    boolean checkPrice(String productSkuId, BigDecimal price);
}
