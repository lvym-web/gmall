package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.api.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.api.bean.PmsBaseAttrValue;
import com.atguigu.gmall.api.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.api.service.AttrService;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseSaleAttrMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AttrServiceImpl implements AttrService {
    @Autowired
    private PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Autowired
    private PmsBaseAttrValueMapper pmsBaseAttrValueMapper;
    @Autowired
    private PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;
    @Override
    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {

         if (StringUtils.isBlank(pmsBaseAttrInfo.getId())){
             //id==null,执行保存
             pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);//不插入null值
             List<PmsBaseAttrValue> attrValueList =pmsBaseAttrInfo.getAttrValueList();
             for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                 pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                 pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
             }
         }else {

             //id!=null 执行修改

             //属性修改
             Example example=new Example(PmsBaseAttrInfo.class);
             example.createCriteria().andEqualTo("id",pmsBaseAttrInfo.getId());
             pmsBaseAttrInfoMapper.updateByExampleSelective(pmsBaseAttrInfo,example);

             //删除pmsBaseAttrValue根据id
             PmsBaseAttrValue pmsBaseAttrValueDel = new PmsBaseAttrValue();
             pmsBaseAttrValueDel.setAttrId(pmsBaseAttrInfo.getId());
             pmsBaseAttrValueMapper.delete(pmsBaseAttrValueDel);
             //添加
             List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
             for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                 pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                 pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
             }

         }

        return "success";

    }
    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> select = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);

        for (PmsBaseAttrInfo baseAttrInfo : select) {
            List<PmsBaseAttrValue> pmsBaseAttrValues=new ArrayList<>();
            PmsBaseAttrValue pmsBaseAttrValue =new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(baseAttrInfo.getId());
            pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);


            baseAttrInfo.setAttrValueList(pmsBaseAttrValues);
        }

        return select;
    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        return pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
    }

    //卖家产品属性
    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return pmsBaseSaleAttrMapper.selectAll();
    }

    //查询平台属性
    @Override
    public List<PmsBaseAttrInfo> getAttrValueListByValueId(Set<String> set) {

        String join = StringUtils.join(set, ",");
        List<PmsBaseAttrInfo> pmsBaseAttrInfos= pmsBaseAttrInfoMapper.selectAttrValueListByValueId(join);
        return pmsBaseAttrInfos;
    }


}
