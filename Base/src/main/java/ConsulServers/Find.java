package ConsulServers;

import ConsulConfig.ConsulConfig;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.health.ServiceHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Find {
    private static Logger logger = LoggerFactory.getLogger(Find.class);
    private static Consul consul = ConsulConfig.getInstance().GetConsul();

    public static void findServer(){
        HealthClient healthClient = consul.healthClient();


        List<ServiceHealth> nodes = healthClient.getAllServiceInstances("TIKA").getResponse(); // discover only "passing" nodes
        for (ServiceHealth node :
                nodes) {

            System.out.println(node.getNode().getAddress());
            System.out.println(node.getService().getPort());
        }
    }

    public static void findServerLoadBalancing(){
        HealthClient healthClient = consul.healthClient();

        List<ServiceHealth> nodes = healthClient.getHealthyServiceInstances("DataService").getResponse(); // discover only "passing" nodes
    }
}
