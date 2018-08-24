package wa.GetContent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.*;

import java.io.IOException;

public class GetContent extends utils.Runner {
    private static Logger logger = LoggerFactory.getLogger(GetContent.class);

    //从extract获取数据
    private final String EXTRACT_QUEUE_NAME ="extract";
    //将任务发送到哪里？ 广播exchange 广播到keyword secret style
    private final String EXCHANGE_NAME = "sensitive";
    private final String foundationEXCHANGE_NAME ="foundation";
    private Tika tika;
    private MongodbProcess mongodbProcess;

    protected void init(){
        super.init();
        tika = new Tika();
        mongodbProcess = new MongodbProcess();
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

    private void Start(){
        this.init();
        TaskInfo taskInfo = new TaskInfo();
        Mqhandle mqhandle = new Mqhandle();
        mqhandle.startMqService();

        SendMqMessage sendMqMessage = new SendMqMessage(mqhandle);

        GetMqMessage getMqMessage = new GetMqMessage(name, taskInfo, mqhandle);
        getMqMessage.SetQueue(EXTRACT_QUEUE_NAME);
        new Thread(getMqMessage).start();
        do {
            //从mq获取任务数据
            logger.debug("开始新任务");
            JSONObject task = taskInfo.GetTask();
            logger.debug("获取到任务！");
            logger.debug("任务内容为：\n{}",JSON.toJSONString(task,true));
            //查询mongodb是否有该文件
            MongodbBase.FileResult fileResult =
                    mongodbProcess.GetFileStreamAndInfo(task.getString("file_type"), task.getString("md5"));

            Metadata tikaMetaData = new Metadata();
            tikaMetaData.set(Metadata.RESOURCE_NAME_KEY, fileResult.gridFSFile.getFilename());
            if (task.containsKey("encryption"))
            {
                tikaMetaData.set("password",task.getString("password"));
            }

            String content = null;
            //抽词
            if (fileResult.in != null) {
                try {
                    content = tika.parseToString(fileResult.in, tikaMetaData);
                } catch (IOException e) {
                    PrintException.Print(logger, e);
                } catch (TikaException e) {
                    PrintException.Print(logger, e);
                    if (AlarmEnrypteFile(e)&& !task.containsKey("password")){
                        task.put("encryption",true);
                        task.put("routekey","extract");
                        //发送mq
                        sendMqMessage.SendTask(foundationEXCHANGE_NAME,"cracking",task,task.getInteger("priority"));
                    }
                }
            }else {
                logger.error("文件没有查询到！{}",task.getString("md5"));
                continue;
            }
            task.put("__content",content);
            //将content写入mongodb sensitive_data_temp表
            mongodbProcess.UpdateContent(task);
            //发任务广播
            sendMqMessage.SendTask(EXCHANGE_NAME,"",task,task.getInteger("priority"));
            logger.debug("任务结束");
        } while (true);

    }

    public static void main(String[] args) {
        //取到任务，从mongodb下载
        GetContent getContent= new GetContent();

        try {
            getContent.Start();
        } catch (Exception e) {
            PrintException.Print(logger,e,"程序出错");
        }
    }

}
