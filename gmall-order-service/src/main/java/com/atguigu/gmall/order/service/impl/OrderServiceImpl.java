package com.atguigu.gmall.order.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.RedisUtil;
import com.atguigu.gmall.api.bean.OmsOrder;
import com.atguigu.gmall.api.bean.OmsOrderItem;

import com.atguigu.gmall.api.service.OrderService;

import com.atguigu.gmall.mp.ActiveMQUtil;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {


    @Autowired
    OmsOrderMapper omsOrderMapper;
    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    ActiveMQUtil activeMQUtil;


    @Override
    public String getTradeCode(String memberId) {

        Jedis jedis = redisUtil.getJedis();

        String tradeKey = "user:" + memberId + ":tradeCode";

        String tradeCode = UUID.randomUUID().toString();

        jedis.setex(tradeKey, 60 * 15, tradeCode);

        jedis.close();

        return tradeCode;


//        Jedis jedis = null;
//        String tradeCode="";
//        try {
//             jedis = redisUtil.getJedis();
//             String codeKey="order:"+memberId+":tradeCode";
//             String uuid= UUID.randomUUID().toString();
//            tradeCode = jedis.setex(codeKey, 60 * 15, uuid);//默认时间单位 秒
//
//
//        }finally {
//            jedis.close();
//        }
//
//        return tradeCode;
    }

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {


        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";


            //String tradeCodeFromCache = jedis.get(tradeKey);// 使用lua脚本在发现key的同时将key删除，防止并发订单攻击
            //对比防重删令牌
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));

            if (eval != null && eval != 0) {
                jedis.del(tradeKey);
                return "success";
            } else {
                return "fail";
            }
        } finally {
            jedis.close();
        }


//        Jedis jedis = null;
//
//        try {
//            jedis=redisUtil.getJedis();
//            String codeKey="order:"+memberId+":tradeCode";
//
//            //对比防重删令牌 lua
//            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//
//            Long eval = (Long) jedis.eval(script, Collections.singletonList(codeKey),
//                    Collections.singletonList(tradeCode));
//
//            if (eval!=null && eval!=0){
//
//                return "success";
//            }else {
//                return "fail";
//            }
//
//        }finally {
//            jedis.close();
//        }


    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();

        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            //删除对应购物车商品

            //  cartService.delCart(omsOrderItem.getId());

        }
    }

    @Override
    public OmsOrder getOrderInfo(String outTradeNo) {

        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);

        return omsOrderMapper.selectOne(omsOrder);
    }

    //更新订单
    @Override
    public void updateOrder(String out_trade_no) {

        Example e = new Example(OmsOrder.class);
        e.createCriteria().andEqualTo("orderSn", out_trade_no);

        OmsOrder omsOrderUpdate = new OmsOrder();
        omsOrderUpdate.setStatus(1);

        // 发送一个订单已支付的队列，提供给库存消费
        Connection connection = null;
        Session session = null;

        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_pay_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(order_pay_queue);
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();

            // 查询订单的对象，转化成json字符串，存入ORDER_PAY_QUEUE的消息队列
            OmsOrder omsOrderParam = new OmsOrder();
            omsOrderParam.setOrderSn(out_trade_no);
            OmsOrder omsOrderResponse = omsOrderMapper.selectOne(omsOrderParam);

            OmsOrderItem omsOrderItemParam = new OmsOrderItem();
            omsOrderItemParam.setOrderSn(out_trade_no);
            List<OmsOrderItem> select = omsOrderItemMapper.select(omsOrderItemParam);
            omsOrderResponse.setOmsOrderItems(select);
            textMessage.setText(JSON.toJSONString(omsOrderResponse));
            //更新操作
            omsOrderMapper.updateByExampleSelective(omsOrderUpdate, e);
            producer.send(textMessage);
            session.commit();
        } catch (JMSException e1) {
            try {
                session.rollback();
            } catch (JMSException e2) {
                e2.printStackTrace();
            }
        } finally {
            try {
                session.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }


    }
}
