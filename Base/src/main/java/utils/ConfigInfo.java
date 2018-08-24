package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigInfo {
    private static Logger logger = LoggerFactory.getLogger(ConfigInfo.class);
    private Properties property = null;
    private Map settings = new HashMap();
    private String filepath = null;


    public ConfigInfo(){
        property = new Properties();
    }

    public void loadFile(InputStream steam,String filepath_){
        filepath = filepath_;
        try {
            property.load(steam);
            for (String s : property.stringPropertyNames()) {
                settings.put(s,property.getProperty(s));
            }
        } catch (IOException e) {
            PrintException.Print(logger,e);
        }
    }

    public String get(String key){
        return (String) settings.get(key);
    }

    public void set(String key,String name,String comment){
        property.setProperty(key, name);
        try {
            FileOutputStream out = new FileOutputStream(filepath);
            property.store(out,comment);
        } catch (FileNotFoundException e) {
            PrintException.Print(logger,e,"配置文件写入失败：");
        } catch (IOException e) {
            PrintException.Print(logger,e,"配置文件写入失败：");
        }
    }

}
