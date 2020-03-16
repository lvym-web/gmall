package com.atguigu.gmall.passport.controller;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpclientUtil;

import java.util.HashMap;
import java.util.Map;

public class testOAuth {

    public static void main(String[] args) {
       //引导用户到如下地址
     String s1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=169157414&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");
       //用户同意授权，页面跳转至 xxx/?code=CODE
    //    String s2 = "http://passport.gmall.com:8085/vlogin?code=8be440671f612effe68a1f603f72a8a3";
      //使用返回的code，换取access token
    //   String s3="https://api.weibo.com/oauth2/access_token?client_id=169157414&client_secret=7b20b4484d8cb55f3dfd8b6f5f27481c&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code=8be440671f612effe68a1f603f72a8a3";
    //    String s = HttpclientUtil.doPost(s3, null);
     //    2.00x6ag6G05Zl8Lf73ce1e69eOcoH9B
     //   System.out.println(s);
    //    {"access_token":"2.00h35WFI05Zl8L67f5f8cf080EsXeg","remind_in":"116735","expires_in":116735,"uid":"7410664495","isRealName":"true"}
   //在用户使用的过程中通过access_token获取用户数据(第三方网站的用户数据)
       // {"access_token":"2.00h35WFI05Zl8L67f5f8cf080EsXeg","remind_in":"117566","expires_in":117566,"uid":"7410664495","isRealName":"true"}
//        String s4="https://api.weibo.com/2/users/show.json?access_token=2.00h35WFI05Zl8L67f5f8cf080EsXeg&uid=7410664495";
//        String s = HttpclientUtil.doGet(s4);
//        Map map = JSON.parseObject(s, Map.class);
//        System.out.println(map);

    }
}
