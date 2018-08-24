package wa.ExDetection;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.PrintException;

import java.util.Arrays;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;

public class MongodbProcess extends utils.MongodbBase{
    private static Logger logger = LoggerFactory.getLogger(MongodbProcess.class);

    private final String sensitive_task = "sensitive_task_temp";
	private final String alarm_task = "alarm_task_temp";
	private final String sensitive_deep_analysis = "sensitive_deep_analysis";
    private final String alarm_deep_analysis = "alarm_deep_analysis";
    private final String sstv_data_temp = "sensitive_data_temp";

    private MongoCollection<Document> sensitive_task_collection;
	private MongoCollection<Document> alarm_task_collection;
    private MongoCollection<Document> sensitive_deep_collection;
    private MongoCollection<Document> alarm_deep_collection;
    private MongoCollection<Document> sstv_data_collection;

    public MongodbProcess(){
        super();
        sensitive_task_collection = mongoDatabase.getCollection(sensitive_task);
		alarm_task_collection = mongoDatabase.getCollection(alarm_task);
        sensitive_deep_collection = mongoDatabase.getCollection(sensitive_deep_analysis);
        alarm_deep_collection = mongoDatabase.getCollection(alarm_deep_analysis);
        sstv_data_collection = mongoDatabase.getCollection(sstv_data_temp);
    }

    /*
    public void update_sstv_task_temp(String md5, String version,String deep_analysis_type){
        Bson inc = Updates.inc("analysis_name_num",1);
        Bson addToSet = Updates.addToSet("analysis_names",deep_analysis_type);
        sensitive_task_collection.updateOne(combine(eq("md5",md5),eq("version",version)),
                combine(inc,addToSet));
    }
    */

    //追加内容文件的告警信息
    public void update_task_metainfo(String md5,String type,JSONObject data){
        Document meta = new Document();
        meta.append("id",data.getString("id"));
        meta.append("dtr_id",data.getString("dtr_id"));
        meta.append("file_name",data.getString("file_name"));
        meta.append("file_type",data.getString("file_type"));
        String parentmd5 = data.getString("parent_md5");
        if (parentmd5 != null){
            meta.append("parent_md5",parentmd5);
        }

        Bson addToSet = Updates.addToSet("meta",meta);
        if (type.equals("alarm"))
        {
            alarm_task_collection.updateOne(combine(eq("md5",md5)),addToSet);
        }else if (type.equals("sensitive")){
            sensitive_task_collection.updateOne(combine(eq("md5",md5)),addToSet);
        }
    }

    //新增任务
    void InsertTaskTemp(JSONObject data){
        Document doc = new Document("md5",data.getString("md5"));
        Document meta = new Document();

        meta.append("id",data.getString("id"));
        meta.append("parent_md5",data.getString("parent_md5"));
        meta.append("dtr_id",data.getString("dtr_id"));
        meta.append("file_name",data.getString("file_name"));
        //meta.append("priority",data.getString("priority"));
        meta.append("file_type",data.getString("file_type"));
        doc.append("meta", Arrays.asList(meta));
        doc.append("time", data.getString("time"));
        doc.append("version", data.getString("version"));

        try{
            String file_type = data.getString("file_type");
            if (file_type.equals("alarm"))
            {
                doc.append("analysis_name_num", 1);
                //doc.append("analysis_names", "alarm");
                alarm_task_collection.insertOne(doc);
            }
            else
            {
                //todo：数字需要修改，和深度分析处理的进程数量有关系
                doc.append("analysis_name_num", 3);//关键词、 版式 、 标密！
                sensitive_task_collection.insertOne(doc);

                //todo:需要考虑ocr特殊场景和爆破场景

                //在临时数据表插入新记录
                Document dataDoc = new Document("md5",data.getString("md5"));
                sstv_data_collection.insertOne(dataDoc);
            }
        }catch (Exception e){
            PrintException.Print(logger,e);
        }
    }

    //判定是否有告警文件分析结果
    public Document GetAnalysisResult( String md5,String type) {
        MongoCollection<Document> collection = null;
        if (type.equals("alarm")){
            collection = alarm_deep_collection;
        }else {
            collection = sensitive_deep_collection;
        }
        BasicDBObject queryObject = new BasicDBObject("md5", md5);
        MongoCursor<Document> mongoCursor = collection.find(queryObject).iterator();
        while (mongoCursor.hasNext())
        {
            return mongoCursor.next();
        }
        return null;
    }

    public Document GetTaskInfo( String md5 ,String type) {
        MongoCollection<Document> collection = null;
        if (type.equals("alarm")){
            collection = alarm_task_collection;
        }else {
            collection = sensitive_task_collection;

        }
        BasicDBObject queryObject = new BasicDBObject("md5", md5);
        MongoCursor<Document> mongoCursor = collection.find(queryObject).iterator();
        while (mongoCursor.hasNext())
        {
            return mongoCursor.next();
        }
        return null;
    }
}
