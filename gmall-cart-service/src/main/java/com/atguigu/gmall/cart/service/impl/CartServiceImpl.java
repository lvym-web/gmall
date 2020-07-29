package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.api.bean.OmsCartItem;
import com.atguigu.gmall.api.service.CartService;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public OmsCartItem getCartByMem(String memberId, String skuId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);

        OmsCartItem omsCartItem1 = omsCartItemMapper.selectOne(omsCartItem);
        return omsCartItem1;
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        if (StringUtils.isNotBlank(omsCartItem.getMemberId())) {
            omsCartItemMapper.insertSelective(omsCartItem);
        }

    }

    @Override
    public void updateCart(OmsCartItem omsCartItem) {

        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id", omsCartItem.getId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem, example);
    }

    @Override
    public void flushCartCache(String memberId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> select = omsCartItemMapper.select(omsCartItem);
        //同步redis
        Jedis jedis = redisUtil.getJedis();



        Map<String, String> map = new HashMap<>();
        for (OmsCartItem cartItem : select) {
            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
            map.put(cartItem.getProductId(), JSON.toJSONString(cartItem));
        }
        jedis.del("user:" + memberId + ":cart");
        jedis.hmset("user:" + memberId + ":cart", map);
        jedis.close();

    }

    //已登录  查询购物车
    @Override
    public List<OmsCartItem> getCartList(String memberId) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            List<String> hvals = jedis.hvals("user:" + memberId + ":cart");

            for (String hval : hvals) {
                OmsCartItem omsCartItem = JSON.parseObject(hval, OmsCartItem.class);
                omsCartItems.add(omsCartItem);
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            jedis.close();
        }


        return omsCartItems;
    }


    //修改  carList
    @Override
    public void updateCartList(OmsCartItem omsCartItem) {
//        Example example = new Example(OmsCartItem.class);
//        example.createCriteria().andEqualTo("memberId", omsCartItem.getMemberId()).andEqualTo("productSkuId", omsCartItem.getProductSkuId());

        omsCartItemMapper.updateCartList(omsCartItem);

       //同步缓存
        flushCartCache(omsCartItem.getMemberId());
    }
}
