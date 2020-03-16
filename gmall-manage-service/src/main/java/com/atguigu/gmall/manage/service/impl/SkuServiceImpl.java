package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.api.bean.PmsSkuAttrValue;
import com.atguigu.gmall.api.bean.PmsSkuImage;
import com.atguigu.gmall.api.bean.PmsSkuInfo;
import com.atguigu.gmall.api.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.api.service.SkuService;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    private PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    private PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    private PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        // 插入skuInfo
        int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();

        // 插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        // 插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
       pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        // 插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }


    }

    //前台sku
    @Override
    public PmsSkuInfo getItemInfoById(String skuId) {

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        //查询小图片
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);

        List<PmsSkuImage> select = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo1.setSkuImageList(select);
        return pmsSkuInfo1;
    }
    //前台sku  redis
    @Override
    public PmsSkuInfo getskuById(String skuId, String remoteAddr) {
        System.out.println("ip为"+remoteAddr+"的帅哥美女"+Thread.currentThread().getName()+"进入商品详情页面");
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        // 链接缓存
        Jedis jedis = redisUtil.getJedis();
        // 查询缓存
        String skuKey = "sku:" + skuId + ":info";
        String skuJson = jedis.get(skuKey);

        if (StringUtils.isNotBlank(skuJson)) {//if(skuJson!=null&&!skuJson.equals(""))
            System.out.println("ip为"+remoteAddr+"的帅哥美女"+Thread.currentThread().getName()+"进入缓存");
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        } else {
            System.out.println("ip为"+remoteAddr+"的帅哥美女"+Thread.currentThread().getName()+"进入开始查询数据库");
            // 如果缓存中没有，查询mysql

            // 设置分布式锁
            String token = UUID.randomUUID().toString();
            String OK = jedis.set("sku:" + skuId + ":lock", token, "nx", "px", 10*1000);
            //拿到锁就查询
            if(StringUtils.isNotBlank(OK)&&OK.equals("OK")){
                System.out.println("ip为"+remoteAddr+"的帅哥美女"+Thread.currentThread().getName()+"拿到锁");
                pmsSkuInfo = getItemInfoById(skuId);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (pmsSkuInfo != null) {

                    // mysql查询结果存入redis
                    jedis.set("sku:" + skuId + ":info", JSON.toJSONString(pmsSkuInfo));

                }else {
                    System.out.println("ip为"+remoteAddr+"的帅哥美女"+Thread.currentThread().getName()+"查询null");
                    //数据库不存在就设置空/null给redis，防止数据库穿透
                    jedis.setex("sku:"+skuId+":info",60*3,JSON.toJSONString(""));

                }
                System.out.println("ip为"+remoteAddr+"的帅哥美女"+Thread.currentThread().getName()+"释放锁");
                //释放锁  根据key匹配vulue，匹配上了就删除锁（防止误删其他线程的锁）

                  String lockToken = jedis.get("sku:" + skuId + ":lock");
//                if(StringUtils.isNotBlank(lockToken)&&lockToken.equals(token)){
//                    jedis.del("sku:" + skuId + ":lock");
//                }
                //对比防重删令牌 lua
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                  jedis.eval(script, Collections.singletonList(lockToken),
                        Collections.singletonList(token));



            }else {
                //没拿到锁就自旋
                System.out.println("ip为"+remoteAddr+"的帅哥美女"+Thread.currentThread().getName()+"开始自旋");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getskuById(skuId, remoteAddr);
            }


        }
        jedis.close();
        return pmsSkuInfo;
    }

    //  查询所有  Sku 数据
    @Override
    public List<PmsSkuInfo> getAllSku() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>");
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
        System.out.println(">>>>>>>>>>>>>"+pmsSkuInfos);
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String id = pmsSkuInfo.getId();

            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(id);

            List<PmsSkuAttrValue> select = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);

            pmsSkuInfo.setSkuAttrValueList(select);


        }


        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpuId(String productId) {
        List<PmsSkuInfo> pmsSkuInfos= pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpuId(productId);
        return pmsSkuInfos;
    }
    //验价
    @Override
    public boolean checkPrice(String productSkuId, BigDecimal price) {
        boolean check=false;

        PmsSkuInfo pmsSkuInfo=new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.select(pmsSkuInfo);
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            if (skuInfo.getPrice().compareTo(price)==0){
                check=true;
            }
        }
        return check;
    }
}
