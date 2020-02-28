package com.atguigu.gmall.api.service;

import com.atguigu.gmall.api.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.api.bean.PmsBaseAttrValue;
import com.atguigu.gmall.api.bean.PmsBaseSaleAttr;

import java.util.List;

public interface AttrService {
    String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo);
    List<PmsBaseAttrInfo> attrInfoList(String catalog3Id);


    List<PmsBaseAttrValue> getAttrValueList(String attrId);

    List<PmsBaseSaleAttr> baseSaleAttrList();
}
