package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.api.bean.PmsProductSaleAttr;
import com.atguigu.gmall.api.bean.PmsSkuInfo;
import com.atguigu.gmall.api.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.api.service.SkuService;
import com.atguigu.gmall.api.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
@CrossOrigin
public class ItemController {
    @Reference
    private SkuService skuService;
    @Reference
    private SpuService spuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap, HttpServletRequest request) {

        String remoteAddr = request.getRemoteAddr();//获得IP

     //   request.getHeaders("");//获得使用nginx的IP

        PmsSkuInfo pmsSkuInfo = skuService.getskuById(skuId,remoteAddr);
        modelMap.put("skuInfo",pmsSkuInfo);

        List<PmsProductSaleAttr> pmsProductSaleAttrs=spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getId(),pmsSkuInfo.getProductId());
        modelMap.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);
        // 查询当前sku的spu的其他sku的集合的hash表
        List<PmsSkuInfo> pmsSkuInfos=skuService.getSkuSaleAttrValueListBySpuId(pmsSkuInfo.getProductId());

        HashMap<String, String> hashMap = new HashMap<>();
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String k="";
            String v=skuInfo.getId();

            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                k+=pmsSkuSaleAttrValue.getSaleAttrValueId()+"|";
            }
            hashMap.put(k,v);
        }
        // 将sku的销售属性hash表放到页面
        String jsonString = JSON.toJSONString(hashMap);
        modelMap.put("skuSaleAttrHashJsonStr",jsonString);

        return "item";
    }
}
