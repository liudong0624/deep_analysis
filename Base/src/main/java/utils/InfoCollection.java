package utils;

import common.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by Administrator on 2016/11/21.
 */
public class InfoCollection {
    private static Logger logger = LoggerFactory.getLogger(InfoCollection.class);
    private static InfoCollection instance = null;
    private Configuration config = null;
    private InfoCollection(){/*
	私有构造函数
	从配置文件读取具体配置内容
	配置文件读取路径可以写死，也可以调用一个java函数
	没有具体研究 ，可以先读绝对路径、相对路径，服务打包等项定型，再改
	InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("test.properties");
	*/
        InputStream in = null;
            /**
             * 如果这里的配置文件路径修改了，需要去Configuration类的更新配置文件的方法里面修改为相同的方法
             * 肖乾柯：note
             * 时间：2018-04-19
             */
        try {
            in = new FileInputStream("./config.json");
        } catch (FileNotFoundException e) {
            PrintException.Print(logger,e);
        }

        config = Configuration.from(in);
    }
    public static synchronized InfoCollection getInstance(){
        if(instance == null){
            instance = new InfoCollection();
        }
        return instance;
    }

    public Configuration getConfig(){
        return config;
    }

}
