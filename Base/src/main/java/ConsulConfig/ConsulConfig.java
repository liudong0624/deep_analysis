package ConsulConfig;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SendMqMessage;


public class ConsulConfig {
    private static Logger logger = LoggerFactory.getLogger(SendMqMessage.class);
    private static ConsulConfig ourInstance = new ConsulConfig();
    private Consul consul = null;

    public static ConsulConfig getInstance() {
        return ourInstance;
    }

    public static ConsulConfig getInstance(String host, int port){
        return new ConsulConfig(host,port);
    }

    private ConsulConfig() {
        consul =  Consul.builder().build();
        ConsulConfig.logger.info("Consul连接成功");
    }
    private ConsulConfig(String host, int port){
        consul = Consul.builder().withHostAndPort(HostAndPort.fromParts(host,port)).build();
        ConsulConfig.logger.info("Consul连接成功");
    }

    public Consul GetConsul(){return consul;}
}
