package wa.Embed;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.client.MongoCollection;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.PrintException;

import javax.print.Doc;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.addToSet;
import static com.mongodb.client.model.Updates.combine;

public class MongodbProcess extends utils.MongodbBase{
    private static Logger logger = LoggerFactory.getLogger(MongodbProcess.class);

    private MongoCollection<Document> Deep_file_infoCollection;
	private final String sstv_data_temp = "sensitive_data_temp";
    private final String deep_file_info = "deep_file_info";
	private MongoCollection<Document> sstv_data_collection;

    public MongodbProcess(){
        super();
        Deep_file_infoCollection = mongoDatabase.getCollection(deep_file_info);
        Deep_file_infoCollection.createIndex(Indexes.ascending("md5"),
                new IndexOptions().unique(true));
		sstv_data_collection = mongoDatabase.getCollection(sstv_data_temp);
    }

    public void WriteFile(String buketName, byte[] file,String fileName){
        GridFSBucket gridFSBucket =  GridFSBuckets.create(mongoDatabase, buketName);
        String md5 = utils.Check_Sum.Md5(file);
        GridFSFindIterable gridFSFindIterable= gridFSBucket.find(eq("md5",md5));

        if (gridFSFindIterable.first() != null){
            logger.debug("在文件库中搜索到该文件，md5为{}",md5);
            return;
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(file);
        gridFSBucket.uploadFromStream(fileName,byteArrayInputStream);
        try {
            byteArrayInputStream.close();
        } catch (IOException e) {
            PrintException.Print(logger,e);
        }

    }

    public void UpdateFileInfo(JSONObject data, Iterator<String> nameiter,  Iterator<byte[]> fileiter){
        JSONObject object = new JSONObject();
        object.put("version",data.getString("version"));
        object.put("md5",data.getString("md5"));
        JSONArray jsonArray = new JSONArray();
        while(nameiter.hasNext()&&fileiter.hasNext()){
            String md5 = utils.Check_Sum.Md5(fileiter.next());
            String nametmp = nameiter.next();
            JSONObject doc = new JSONObject();
            doc.put("childpath",nametmp);
            doc.put("childmd5",md5);
            doc.put("childfilename",nametmp);
            jsonArray.add(doc);
        }

        object.put("children",jsonArray);

        Deep_file_infoCollection.insertOne(Document.parse(object.toJSONString()));
    }


    //追加抽词内容
    void updateSstvDataTemp(String md5, String content){
        Document doc = new Document();
        Bson addToSet = Updates.addToSet("content",content);
        try{
            sstv_data_collection.updateOne(eq("md5", md5), addToSet);
        }catch (Exception e){
            PrintException.Print(logger,e);
        }
    }

    /*
    void WriteTaskTemp(JSONObject data){
        Document doc = new Document("md5",data.getString("md5"));
        Document meta = new Document();

        meta.append("id",data.getString("id"));
        meta.append("dtr_id",data.getString("dtr_id"));
        meta.append("file_name",data.getString("file_name"));
        meta.append("priority",data.getString("priority"));
        meta.append("file_type",data.getString("file_type"));
        meta.append("version",data.getString("version"));
        meta.append("time",data.getString("time"));
        doc.append("meta", Arrays.asList(meta));
        try{
            sstv_data_collection.insertOne(doc);
        }catch (Exception e){
            PrintException.Print(logger,e);
        }
    }

    void UpdateTaskTemp(Document doc,JSONObject data) {
        Document meta = new Document();
        meta.append("id", data.getString("id"));
        meta.append("dtr_id", data.getString("dtr_id"));
        meta.append("file_name", data.getString("file_name"));
        meta.append("priority", data.getString("priority"));
        meta.append("file_type", data.getString("file_type"));
        meta.append("version", data.getString("version"));
        meta.append("time", data.getString("time"));
        doc.append("meta", Arrays.asList(meta));
        try {
            sstv_data_collection.updateOne(eq(doc.getObjectId("_id")), addToSet("meta", meta));
        }catch (Exception e){
            PrintException.Print(logger,e);
        }
    }
    */
}