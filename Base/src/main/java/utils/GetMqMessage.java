package utils;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.*;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GetMqMessage implements Runnable{
    Logger logger = LoggerFactory.getLogger(GetMqMessage.class);
    private Mqhandle mqhandle;
    private  TaskInfo taskInfo;
    private String consumerName;
    private String queueName;

    /**
     * @param queue
     */
    public void SetQueue(String queue){
        queueName = queue;
    }

    public GetMqMessage(String concumerName_, TaskInfo taskInfo_,Mqhandle mqhandle_){
        consumerName = concumerName_;
        taskInfo = taskInfo_;
        mqhandle = mqhandle_;
    }

    @Override
    public void run() {
        Channel channel = mqhandle.getChannel(Thread.currentThread().getId());
        try {
            channel.basicQos(1);
        } catch (IOException e) {
            PrintException.Print(logger,e);
        }
        Consumer consumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
            {
                JSONObject data = null;
                try{
                    String message = new String(body, "UTF-8");
                    data = JSONObject.parseObject(message);
                    logger.debug("mq取到任务");
                }catch (Exception e){
                    PrintException.Print(logger,e,"json格式错误");
                }
                //
                taskInfo.SetTask(data);
            }
        };
        if (channel != null) {
            try {
                channel.basicConsume(queueName, true, consumerName,
                        consumer);
            } catch (IOException e) {
                PrintException.Print(logger,e,"获取message错误");
            }
        }
    }
}
