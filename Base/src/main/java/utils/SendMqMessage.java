package utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;

import java.nio.charset.Charset;

public class SendMqMessage {
    private Logger logger = LoggerFactory.getLogger(SendMqMessage.class);

    private Mqhandle mqhandle;
    private Channel channel;
    private final String EXCHANGENAME_F = "foundation";
    private final String EXCHANGENAME_S = "";
    private final String ROUTEKEY_F = "detection";

    public SendMqMessage(Mqhandle mqhandle_){
        mqhandle = mqhandle_;
        channel = mqhandle.getChannel(Thread.currentThread().getId());
    }

    public void SendFoundationTask(JSONObject data,int priority){
        SendTask(EXCHANGENAME_F,ROUTEKEY_F,data,priority);
    }

    public void SendTask(String exchangeName, String  routeKey, JSONObject data, int priority){
        byte [] body;
        body = data.toJSONString().getBytes(Charset.forName("UTF-8"));
        logger.debug("发送exchangName :{}",exchangeName);
        logger.debug("发送routekey :{}",routeKey);
        logger.debug("发送内容\n{}", JSON.toJSONString(data,true));
        try{
            if (!channel.isOpen() || !mqhandle.connection_isopen()){
            channel = mqhandle.resetChannel(Thread.currentThread().getId());
            }
            channel.basicPublish(
                    exchangeName,routeKey,
                    new BasicProperties().builder().priority(priority).build(),
                    body);
            new BasicProperties().builder().deliveryMode(2);
        }catch (IOException e){
            PrintException.Print(logger,e,"发送消息失败");
        }
    }
}
