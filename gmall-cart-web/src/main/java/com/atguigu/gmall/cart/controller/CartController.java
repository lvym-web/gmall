package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.api.bean.OmsCartItem;
import com.atguigu.gmall.api.bean.PmsSkuInfo;
import com.atguigu.gmall.api.service.CartService;
import com.atguigu.gmall.api.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {
    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;



    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess=false)
    public String cartList(String isChecked, String skuId, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("memberId");
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setIsChecked(isChecked);
        omsCartItem.setProductSkuId(skuId);
        cartService.updateCartList(omsCartItem);

        //渲染页面
        if (StringUtils.isNotBlank(memberId)){
            List<OmsCartItem> cartList = cartService.getCartList(memberId);
            modelMap.put("cartList", cartList);
            BigDecimal bigDecimal = getTotalAmount(cartList);
            modelMap.put("totalAmount", bigDecimal);
        }else {
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            List<OmsCartItem> omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);

            List<OmsCartItem> cartItems=new ArrayList<>();
            for (OmsCartItem cartItem : omsCartItems) {
                cartItem.setIsChecked(isChecked);
               cartItems=new ArrayList<>();
                cartItems.add(cartItem);
            }


            modelMap.put("cartList", cartItems);
            BigDecimal bigDecimal = getTotalAmount(cartItems);
            modelMap.put("totalAmount", bigDecimal);

        }

        //获取总价

        return "cartListInner";
    }

    @RequestMapping("cartList")
    @LoginRequired(loginSuccess=false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();

        String memberId = (String) request.getAttribute("memberId");

        if (StringUtils.isNotBlank(memberId)) {
            //已登陆
            omsCartItems = cartService.getCartList(memberId);
        } else {
            //未登录
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)) {
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        }
        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));

        }
        modelMap.put("cartList", omsCartItems);
        //获取总价
        BigDecimal bigDecimal = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount", bigDecimal);

        return "cartList";
    }

    //求取总价

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");

        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();
            String isChecked = omsCartItem.getIsChecked();
            if (StringUtils.isNotBlank(isChecked) && isChecked.equals("1")) {
                totalAmount = totalAmount.add(totalPrice);
            }
        }

        return totalAmount;
    }

    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess=false)
    public String addToCart(String skuId, long quantity, HttpServletRequest request, HttpServletResponse response) {

        List<OmsCartItem> omsCartItems = new ArrayList<>();

        PmsSkuInfo pmsSkuInfo = skuService.getskuById(skuId, "");
        OmsCartItem omsCartItem = new OmsCartItem();
        // 将商品信息封装成购物车信息

        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(pmsSkuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(pmsSkuInfo.getCatalog3Id());
        omsCartItem.setProductId(pmsSkuInfo.getProductId());
        omsCartItem.setProductName(pmsSkuInfo.getSkuName());
        omsCartItem.setProductPic(pmsSkuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("11111111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));
        omsCartItem.setTotalPrice(pmsSkuInfo.getPrice().multiply(BigDecimal.valueOf(quantity)));

        //
        String memberId = (String) request.getAttribute("memberId");
        if (StringUtils.isBlank(memberId)) {
            // 用户没有登录

            // cookie里原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isBlank(cartListCookie)) {
                omsCartItems.add(omsCartItem);
            } else {
                //更新cookie
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);//将json转对象
                // 判断添加的购物车数据在cookie中是否存在
                boolean exist = if_cart_exist(omsCartItems, omsCartItem);
                // 之前添加过，更新购物车添加数量
                if (exist) {
                    for (OmsCartItem cartItem : omsCartItems) {
                        if (cartItem.getProductId().equals(omsCartItem.getProductId())) {
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
                        }
                    }
                } else {

                    omsCartItems.add(omsCartItem);
                }

            }

            CookieUtil.setCookie(request, response, "cartListCookie", JSON.toJSONString(omsCartItems), 60 * 60 * 72, true);

        } else {
            //登录
            //从数据库中查询购物车中数据
            OmsCartItem omsCartItemFromDB = cartService.getCartByMem(memberId, skuId);
            //数据库受否存在
            if (omsCartItemFromDB == null) {
                //不存在
                omsCartItem.setMemberId(memberId);
                // 该用户没有添加过当前商品

                omsCartItem.setMemberNickname("test小明");

                cartService.addCart(omsCartItem);
            } else {
                omsCartItemFromDB.setQuantity(omsCartItemFromDB.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDB);
            }

            //同步缓存
            cartService.flushCartCache(memberId);


        }

        return "redirect:/success.html";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        boolean b = false;
        for (OmsCartItem cartItem : omsCartItems) {
            String productId = cartItem.getProductId();
            if (productId.equals(omsCartItem.getProductId())) {
                b = true;
            }

        }
        return b;
    }
}
