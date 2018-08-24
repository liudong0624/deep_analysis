package ConsulServers;

import ConsulConfig.ConsulConfig;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.PrintException;

import java.util.Collections;

public class Register {
    private static Logger logger = LoggerFactory.getLogger(Register.class);

    private static Consul consul = ConsulConfig.getInstance().GetConsul();

    public static boolean register(String name, String id, int port){
        logger.info("注册服务，服务名：{},ID:{},端口:{}",name,id,port);
        AgentClient agentClient = consul.agentClient();
<<<<<<< .mine
        Registration.RegCheck check = Registration.RegCheck.grpc("192.168.4.46:50053/io.grpc.grpctika.Tika/Check",30);
||||||| .r536
        Registration.RegCheck check = Registration.RegCheck.grpc("192.168.4.23:50053/io.grpc.grpctika.Tika/Check",30);
=======
        Registration.RegCheck check = Registration.RegCheck.grpc("192.168.4.23:50053",30);
>>>>>>> .r593

        agentClient.register(
                port,
                check,
                name,id, Collections.emptyList(),Collections.emptyMap());
        return true;
    }

}
