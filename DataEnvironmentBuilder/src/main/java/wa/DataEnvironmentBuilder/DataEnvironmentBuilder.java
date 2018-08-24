package wa.DataEnvironmentBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Mqhandle;

public class DataEnvironmentBuilder extends utils.Runner{
    private static Logger logger = LoggerFactory.getLogger(DataEnvironmentBuilder.class);
    public static void main(String[] args) {
        Mqhandle mq;
        mq = new Mqhandle();
        mq.startMqService();
        mq.loadDefultData();
        mq.deClareDefult();
        mq.boundMqDefult();
    }
}
