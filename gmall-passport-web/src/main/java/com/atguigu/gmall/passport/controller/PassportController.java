package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.api.bean.UmsMember;
import com.atguigu.gmall.api.service.UserService;
import com.atguigu.gmall.util.HttpclientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {


    @Reference
    UserService userService;

    @RequestMapping("vlogin")
    public String vlogin(String code,HttpServletRequest request) {
// 授权码换取access_token
        // 换取access_token
         String s3="https://api.weibo.com/oauth2/access_token?";
         Map<String,String> map=new HashMap<>();
         map.put("client_id","169157414");
        map.put("client_secret","7b20b4484d8cb55f3dfd8b6f5f27481c");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        map.put("code",code);//授权有效期内可以使用，没新生成一次授权码，说明用户对第三方数据进行重启授权，之前的access_token和授权码全部过期
        String access_token = HttpclientUtil.doPost(s3, map);
        Map parseObject = JSON.parseObject(access_token, Map.class);

        String get_access_token = (String) parseObject.get("access_token");
        String uid = (String) parseObject.get("uid");
        String s4="https://api.weibo.com/2/users/show.json?access_token="+get_access_token+"&uid="+uid;
        String s = HttpclientUtil.doGet(s4);
        Map map1 = JSON.parseObject(s, Map.class);

         String gender= (String) map1.get("gender");
        //m：男、f：女、n：未知
        Integer sex=0;
        if (gender=="m"){
            sex=1;
        }else {
            sex=2;
        }

       //保存数据库
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType(3);
        umsMember.setSourceUid(uid);
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(get_access_token);
        umsMember.setCity((String) map1.get("location"));
        umsMember.setGender(sex);
        umsMember.setNickname((String) map1.get("screen_name"));
        umsMember.setStatus(1);
        //查询数据库是否存在
        UmsMember member=userService.checkUmsMember(uid);
        if (member==null){
            umsMember=userService.addOauthUser(umsMember);
        }else {
            umsMember=member;
        }
        // 生成jwt的token，并且重定向到首页，携带该token
        // 生成jwt的token，并且重定向到首页，携带该token
        String token = null;
        String memberId = umsMember.getId();
        String nickname = umsMember.getNickname();
        Map<String,Object> userMap = new HashMap<>();
        userMap.put("memberId",memberId);
        userMap.put("nickname",nickname);


        String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
        if(StringUtils.isBlank(ip)){
            ip = request.getRemoteAddr();// 从request中获取ip
            if(StringUtils.isBlank(ip)){
                ip = "127.0.0.1";
            }
        }

        // 按照设计的算法对参数进行加密后，生成token
        token = JwtUtil.encode("2019gmall0105", userMap, ip);

        // 将token存入redis一份
        userService.addTokenRedis(token,memberId);

        return "redirect:http://search.gmall.com:8083/index?token="+token;
    }



    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,String currentIp) {

     //   eyJhbGciOiJIUzI1NiJ9.eyJuaWNrbmFtZSI6IndpbmRpciIsIm1lbWJlcklkIjoiMSJ9.R9H8_bXLAp2Awv-a6zj9c8N2pER8Tny6gvDZDK4maZc

         //通过jwt验证token
        Map<String,String> map=new HashMap<>();
        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall0105", currentIp);
        if (decode!=null){
            map.put("status","success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));
        }else {
            map.put("status","fail");
        }

        return JSON.toJSONString(map);
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(String username, String password, HttpServletRequest request) {
        String token = "";
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            UmsMember umsMember = userService.login(username, password);

            if (umsMember!= null) {
                //登录成功 给予token
                Map<String,Object> map=new HashMap<>();
                map.put("memberId",umsMember.getId());
                map.put("nickname",umsMember.getNickname());

                String ip = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
                if (StringUtils.isBlank(ip)){
                    ip=request.getRemoteAddr();// 从request中获取ip
                    if (StringUtils.isBlank(ip)){
                        ip="127.0.0.1";
                    }
                }
                // 用jwt制作token
                // 按照设计的算法对参数进行加密后，生成token
                token= JwtUtil.encode("2019gmall0105", map, ip);

                userService.addTokenRedis(token,umsMember.getId());


            } else {
                token = "fail";
            }

        }

        return token;
    }

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap modelMap) {
        modelMap.put("ReturnUrl", ReturnUrl);
        return "index";
    }




}
