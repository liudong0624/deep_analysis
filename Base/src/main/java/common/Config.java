package common;

import utils.RandomName;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * 这个是测试配置文件更新的类
 * xiaoqianke
 * 2018-04-19
 */
public class Config {
    private static String name;
    public static void main(String[] arg){
        Config c = new Config();
        InputStream in = null;
        try {
            in = new FileInputStream("./config.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Configuration configuration = Configuration.from(in);
        System.out.println(configuration.toJSON());
        name = configuration.getString("MyName");
        if (name == null || name.isEmpty()) {
            name = RandomName.getName();
            System.out.println("该模块进程名字为：{}"+name);
            configuration.set("MyName", name,Thread.currentThread().getContextClassLoader().getResource("config.json").getPath());
            System.out.println(configuration.toJSON());
        } else {
            System.out.println("该模块进程名字为：{}"+ name);
        }
    }
}
