package com.atguigu.gmall.api.service;

import com.atguigu.gmall.api.bean.PmsBaseCatalog1;
import com.atguigu.gmall.api.bean.PmsBaseCatalog2;
import com.atguigu.gmall.api.bean.PmsBaseCatalog3;

import java.util.List;

public interface CatalogService {

    List<PmsBaseCatalog1> pmsBaseCatalog1();
    List<PmsBaseCatalog2> pmsBaseCatalog2(String catalog1Id);
    List<PmsBaseCatalog3> pmsBaseCatalog3(String catalog2Id);
}
