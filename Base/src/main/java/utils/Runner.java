package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {
    private static Logger logger = LoggerFactory.getLogger(Runner.class);
    protected  String name;
    protected  void init() {
/*        Configuration configInfo = InfoCollection.getInstance().getConfig();
        name = configInfo.getString("MyName");
        if (name == null || name.isEmpty()) {
            name = RandomName.getName();
            logger.info("该模块进程名字为：{}", name);
            configInfo.set("MyName",
                    name,new File("./config.json").getPath());
        } else {
            logger.info("该模块进程名字为：{}", name);
        }*/

        name = RandomName.getName();
        logger.info("该模块进程名字为：{}", name);
    }
}
