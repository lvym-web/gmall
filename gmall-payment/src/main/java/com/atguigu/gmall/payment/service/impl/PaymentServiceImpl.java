package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.api.bean.PaymentInfo;
import com.atguigu.gmall.api.service.PaymentService;
import com.atguigu.gmall.mp.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;
    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {

        //幂等性操作  访问一个请求/多次访问同一个请求 返回结果不变
        PaymentInfo paymentInfocheck=new PaymentInfo();
        paymentInfocheck.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoCheckResult = paymentInfoMapper.selectOne(paymentInfocheck);
        if (paymentInfoCheckResult!=null && paymentInfoCheckResult.getPaymentStatus().equals("已支付")){
            return;
        }else {
            Example example = new Example(PaymentInfo.class);
            example.createCriteria().andEqualTo("orderSn", paymentInfo.getOrderSn());
            Connection connection = null;
            Session session = null;
            try {
                connection = activeMQUtil.getConnectionFactory().createConnection();
                session = connection.createSession(true, Session.SESSION_TRANSACTED);//开启事务

            } catch (JMSException e) {
                e.printStackTrace();
            }


            try {
                paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
                // 支付成功后，引起的系统服务-》订单服务的更新-》库存服务-》物流服务
                // 调用mq发送支付成功的消息
                Queue payhment_success_queue = session.createQueue("PAYHMENT_SUCCESS_QUEUE");//产生队列
                MessageProducer producer = session.createProducer(payhment_success_queue);//
                //TextMessage textMessage=new ActiveMQTextMessage();//字符串文本
                ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();// hash结构
                mapMessage.setString("out_trade_no", paymentInfo.getOrderSn());
                producer.send(mapMessage);
                session.commit();

            } catch (JMSException e) {
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }
            } finally {
                try {
                    session.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }





    }

    //延迟队列
    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo, Integer count) {

        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        try{
            Queue payhment_success_queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payhment_success_queue);

            //TextMessage textMessage=new ActiveMQTextMessage();//字符串文本

            MapMessage mapMessage = new ActiveMQMapMessage();// hash结构

            mapMessage.setString("out_trade_no",outTradeNo);
            mapMessage.setInt("count",count);

            // 为消息加入延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*10);

            producer.send(mapMessage);

            session.commit();
        }catch (Exception ex){
            // 消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }

        }
    }

    //检查阿里接口
    @Override
    public Map<String, Object> checkAliPayPayment(String out_trade_no) {
        Map<String, Object> resultMap = new HashMap<>();

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no", out_trade_no);
        request.setBizContent(JSON.toJSONString(requestMap));

        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("有可能交易已创建，调用成功");
            resultMap.put("out_trade_no",response.getOutTradeNo());
            resultMap.put("trade_no",response.getTradeNo());
            resultMap.put("trade_status",response.getTradeStatus());
            resultMap.put("call_back_content",response.getMsg());
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
        return resultMap;
    }
}
