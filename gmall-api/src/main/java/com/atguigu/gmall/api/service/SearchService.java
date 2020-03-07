package com.atguigu.gmall.api.service;

import com.atguigu.gmall.api.bean.PmsSearchParam;
import com.atguigu.gmall.api.bean.PmsSearchSkuInfo;

import java.util.List;

public interface SearchService {
    List<PmsSearchSkuInfo> getSearchList(PmsSearchParam pmsSearchParam);
}
