package com.atguigu.gmall.test.redisson;

import com.atguigu.gmall.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;


@Controller
public class TestRedisson {


    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RedissonClient redissonClient;


    @RequestMapping("redisson")
    @ResponseBody
    public String redissonT() {

        Jedis jedis = redisUtil.getJedis();
        //声明锁
        RLock lock = redissonClient.getLock("lock");
        //上锁
        lock.lock();

        try {
            String v = jedis.get("k");
            if (StringUtils.isBlank(v)) {
                v = "1";
            }
            System.out.println(">>>>>>" + v);
            jedis.set("k", (Integer.parseInt(v) + 1) + "");

        }finally {
            jedis.close();
            lock.unlock();
        }

        return "success";
    }

}
