package wa.ExDetection;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.MongoGridFSException;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExDetection extends  utils.Runner{
    private static Logger logger = LoggerFactory.getLogger(ExDetection.class);

    private final String EXCHANGE_NAME = "foundation";
    private final String ALARM_EXCHANGE_NAME = "alarm";

    private final String DEEP_ANALYSIS_QUEUE_NAME = "deep_analysis";
    private final String DEEP_RESULT_QUEUE_NAME = "deep_result";

    private final String PCAP_ANALYSIS_ROUTEKEY = "pcap_analysis";
    private final String TROJAN_ANALYSIS_ROUTEKEY = "trojan_analysis";

    private MessageMap mesmap_ = new MessageMap();
    private Tika tika;
    private MongodbProcess mongodbProcess;

    public ExDetection(){}
    protected void init(){
        super.init();
        tika = new Tika();
        mongodbProcess = new MongodbProcess();
    }

    private void Start() {
        this.init();
        TaskInfo taskInfo = new TaskInfo();
        Mqhandle mqhandle = new Mqhandle();
        mqhandle.startMqService();

        SendMqMessage sendMqMessage = new SendMqMessage(mqhandle);

        GetMqMessage getMqMessage = new GetMqMessage(name, taskInfo, mqhandle);
        getMqMessage.SetQueue(DEEP_ANALYSIS_QUEUE_NAME);
        new Thread(getMqMessage).start();
        do {
            //从mq获取任务数据
            logger.debug("开始新任务");
            JSONObject task = taskInfo.GetTask();

            task.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            task.put("UUID", GetUUID.getUUID());
            task.put("version", 1);//临时

            logger.debug("任务内容为：\n{}",JSON.toJSONString(task,true));

            //查询mongodb是否有该文件
            MongodbBase.FileResult fileResult =
                    mongodbProcess.GetFileStreamAndInfo(task.getString("file_type"), task.getString("md5"));

            if (fileResult == null){
                logger.error("文件没有查询到！{}",task.getString("md5"));
                continue;
            }
            //对mediatype进行处理
            //查看mediatype是否在map中，查看从mongodb获取的type是否在map中
            String mediaType = null;//image/x-png
            String mongoContentType;
            try {
                mongoContentType = fileResult.gridFSFile.getContentType();//image/x-png
            } catch (MongoGridFSException e) {
                mongoContentType = null;
            }

            Metadata tikaMetaData = new Metadata();

            String filename = fileResult.gridFSFile.getFilename();
            long lenth = fileResult.gridFSFile.getLength();
            logger.debug("本任务的文件名为:{}",filename);
            logger.debug("本任务的文件长度为:{}",lenth);

            tikaMetaData.set(Metadata.RESOURCE_NAME_KEY, filename);
            tikaMetaData.set(Metadata.CONTENT_LENGTH,Long.toString(lenth));

            InputStream tikain = TikaInputStream.get(fileResult.in);

            if (fileResult.in != null) {
                try {
                    mediaType = tika.detect(tikain, tikaMetaData);
                } catch (IOException e) {
                    PrintException.Print(logger, e);
                }
            }

            logger.debug("tika检测文件类型为{}", mediaType);
            logger.debug("mongodb给出文件类型为{}", mongoContentType);

            //retodo:是否有必要将新的mime写回mongodb？ 这里要看这个值以后是否需要，文件类型识别如果要参考该值，最好还是跟新为好
            //根据文件流、文件名、文件类型提示，获取文件相应文件类型，根据文件类型，获取routekey，发送任务内容
            if (mongoContentType == null || mongoContentType.compareTo(mediaType) != 0) {
                mongodbProcess.UpdateOneFileContentType(
                        task.getString("file_type"),
                        fileResult.gridFSFile.getMD5(),
                        mediaType
                );
            }

            //mime归一
            String mimeType = null;
            if (null != mediaType) {
                mimeType = mediaType;
            } else if (null != mongoContentType) {
                mimeType = mongoContentType;
            }

            //查询数据中心中是否有该文件的分析结果

            Document analysisResult = null;
            /*{
                "_id" : ObjectId("5b45af8ce138230be742a650"),
                "analysis_name" : "trojan_analysis",
                "deep_info" : [
                    {}
                ],
                "md5" : "afaa2060b932210bb653fea522bed893"
            }*/
            Document taskResult = null;
            if (task.getString("file_type").equals("alarm")) {
                //攻击告警的所有告警
                //reTODO 查询数据结果表，没有考虑多版本的情况，需要在查询时加上该参数 ？
                analysisResult = mongodbProcess.GetAnalysisResult(task.getString("md5"),"alarm");

                if (null == analysisResult) {//
                    logger.debug("没有在analysis表中查找到数据{}",task.getString("md5"));
                    //如果数据中心没有分析结果，则查询临时任务表中是否有该文件信息
                    taskResult = mongodbProcess.GetTaskInfo(task.getString("md5"),"alarm");
                    //如果有该任务，则跟新表
                    //没有该任务则 1.添加taskinfo表，生成任务消息并发送

                    if (null == taskResult){
                        //添加taskinfo，生成任务消息并发送
                        logger.debug("没有在temp表中查找到数据{}",task.getString("md5"));
                        mongodbProcess.InsertTaskTemp(task);
                        if (mimeType.contains("pcap")){
                            sendMqMessage.SendTask(ALARM_EXCHANGE_NAME,PCAP_ANALYSIS_ROUTEKEY,task,task.getInteger("priority"));
                        }else {
                            sendMqMessage.SendTask(ALARM_EXCHANGE_NAME,TROJAN_ANALYSIS_ROUTEKEY,task,task.getInteger("priority"));
                        }

                    }else {
                        //跟新表
                        logger.debug("在temp表中查找到数据");
                        mongodbProcess.update_task_metainfo(task.getString("md5"),"alarm",task);
                    }
                }else {
                    //检测到结果，则发送mq消息到队列deep_result
                    //消息组成由  告警元数据、文件对应挖掘结果
                    logger.debug("在analysis表中查找到数据");
                    JSONObject message = (JSONObject) task.clone();
                    message.put("analysis_name","");//可能要改
                    message.put("deep_info",analysisResult.get("deep_info"));

                    sendMqMessage.SendTask(EXCHANGE_NAME,DEEP_RESULT_QUEUE_NAME,message,message.getInteger("priority"));
                }
            } else {
                //内容告警
                //按照类型发送消息到mq
                //这里要区分  抽签套、解压缩、解密类型 和  抽词、ocr类型
                //前者直接发送任务到相应队列，后者需要对文件进行查询
                String routekey = mesmap_.map_.getString(mimeType);
                if (routekey == null){
                    logger.error("未知文件类型，没有做处理，{}",mimeType);
                    continue;
                }
                if (routekey.equals("ocr")||routekey.equals("extract")){
                    //查询
                    Document sensitive_analysis_Result;
                    sensitive_analysis_Result = mongodbProcess.GetAnalysisResult(task.getString("md5"),"sensitive");
                    Document sensitive_taskResult = null;
                    if (sensitive_analysis_Result == null){
                        //没有查询到结果
                        //如果数据中心没有分析结果，则查询临时任务表中是否有该文件信息
                        logger.debug("没有在analysis表中查找到数据{}",task.getString("md5"));
                        sensitive_taskResult = mongodbProcess.GetTaskInfo(task.getString("md5"),"sensitive");
                        if (sensitive_taskResult == null){
                            //添加taskinfo，生成任务消息并发送
                            logger.debug("没有在temp表中查找到数据{}",task.getString("md5"));
                            mongodbProcess.InsertTaskTemp(task);
                            sendMqMessage.SendTask(EXCHANGE_NAME,routekey,task,task.getInteger("priority"));
                        }else {
                            //跟新表，添加meta数据
                            logger.debug("在temp表中查找到数据");
                            mongodbProcess.update_task_metainfo(task.getString("md5"),"sensitive",task);
                        }

                    }else {
                        //检测到结果，则发送mq消息到队列deep_result
                        //消息组成由  告警元数据、文件对应挖掘结果
                        logger.debug("在analysis表中查找到数据！");
                        logger.debug("数据详细为：\n{}",sensitive_analysis_Result.toJson());
                        JSONObject message = (JSONObject) task.clone();

                        message.put("analysis_name","");//可能要改
                        message.put("content",sensitive_analysis_Result.getString("content"));
                        message.put("deep_info",sensitive_analysis_Result.get("deep_info"));
                        sendMqMessage.SendTask(EXCHANGE_NAME,DEEP_RESULT_QUEUE_NAME,message,message.getInteger("priority"));
                    }
                }
                if (routekey.equals("extract")){
                    //发送embed
                    sendMqMessage.SendTask(EXCHANGE_NAME,"embed",task,task.getInteger("priority"));
                }
                if (routekey.equals("uncompress")){
                    //发送解压缩
                    sendMqMessage.SendTask(EXCHANGE_NAME,routekey,task,task.getInteger("priority"));
                }
                logger.debug("任务结束");
            }
        } while (true);
    }

    public static void main(String[] args) {
        //取到任务，从mongodb下载
        logger.info("任务分发程序开始！");
        ExDetection detection = new ExDetection();

        try {
            detection.Start();
        } catch (Exception e) {
            PrintException.Print(logger,e,"程序出错");
        }
    }
}
