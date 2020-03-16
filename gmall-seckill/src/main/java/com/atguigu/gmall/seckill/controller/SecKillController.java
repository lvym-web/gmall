package com.atguigu.gmall.seckill.controller;

import com.atguigu.gmall.RedisUtil;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class SecKillController {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    RedissonClient redissonClient;

    @RequestMapping("seckillRedisson")
    @ResponseBody
    public String seckillRedisson() {
        RSemaphore semaphore = redissonClient.getSemaphore("106");
        boolean acquire = semaphore.tryAcquire();
        if (acquire){
            System.err.println( "抢购成功"+"Redisson先到先得");
        } else {
            System.err.println("抢购失败");

        }

        return "seckillRedisson";
    }

    @RequestMapping("seckill")
    @ResponseBody
    public String seckill() {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();

            jedis.watch("106");//开启监听

            Integer stock = Integer.parseInt(jedis.get("106"));
            if (stock > 0) {
                Transaction multi = jedis.multi();//开启事务

                multi.incrBy("106", -1);

                List<Object> exec = multi.exec(); //执行事务
                if (exec != null && exec.size() > 0) {
                    System.err.println("库存剩余:" + stock + "抢购成功"+"redis随机得到");
                } else {
                    System.err.println("库存剩余:" + stock + "抢购失败");
                }

            }


        } catch (RuntimeException e) {
            e.printStackTrace();

        } finally {
            jedis.close();
        }


        return "kill";

    }
}
