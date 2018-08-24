package wa.Embed;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoGridFSException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import utils.*;
import utils.Runner;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Embed extends  Runner{
    private static Logger logger = LoggerFactory.getLogger(Embed.class);
    private Tika tika;
    private final String QUEUE_NAME = "embed";
    private final String EXCHANGE_NAME = "fileinput";
    private ExtractEmbedds extractEmbedds = new ExtractEmbedds();
    private MongodbProcess mongodbProcess = new MongodbProcess();
    private final String foundationEXCHANGE_NAME ="foundation";

    protected void init(){
        super.init();
        tika = new Tika();
    }

    public Embed(){}
    public void Start() {
        try{
            this.init();
        }catch (Exception e){
            PrintException.Print(logger,e);
        }

        TaskInfo taskInfo = new TaskInfo();
        Mqhandle mqhandle = new Mqhandle();
        mqhandle.startMqService();

        SendMqMessage sendMqMessage = new SendMqMessage(mqhandle);

        GetMqMessage getMqMessage = new GetMqMessage(name, taskInfo, mqhandle);
        getMqMessage.SetQueue(QUEUE_NAME);

        new Thread(getMqMessage).start();
        while (true) {
            extractEmbedds.clear();
            logger.debug("开始新任务！");
            JSONObject task = taskInfo.GetTask();
            logger.debug("任务内容为：\n{}",JSON.toJSONString(task,true));

            //获取文件信息
            MongodbProcess.FileResult fileResult =
                    mongodbProcess.GetFileStreamAndInfo(
                            task.getString("file_type"),
                            task.getString("md5"));

            if (fileResult == null){
                logger.error("取不到文件，该任务丢弃？或者发送到error");
                continue;
            }

            try {

                logger.debug("抽取嵌套文件开始");
                String password = task.getString("password");
                if (password == null)
                {
                    password = new String();
                }
                extractEmbedds.extract(fileResult.in,password);

            } catch (SAXException e) {
               PrintException.Print(logger,e);
            } catch (TikaException e) {
                PrintException.Print(logger, e);
                if (AlarmEnrypteFile(e)&& !task.containsKey("password")){
                    task.put("encryption",true);
                    task.put("routekey","embed");
                    //发送mq
                    sendMqMessage.SendTask(foundationEXCHANGE_NAME,"cracking",task,task.getInteger("priority"));
                }
            } catch (IOException e) {
                PrintException.Print(logger,e);
            }

            if (extractEmbedds.isHaveEmbededDoc()){
                List<byte[]> file = extractEmbedds.getFile();
                List<String> name =  extractEmbedds.getFileName();
                Iterator<byte[]> fileiter = file.iterator();
                Iterator<String> nameiter = name.iterator();

                //解嵌套(java)、解压缩(python)和爆破(c)的逻辑和如下逻辑一致！
                while(fileiter.hasNext() && nameiter.hasNext()){
					//1、如有有嵌套文件，将解嵌套后的文件写入数据中心（现在是mongo）
                    logger.debug("有嵌套文件！");
                    byte[] f = fileiter.next();
                    String name_path = nameiter.next();
                    mongodbProcess.WriteFile("sensitive",f,name_path);
                    logger.debug("写入文件{}",name_path);

                    String children_md5 = utils.Check_Sum.Md5(f);
					//发送MQ消息(queue:deep_analysis)，通知数据中心对接程序去数据中心去取任务
					//填充告警信息
					JSONObject mqMessage = new JSONObject();
                    String fileNameTmp = task.getString("file_name").concat("/");
                    String newFileName = fileNameTmp.concat(name_path);
                    mqMessage.put("id",task.getString("id"));
                    mqMessage.put("parent_md5", task.getString("md5")); //原来的MD5
					mqMessage.put("md5",children_md5);  ///该MD5需要改成子文件的MD5
                    mqMessage.put("file_name", newFileName); //通过文件名来记录文件的包含关系
                    mqMessage.put("dtr_id", task.getString("dtr_id")); //检测器id
                    mqMessage.put("priority", task.getString("priority"));
                    mqMessage.put("file_type", task.getString("file_type"));
                    mqMessage.put("time",task.getString("time"));//没有
                    mqMessage.put("version",task.getString("version"));//没有
                    mqMessage.put("file_sources", "embed"); //文件来源，解压缩程序也需要填充该信息
                    sendMqMessage.SendTask(EXCHANGE_NAME, "deep_analysis", mqMessage,
                                            mqMessage.getInteger("priority"));
                    logger.debug("任务结束");
                }
            }else {
                logger.debug("没有嵌套文件生成");
            }
        }
    }

    private boolean AlarmEnrypteFile(TikaException e) {
        Throwable err = e;
        while (err != null) {
            String str = err.toString();
            logger.debug("加密文件报警、异常监测，异常字串为：");
            logger.debug(str);
            if (str.toLowerCase().contains("encrypt")) {
                logger.debug("检测到加密文件");
                return true;
            }
            err = err.getCause();
        }
        return false;
    }

    public static void main(String[] args) {
        //取到任务，从mongodb下载
        Embed embed = new Embed();

        try {
            embed.Start();
        } catch (Exception e) {
            PrintException.Print(logger,e,"程序出错");
        }
    }
}
