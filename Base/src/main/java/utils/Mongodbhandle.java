package utils;

import ConsulConfig.ConsulConfig;
import com.mongodb.MongoClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.health.ServiceHealth;
import common.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class Mongodbhandle {
    private static Logger logger = LoggerFactory.getLogger(Mongodbhandle.class);
    private static volatile Mongodbhandle instance;
    private MongoClient client = null;

    private String ipString = null;
    private int portInt = 0;

    private Mongodbhandle() {
        //Configuration configInfo = InfoCollection.getInstance().getConfig();
        Consul consul = ConsulConfig.getInstance().GetConsul();

        ipString = consul.keyValueClient().getValueAsString("common.mongodb.host").get();

        //ipString = configInfo.getString("MongoHost");
        logger.info("mongo链接IP:"+ipString);
        String portString = consul.keyValueClient().getValueAsString("common.mongodb.port").get();
        portInt = Integer.parseInt(portString);
        //portInt = configInfo.getInt("MongoPort");
        logger.info("mongo链接port："+portInt);
        client = new MongoClient(ipString, portInt);
    }

    public static Mongodbhandle getInstance() {
        if (instance == null) {
            instance = new Mongodbhandle();
        }
        return instance;
    }

    public  MongoClient getMongoClient() {return client;}
}
