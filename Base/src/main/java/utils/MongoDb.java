package utils;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.gridfs.model.GridFSFile;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.set;

public class MongoDb {
    private static Logger logger = LoggerFactory.getLogger(MongoDb.class);

    private MongoClient mongoClient;

    private MongoDatabase mongoDatabase;

    private GridFSBucket gridFSBucket;

    private MongoCollection<Document> mongoCollection;

    private final String databaseName = "Plantform_Chuzhi";

    private final String bucketName = "sensitive";

    private final String collectionName = "sensitive.files";

    public MongoDb(){
        mongoClient = Mongodbhandle.getInstance().getMongoClient();
        mongoDatabase = mongoClient.getDatabase(databaseName);
        gridFSBucket = GridFSBuckets.create(mongoDatabase,bucketName);
        mongoCollection = mongoDatabase.getCollection(collectionName);
    }

    public class FileResult{
        public FileResult(InputStream in_, GridFSFile gridFSFile_){
            in = in_;
            gridFSFile = gridFSFile_;
        }
        public InputStream in;
        public GridFSFile gridFSFile;
    }


    /**
     * @param md5
     * @return InputStream
     *
     */
    public FileResult GetFileStreamAndInfo(String md5){
        GridFSFindIterable it = gridFSBucket.find(
                eq("md5",md5));

        final ObjectId[] id = new ObjectId[1];
        final GridFSFile[] datas = new GridFSFile[1];

        //这里只可能有一个数据被检索到
        Block<GridFSFile> block = gridFSFile -> {
            id[0] = gridFSFile.getObjectId();
            datas[0] = gridFSFile;

            logger.debug("从mongodb取得数据girdfile对象: {}",datas[0].toString());
            //logger.debug("从mongodb取得数据: 元数据{}" ,datas[0].getMetadata().toJson());
        };

        for (GridFSFile fsFile : it) {
            block.apply(fsFile);
        }
        if (id[0] == null)
            return null;
        return new FileResult(gridFSBucket.openDownloadStream(id[0]),datas[0]);
    }

    public void FileUpload(InputStream in,String filename, String content_type){
        try {
            GridFSUploadOptions options = new GridFSUploadOptions()
                    .chunkSizeBytes(261120);

            gridFSBucket.uploadFromStream(filename, in, options);
        }catch (Exception e){
            PrintException.Print(logger,e);
        }
    }

    public void UpdateOneFileContentType(String md5,String type){
        mongoCollection.updateOne(eq("md5",md5),set("contentType",type));
    }

    public void GetFileJson(TaskInfo task){


        long sum = mongoCollection.count();

        int i = 0;
        while(sum-100 > 0){
            gridFSBucket.find().skip(i*100).limit(100).forEach(new Block<GridFSFile>() {
                @Override
                public void apply(GridFSFile gridFSFile) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("priority",1);
                    jsonObject.put("md5",gridFSFile.getMD5());
                    task.SetTask(jsonObject);
                }
            });
            i++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //测试代码 后面不写了
    }

    public void WriterTestData(JSONObject data){
        MongoCollection<Document> collection = mongoDatabase.getCollection("indexdata");

        collection.insertOne(Document.parse(data.toJSONString()));
    }




    public static void main(String[] args) throws IOException {

        MongoClient mongoClient = new MongoClient( "192.168.4.166" , 27018 );
        MongoDatabase myDatabase = mongoClient.getDatabase("Plantform_Chuzhi");

        myDatabase.listCollectionNames().forEach(
                (Block<String>) str -> System.out.println(str)
        );

        MongoCollection<Document> collection = myDatabase.getCollection("sensitive.files");
        collection.find(new Document().append("md5","e691df19277d08bc111f101074ee7a88")).forEach(
                (Block<Document>) doc ->{
                    System.out.println(doc.getDate("uploadDate"));
                }
        );
        collection.updateOne(eq("md5","e691df19277d08bc111f101074ee7a88"),set("contentType","aaaa"));

        //这里要注意bucketName和collectionName是两个概念，注意区分，写错了查询不到文件
        GridFSBucket gridFSFilesBucket = GridFSBuckets.create(myDatabase, "sensitive");

        GridFSFindIterable it = gridFSFilesBucket.find(eq("md5","fd313f599ffc7af4231628f4492f9dcc"));

        it.forEach((Block<GridFSFile>) gridFSFile -> {
            FileOutputStream streamToDownloadTo = null;
            try {
                streamToDownloadTo = new FileOutputStream(gridFSFile.getFilename());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            //放在数据最上层，可以使用该函数读取，但是该函数将不被支持，替换的方法是在上传文件的时候，
            gridFSFile.getExtraElements();
            gridFSFilesBucket.downloadToStream(gridFSFile.getObjectId(),streamToDownloadTo);
            try {
                streamToDownloadTo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

}
