package utils;

import ConsulConfig.ConsulConfig;
import com.alibaba.fastjson.JSONObject;
import com.orbitz.consul.Consul;
import com.rabbitmq.client.*;
import common.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;


public class Mqhandle {
	
	private static Logger logger = LoggerFactory.getLogger(Mqhandle.class);
	
	private String userNameString;
	private String passWordString;
	private String ipString;
	private int portInt;
	private static final String DATA_PATH_STRING = "./MqData.json";

	private List<String> queueList = new LinkedList<String>();
	private List<String> exChangeList = new LinkedList<String>();
	
	private JSONObject mqJsonData = null;
	Envelope envelope = null;

	private ConnectionFactory factory = null;
	private Connection connection = null;
	//注意这个不是线程安全的
	private Channel channel = null;

	private Map<Long,Channel> channelMap = new HashMap<Long,Channel>();
	
	public Mqhandle(){
/*		Configuration configInfo = InfoCollection.getInstance().getConfig();
		userNameString = configInfo.getString("MqUserName");
		passWordString = configInfo.getString("MqPassWord");
		ipString = configInfo.getString("MqIpHost");
		portInt = configInfo.getInt("MqPort");
		*/
        Consul consul = ConsulConfig.getInstance().GetConsul();
        userNameString = consul.keyValueClient().getValueAsString("common.mq.username").get();
        passWordString = consul.keyValueClient().getValueAsString("common.mq.password").get();
        ipString = consul.keyValueClient().getValueAsString("common.mq.host").get();
        String portString = consul.keyValueClient().getValueAsString("common.mq.port").get();
        portInt = Integer.parseInt(portString);
		
		logger.info("mq用户名："+userNameString);
		logger.info("mq密码："+passWordString);
		logger.info("mqIpHost:"+ipString);
		logger.info("MqPort:"+portInt);


	}
	public boolean connection_isopen(){
		return connection.isOpen();
	}

	public void close(){

		try {
			channel.close();
		} catch (IOException e) {
			PrintException.Print(logger,e);
		} catch (TimeoutException e) {
			PrintException.Print(logger,e);
		}

		try {
			connection.close();
		} catch (IOException e) {
			PrintException.Print(logger,e);
		}
	}

	public Mqhandle startMqService(){
		factory = new ConnectionFactory();

		factory.setHost(ipString);
		factory.setPort(portInt);
		factory.setUsername(userNameString);
		factory.setPassword(passWordString);


		try {
			connection = factory.newConnection();
		} catch (TimeoutException e) {
			PrintException.Print(logger,e);
		} catch (IOException e) {
			PrintException.Print(logger,e);
		}

		try {
			channel = connection.createChannel();
		} catch (IOException e) {
			PrintException.Print(logger,e);
		}

		logger.info("消息队列连接成功");
		return this;
	}

	private Channel CreatChannel(){
		Channel channel_ = null;
		if (connection.isOpen()){
			try {
				channel_ = connection.createChannel();
			} catch (IOException e) {
				PrintException.Print(logger,e);
			}
		}else
		{
			try {
				connection = factory.newConnection();
			} catch (IOException e) {
				PrintException.Print(logger,e);
			} catch (TimeoutException e) {
				PrintException.Print(logger,e);
			}
			try {
				channel_ = connection.createChannel();
			} catch (IOException e) {
				PrintException.Print(logger,e);
			}
		}
		return channel_;
	}

	public Channel getChannel(){
		return CreatChannel();
	}

	public Channel getChannel(long id){
		Channel channel_ = null;
		logger.debug("获取mq channel,线程id为{}",id);
		if (channelMap.containsKey(id)){
			logger.debug("channelMap 中已存在该线程的 channel");
			channel_ = channelMap.get(id);
		}else{
			logger.debug("重新生成channel");
			channel_ = CreatChannel();
			logger.debug("将新生成的channel 写入map id为{}",id);
			channelMap.put(id,channel_);
		}
		return channel_;
	}
	public Channel resetChannel(long id){
		Channel channel_ = null;
		if (channelMap.containsKey(id)){
			logger.debug("channelMap 中已存在该线程的 channel");
			try {
				channelMap.get(id).close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
		}
		channel_ = CreatChannel();
		channelMap.put(id,channel_);
		return channel_;
	}

	public void queueDeclare(
			String queue,
			boolean durable,
			boolean exclusive,
			boolean autoDelete,
			Map<String, Object> arguments) throws IOException{
		AMQP.Queue.DeclareOk ok =
				channel.queueDeclare(
						queue,
						durable,
						exclusive,
						autoDelete,
						arguments);
		queueList.add(queue);
		logger.info("声明队列"+ok.getQueue()+"成功");
	}

	void exchangeDeclare(
			String exchange,
			String type,
			boolean durable) throws IOException{
		AMQP.Exchange.DeclareOk ok =
				channel.exchangeDeclare(exchange,type,durable);
		exChangeList.add(exchange);
		logger.info("声明交换机成功");
	}

	void queueBind(String queue, String exchange, String routingKey) throws IOException {
		AMQP.Queue.BindOk ok = channel.queueBind(queue,exchange,routingKey);
		logger.debug(ok.toString());
		logger.info(queue+":"+exchange+" 绑定成功");
	}

	public void printLists(){
		for (String queue:queueList){
			logger.info(queue);
		}
		for (String exchange:exChangeList){
			logger.info(exchange);
		}
	}

	public Mqhandle deClareDefult(){
		//mqJsonData
		for (Object queue : mqJsonData.getJSONArray("queueNames") ) {
			try {
				// 队列名字, 持久化，专有的，自动删除
				channel.queueDeclare((String)queue, false, false, false, null);
			} catch (IOException e) {
				PrintException.Print(logger,e);
			}
		}
	
		for (Object change : mqJsonData.getJSONArray("change")) {
			try {
				channel.exchangeDeclare(((JSONObject)change).getString("changeName") , ((JSONObject)change).getString("type"), false, false, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				PrintException.Print(logger,e);
			}
		} 
		return this;
	}
	
	public Mqhandle boundMqDefult(){
		for (Object bound_ : mqJsonData.getJSONArray("bounds")) {
			try {
				channel.queueBind(((JSONObject)bound_).getString("queueName"), ((JSONObject)bound_).getString("changeName"), ((JSONObject)bound_).getString("routekey"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				PrintException.Print(logger,e);
			}
		}
		return this;
	}
	
	public Mqhandle loadDefultData(){
		Path path = Paths.get(DATA_PATH_STRING);
		StringBuilder finalResult = new StringBuilder();
		try{
			BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.US_ASCII);
			String line;
			while((line = reader.readLine()) != null){
				finalResult.append(line).append(System.lineSeparator());
			}
			reader.close();
		} catch (IOException e) {
			PrintException.Print(logger,e);
		}
		String jsonString = finalResult.toString();
		mqJsonData = JSONObject.parseObject(jsonString);
		return this;
	}	
}


























