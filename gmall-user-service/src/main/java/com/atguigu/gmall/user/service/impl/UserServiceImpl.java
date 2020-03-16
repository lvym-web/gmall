package com.atguigu.gmall.user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.RedisUtil;
import com.atguigu.gmall.api.bean.UmsMember;
import com.atguigu.gmall.api.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.api.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public List<UmsMember> getAllUser() {

        List<UmsMember> umsMembers = userMapper.selectAll();//userMapper.selectAllUser();

        return umsMembers;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {

        // 封装的参数对象
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);


//       Example example = new Example(UmsMemberReceiveAddress.class);
//       example.createCriteria().andEqualTo("memberId",memberId);
//       List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(example);

        return umsMemberReceiveAddresses;
    }

    @Override
    public UmsMember login(String username, String password) {

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            if (jedis!=null){
                String getMember = jedis.get("user:" + username + password + ":info");
                if (StringUtils.isNotBlank(getMember)){
                    //转为对象
                    UmsMember umsMember = JSON.parseObject(getMember, UmsMember.class);
                    return umsMember;
                }
            }
            //查询数据库
            UmsMember umsMember=new UmsMember();
            umsMember.setUsername(username);
            umsMember.setPassword(password);
            UmsMember umsMemberDb=selectDB(umsMember);
            if (umsMemberDb!=null){
                //存储redis
                jedis.setex("user:" + username + password + ":info", 60 * 60, JSON.toJSONString(umsMemberDb));


            }

            return umsMemberDb;
        }finally {
           jedis.close();
        }

    }

    @Override
    public void addTokenRedis(String token, String id) {
        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:"+id+":token",60*60,token);
        jedis.close();
    }

    private UmsMember selectDB(UmsMember umsMember) {
        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if (umsMembers!=null){
            return umsMembers.get(0);
        }
        return null;
    }
    //查询数据库是否存在    用户
    @Override
    public UmsMember checkUmsMember(String uid) {
        UmsMember umsMember=new UmsMember();
                umsMember.setSourceUid(uid);
        return userMapper.selectOne(umsMember);
    }

    //   第三方登录    aDD
    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        //添加也可以返回
        return umsMember;
    }
    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {
       UmsMemberReceiveAddress umsMemberReceiveAddress=new UmsMemberReceiveAddress();
       umsMemberReceiveAddress.setId(receiveAddressId);

        return umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
    }
}
